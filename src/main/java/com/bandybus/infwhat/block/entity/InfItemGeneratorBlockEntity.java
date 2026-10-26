package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch makes the stored item an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the Fabric item storage view remains available to pipes/import buses on every side.
 *
 * ENGINE LIMIT: a vanilla {@link ItemStack} stores its count as an {@code int}, so the internal
 * slot always holds exactly one template item while the exposed storage reports
 * {@link Long#MAX_VALUE} during infinite mode, so AE2 storage buses and pipes see an effectively
 * unbounded amount, and extraction never depletes.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";

    public static final int DEFAULT_OUTPUT_RATE = 8;

    private static final int ALL_SIDES = (1 << Direction.values().length) - 1;

    private boolean infiniteModeEnabled = false;
    private boolean outputEnabled = false;
    private int outputSideMask = ALL_SIDES;
    private int outputItemsPerTick = DEFAULT_OUTPUT_RATE;

    /** The stored item, always a single unit internally. */
    private ItemStack storedItem = ItemStack.EMPTY;

    private final GeneratorItemStorage itemStorage = new GeneratorItemStorage();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Item storage
    // ------------------------------------------------------------------

    public Storage<ItemVariant> getItemStorage() {
        return this.itemStorage;
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.storedItem;
    }

    /** Replaces the template item; the slot always keeps exactly one unit. */
    public void setStoredItem(ItemStack stack) {
        ItemStack next = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(this.storedItem, next)) {
            return;
        }
        this.storedItem = next;
        markUpdated();
    }

    public void removeStored(int amount) {
        if (this.storedItem.isEmpty() || amount <= 0) {
            return;
        }
        if (amount >= this.storedItem.getCount()) {
            this.storedItem = ItemStack.EMPTY;
        } else {
            this.storedItem.shrink(amount);
        }
        markUpdated();
    }

    /**
     * Manual retrieval used by the GUI. While infinite the source is never depleted; otherwise
     * the stored template item is consumed.
     */
    public ItemStack extractStored(int amount) {
        if (this.storedItem.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (effectivelyInfinite()) {
            return this.storedItem.copyWithCount(
                    Math.min(amount, this.storedItem.getMaxStackSize()));
        }
        ItemStack result = this.storedItem.copyWithCount(Math.min(amount, this.storedItem.getCount()));
        removeStored(result.getCount());
        return result;
    }

    /** Whether the GUI slot may accept the given stack. */
    public boolean canInsertItem(ItemStack stack) {
        return !stack.isEmpty()
                && !effectivelyInfinite()
                && this.storedItem.isEmpty()
                && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    public boolean isInfiniteModeEnabled() {
        return this.infiniteModeEnabled;
    }

    public void setInfiniteModeEnabled(boolean enabled) {
        if (this.infiniteModeEnabled == enabled) {
            return;
        }
        this.infiniteModeEnabled = enabled;
        markUpdated();
    }

    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    public void setOutputEnabled(boolean enabled) {
        if (this.outputEnabled == enabled) {
            return;
        }
        this.outputEnabled = enabled;
        markUpdated();
    }

    public boolean isOutputSideEnabled(Direction direction) {
        return (this.outputSideMask & (1 << direction.get3DDataValue())) != 0;
    }

    public void toggleOutputSide(Direction direction) {
        this.outputSideMask ^= 1 << direction.get3DDataValue();
        markUpdated();
    }

    public int getOutputItemsPerTick() {
        return this.outputItemsPerTick;
    }

    public void setOutputItemsPerTick(int amount) {
        int clamped = Math.max(1, amount);
        if (this.outputItemsPerTick == clamped) return;
        this.outputItemsPerTick = clamped;
        markUpdated();
    }

    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    public void applyOutputConfiguration(boolean enabled, int sideMask, int amountPerTick,
                                         boolean infiniteMode) {
        this.outputEnabled = enabled;
        this.outputSideMask = sideMask & ALL_SIDES;
        this.outputItemsPerTick = Math.max(1, amountPerTick);
        this.infiniteModeEnabled = infiniteMode;
        markUpdated();
    }

    /**
     * The generator is only infinite while its GUI switch is enabled, it holds an item, and the
     * shared infinite-source balance switch is enabled.
     */
    private boolean effectivelyInfinite() {
        return this.infiniteModeEnabled
                && !this.storedItem.isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfItemGeneratorMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfItemGeneratorBlockEntity blockEntity) {
        blockEntity.tick();
    }

    private void tick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        if (effectivelyInfinite() && this.outputEnabled) {
            pushToNeighbors();
        }
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose an item
     * storage (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     */
    private void pushToNeighbors() {
        ItemStack stored = this.storedItem;
        if (stored.isEmpty() || this.level == null) {
            return;
        }
        ItemVariant resource = ItemVariant.of(stored);
        long count = this.outputItemsPerTick;
        for (Direction direction : Direction.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            BlockPos neighborPos = this.worldPosition.relative(direction);
            if (!this.level.isLoaded(neighborPos)) {
                continue;
            }
            BlockEntity neighbor = this.level.getBlockEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            Storage<ItemVariant> target = ItemStorage.SIDED.find(this.level, neighborPos,
                    direction.getOpposite());
            if (target == null) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                target.insert(resource, count, transaction);
                transaction.commit();
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.storedItem.isEmpty()) {
            tag.put(KEY_ITEM, this.storedItem.save(new CompoundTag()));
        }
        tag.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.storedItem = ItemStack.EMPTY;
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.of(tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.storedItem = loaded.copyWithCount(1);
            }
        }
        this.infiniteModeEnabled = tag.getBoolean(KEY_INFINITE_MODE);
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                ? tag.getInt(KEY_OUTPUT_SIDES)
                : ALL_SIDES;
        this.outputItemsPerTick = tag.contains(KEY_OUTPUT_RATE, Tag.TAG_INT)
                ? Math.max(1, tag.getInt(KEY_OUTPUT_RATE)) : DEFAULT_OUTPUT_RATE;
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------
    // Item storage implementation (single slot, transactional)
    // ------------------------------------------------------------------

    private final class GeneratorItemStorage extends SnapshotParticipant<ItemStack>
            implements SingleSlotStorage<ItemVariant> {

        @Override
        protected ItemStack createSnapshot() {
            return InfItemGeneratorBlockEntity.this.storedItem.copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            InfItemGeneratorBlockEntity.this.storedItem = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            markUpdated();
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount < 1) {
                return 0;
            }
            // Only one item is ever accepted, and never while the infinite source is active.
            if (effectivelyInfinite() || !InfItemGeneratorBlockEntity.this.storedItem.isEmpty()) {
                return 0;
            }
            if (!InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(resource.getItem()))) {
                return 0;
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.storedItem = resource.toStack(1);
            return 1;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0
                    || InfItemGeneratorBlockEntity.this.storedItem.isEmpty()
                    || !resource.equals(ItemVariant.of(InfItemGeneratorBlockEntity.this.storedItem))) {
                return 0;
            }
            ItemStack stored = InfItemGeneratorBlockEntity.this.storedItem;
            if (effectivelyInfinite()) {
                // Unlimited source: never deplete, hand out up to one normal stack per call.
                return Math.min(maxAmount, stored.getMaxStackSize());
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.storedItem = ItemStack.EMPTY;
            return 1;
        }

        @Override
        public boolean isResourceBlank() {
            return InfItemGeneratorBlockEntity.this.storedItem.isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            ItemStack stored = InfItemGeneratorBlockEntity.this.storedItem;
            return stored.isEmpty() ? ItemVariant.blank() : ItemVariant.of(stored);
        }

        @Override
        public long getAmount() {
            if (effectivelyInfinite()) {
                return Long.MAX_VALUE;
            }
            return InfItemGeneratorBlockEntity.this.storedItem.isEmpty() ? 0 : 1;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : 1;
        }
    }
}
