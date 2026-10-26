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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
 * sides; the generic Fabric {@link ItemStorage} remains available to pipes/import buses on every
 * side.
 *
 * The internal slot always holds exactly one template item; while infinite the exposed storage
 * reports {@link Long#MAX_VALUE} so storage buses and pipes see an effectively unbounded amount,
 * and extraction never depletes. The GUI shows the item name/id instead of an infinity badge to
 * avoid clashing with the vanilla stack-count overlay.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos> {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";

    public static final int DEFAULT_OUTPUT_RATE = 8;

    private ItemStack stored = ItemStack.EMPTY;
    private boolean infiniteModeEnabled = false;
    private boolean outputEnabled = false;
    private int outputSideMask = (1 << Direction.values().length) - 1;
    private int outputItemsPerTick = DEFAULT_OUTPUT_RATE;

    private final GeneratorItemStorage itemHandler = new GeneratorItemStorage();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.stored.isEmpty() ? ItemStack.EMPTY : this.stored.copy();
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
        if (this.outputItemsPerTick == clamped) {
            return;
        }
        this.outputItemsPerTick = clamped;
        markUpdated();
    }

    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    public void applyOutputConfiguration(boolean enabled, int sideMask, int amountPerTick,
                                         boolean infiniteMode) {
        this.outputEnabled = enabled;
        this.outputSideMask = sideMask & ((1 << Direction.values().length) - 1);
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
                && !this.stored.isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    public Storage<ItemVariant> getItemHandler() {
        return this.itemHandler;
    }

    /**
     * Accepts one template item from the GUI slot. The slot is locked while a template is present
     * or while the infinite source is active.
     */
    public void setStoredItem(ItemStack stack) {
        if (stack.isEmpty()) {
            clearStoredItem();
            return;
        }
        if (effectivelyInfinite() || !this.stored.isEmpty()) {
            return;
        }
        if (!InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }
        this.stored = stack.copyWithCount(1);
        markUpdated();
    }

    /**
     * Removes the template item (or hands out a copy while infinite, without depleting the source).
     * The internal slot never holds more than one unit.
     */
    public ItemStack extractStored(int amount) {
        if (amount <= 0 || this.stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (effectivelyInfinite()) {
            return this.stored.copyWithCount(Math.min(amount, this.stored.getMaxStackSize()));
        }
        ItemStack result = this.stored.copyWithCount(1);
        this.stored = ItemStack.EMPTY;
        markUpdated();
        return result;
    }

    public void clearStoredItem() {
        if (this.stored.isEmpty()) {
            return;
        }
        this.stored = ItemStack.EMPTY;
        markUpdated();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfItemGeneratorMenu(containerId, inventory, this.worldPosition);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfItemGeneratorBlockEntity blockEntity) {
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
     * Tries to insert the generated item into selected adjacent blocks that expose an item storage
     * (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     */
    private void pushToNeighbors() {
        if (this.stored.isEmpty() || this.level == null) {
            return;
        }
        ItemVariant variant = ItemVariant.of(this.stored);
        long count = this.outputItemsPerTick;
        try (Transaction transaction = Transaction.openOuter()) {
            boolean moved = false;
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
                if (target.insert(variant, count, transaction) > 0L) {
                    moved = true;
                }
            }
            if (moved) {
                transaction.commit();
            } else {
                transaction.abort();
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.stored.isEmpty()) {
            tag.put(KEY_ITEM, this.stored.save(registries));
        }
        tag.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stored = ItemStack.EMPTY;
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.stored = loaded.copyWithCount(1);
            }
        }
        this.infiniteModeEnabled = tag.getBoolean(KEY_INFINITE_MODE);
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                ? tag.getInt(KEY_OUTPUT_SIDES)
                : (1 << Direction.values().length) - 1;
        this.outputItemsPerTick = tag.contains(KEY_OUTPUT_RATE, Tag.TAG_INT)
                ? Math.max(1, tag.getInt(KEY_OUTPUT_RATE)) : DEFAULT_OUTPUT_RATE;
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------
    // Item storage (huge amount while infinite)
    // ------------------------------------------------------------------

    private final class GeneratorItemStorage extends SnapshotParticipant<ItemStack>
            implements SingleSlotStorage<ItemVariant> {

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L || effectivelyInfinite()) {
                return 0L;
            }
            // Only one template item is ever accepted.
            if (!stored.isEmpty()) {
                return 0L;
            }
            if (!InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(resource.getItem()))) {
                return 0L;
            }
            updateSnapshots(transaction);
            stored = resource.toStack(1);
            markUpdated();
            return 1L;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (stored.isEmpty() || resource.isBlank() || maxAmount <= 0L
                    || !resource.matches(stored)) {
                return 0L;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never deplete, hand out up to one normal stack per call.
                return Math.min(maxAmount, stored.getMaxStackSize());
            }
            updateSnapshots(transaction);
            stored = ItemStack.EMPTY;
            markUpdated();
            return 1L;
        }

        @Override
        public boolean isResourceBlank() {
            return stored.isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return stored.isEmpty() ? ItemVariant.blank() : ItemVariant.of(stored);
        }

        @Override
        public long getAmount() {
            if (effectivelyInfinite()) {
                return Long.MAX_VALUE;
            }
            return stored.isEmpty() ? 0L : 1L;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : 1L;
        }

        @Override
        protected ItemStack createSnapshot() {
            return stored.copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            stored = snapshot.copy();
            markUpdated();
        }
    }
}
