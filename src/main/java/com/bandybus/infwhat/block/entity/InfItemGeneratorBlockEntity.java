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
 * sides; the Fabric {@link ItemStorage} lookup remains available to pipes/import buses on every side.
 *
 * <p>The internal slot always holds exactly one template item. While infinite, the exposed
 * storage reports {@link Long#MAX_VALUE} (the largest amount the transfer API can express), so
 * AE2 storage buses and pipes see an effectively unbounded amount, and extraction never depletes.
 * The GUI shows the item name/id instead of an infinity badge to avoid clashing with the vanilla
 * stack-count overlay.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";

    public static final int DEFAULT_OUTPUT_RATE = 8;

    private boolean infiniteModeEnabled = false;
    private boolean outputEnabled = false;
    private int outputSideMask = (1 << Direction.values().length) - 1;
    private int outputItemsPerTick = DEFAULT_OUTPUT_RATE;

    /** The stored item, always a single unit internally. */
    private ItemStack template = ItemStack.EMPTY;

    /**
     * Capability view of the single slot. It reports the huge infinite count and lets pipes/AE2
     * pull from the block, while the GUI slot keeps facing a plain one-item view.
     */
    private final GeneratorStorage itemStorage = new GeneratorStorage();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    public Storage<ItemVariant> getItemStorage() {
        return this.itemStorage;
    }

    /** The stored item as a variant, used by the GUI for infinite extraction. */
    public ItemVariant getStoredVariant() {
        return this.template.isEmpty() ? ItemVariant.blank() : ItemVariant.of(this.template);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.template;
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
                && !this.template.isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    /** Replaces the template item with a single unit of {@code stack}. */
    public void setStoredItem(ItemStack stack) {
        ItemStack clamped = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(this.template, clamped)) {
            return;
        }
        this.template = clamped;
        markUpdated();
    }

    /** Removes the template item, reporting at most one unit back to the caller. */
    public ItemStack removeStoredItem(int amount) {
        if (this.template.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.template.copyWithCount(1);
        this.template = ItemStack.EMPTY;
        markUpdated();
        return removed;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfItemGeneratorMenu(containerId, inventory, this.worldPosition);
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
     * Tries to insert the generated item into selected adjacent blocks that expose an
     * {@link ItemStorage} (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     */
    private void pushToNeighbors() {
        if (this.template.isEmpty() || this.level == null) {
            return;
        }
        long count = this.outputItemsPerTick;
        ItemVariant generated = ItemVariant.of(this.template);
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
                target.insert(generated, count, transaction);
                transaction.commit();
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.template.isEmpty()) {
            tag.put(KEY_ITEM, this.template.save(registries));
        }
        tag.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.template = ItemStack.EMPTY;
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.template = loaded.copyWithCount(1);
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

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------
    // Item storage (huge amount while infinite)
    // ------------------------------------------------------------------

    /**
     * The storage given to pipes and AE2 storage buses. While the generator is infinite it reports
     * {@link Long#MAX_VALUE} for the single slot and never depletes on extraction, so the network
     * sees an effectively unbounded amount instead of one item. Insertion is always limited to one
     * template item, and never while the infinite source is active.
     */
    private final class GeneratorStorage extends SnapshotParticipant<ItemStack>
            implements SingleSlotStorage<ItemVariant> {

        @Override
        protected ItemStack createSnapshot() {
            return InfItemGeneratorBlockEntity.this.template.copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            InfItemGeneratorBlockEntity.this.template = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            markUpdated();
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L) {
                return 0L;
            }
            if (effectivelyInfinite() || !InfItemGeneratorBlockEntity.this.template.isEmpty()) {
                return 0L;
            }
            if (!InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(resource.getItem()))) {
                return 0L;
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.template = resource.toStack(1);
            return 1L;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L) {
                return 0L;
            }
            ItemStack stored = InfItemGeneratorBlockEntity.this.template;
            if (stored.isEmpty() || !resource.matches(stored)) {
                return 0L;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: hand out up to one normal stack per call, never deplete.
                return Math.min(maxAmount, stored.getMaxStackSize());
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.template = ItemStack.EMPTY;
            return 1L;
        }

        @Override
        public boolean isResourceBlank() {
            return InfItemGeneratorBlockEntity.this.template.isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return getStoredVariant();
        }

        @Override
        public long getAmount() {
            if (effectivelyInfinite()) {
                return Long.MAX_VALUE;
            }
            return InfItemGeneratorBlockEntity.this.template.isEmpty() ? 0L : 1L;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : 1L;
        }
    }
}
