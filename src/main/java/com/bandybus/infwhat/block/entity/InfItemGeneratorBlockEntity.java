package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import javax.annotation.Nullable;


/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch makes the stored item an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the standard NeoForge {@code ResourceHandler<ItemResource>} capability remains
 * available to pipes/import buses on every side.
 *
 * ENGINE LIMIT: {@link ItemStack} stores its count as an {@code int}, so a true
 * {@code Long.MAX_VALUE} count cannot be stored. The internal slot always holds exactly one
 * template item; while infinite the exposed capability reports {@link Integer#MAX_VALUE} (the
 * largest amount the engine can express) so AE2 storage buses and pipes see an effectively
 * unbounded amount, and extraction never depletes. The GUI shows the item name/id instead of an
 * infinity badge to avoid clashing with the vanilla stack-count overlay.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity implements MenuProvider {

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

    /** The stored template item. Always held with a count of exactly one. */
    private ItemStack storedItem = ItemStack.EMPTY;
    /**
     * Capability view of the single slot. It reports the huge infinite count and lets pipes/AE2
     * pull from the block, while {@link MenuItemHandler} keeps the GUI facing a plain
     * one-item slot.
     */
    private final ResourceHandler<ItemResource> resourceHandler = new GeneratorResourceHandler();
    private final MenuItemHandler menuHandler = new MenuItemHandler();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    public ResourceHandler<ItemResource> getResourceHandler() {
        return this.resourceHandler;
    }

    /**
     * Menu-facing view of the slot: always reports a count of one so the GUI slot shows the
     * plain template item instead of the huge capability count.
     */
    public ResourceHandler<ItemResource> getMenuHandler() {
        return this.menuHandler;
    }

    /** Direct mutation function backing the menu slot (place / pick up the template item). */
    public IndexModifier<ItemResource> getMenuModifier() {
        return this.menuHandler;
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.storedItem;
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
                && !this.storedItem.isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Nullable
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
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        if (effectivelyInfinite() && this.outputEnabled) {
            pushToNeighbors();
        }
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose a NeoForge
     * {@code ResourceHandler<ItemResource>} (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     */
    private void pushToNeighbors() {
        ItemStack stored = this.storedItem;
        if (stored.isEmpty() || this.level == null) {
            return;
        }
        int count = this.outputItemsPerTick;
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
            ResourceHandler<ItemResource> target = this.level.getCapability(Capabilities.Item.BLOCK,
                    neighborPos, direction.getOpposite());
            if (target == null) {
                continue;
            }
            ItemResource resource = ItemResource.of(stored);
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                int accepted = target.insert(resource, count, transaction);
                if (accepted > 0) {
                    transaction.commit();
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack stored = this.storedItem;
        if (!stored.isEmpty()) {
            output.store(KEY_ITEM, ItemStack.CODEC, stored);
        }
        output.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        output.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        output.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        output.putInt(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedItem = ItemStack.EMPTY;
        ItemStack loaded = input.read(KEY_ITEM, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (!loaded.isEmpty()) {
            // The slot always holds exactly one template item.
            this.storedItem = loaded.copyWithCount(1);
        }
        this.infiniteModeEnabled = input.getBooleanOr(KEY_INFINITE_MODE, false);
        this.outputEnabled = input.getBooleanOr(KEY_OUTPUT_ENABLED, false);
        this.outputSideMask = input.getIntOr(KEY_OUTPUT_SIDES, (1 << Direction.values().length) - 1);
        this.outputItemsPerTick = Math.max(1, input.getIntOr(KEY_OUTPUT_RATE, DEFAULT_OUTPUT_RATE));
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------
    // Capability item handler (huge count while infinite)
    // ------------------------------------------------------------------

    /**
     * The {@code ResourceHandler<ItemResource>} given to pipes and AE2 storage buses. While the
     * generator is infinite it reports {@link Long#MAX_VALUE} for the single slot and never
     * depletes on extraction, so the network sees an effectively unbounded amount instead of
     * one item. Insertion is always limited to one template item.
     */
    private final class GeneratorResourceHandler extends SnapshotJournal<ItemStack>
            implements ResourceHandler<ItemResource> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            checkIndex(index);
            return storedItem.isEmpty() ? ItemResource.EMPTY : ItemResource.of(storedItem);
        }

        @Override
        public long getAmountAsLong(int index) {
            checkIndex(index);
            if (storedItem.isEmpty()) return 0;
            return effectivelyInfinite() ? Long.MAX_VALUE : 1L;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            checkIndex(index);
            return effectivelyInfinite() ? Long.MAX_VALUE : 1L;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            checkIndex(index);
            return !resource.isEmpty() && !effectivelyInfinite() && storedItem.isEmpty()
                    && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(resource.getItem()));
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0 || !isValid(index, resource)) return 0;
            updateSnapshots(transaction);
            storedItem = resource.toStack(1);
            return 1;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0 || storedItem.isEmpty()
                    || !ItemResource.of(storedItem).equals(resource)) return 0;
            if (effectivelyInfinite()) return amount;
            updateSnapshots(transaction);
            storedItem = ItemStack.EMPTY;
            return 1;
        }

        @Override
        protected ItemStack createSnapshot() {
            return storedItem.copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            storedItem = snapshot.copy();
        }

        @Override
        protected void onRootCommit(ItemStack snapshot) {
            markUpdated();
        }

        private void checkIndex(int index) {
            if (index != 0) throw new IndexOutOfBoundsException(index);
        }
    }

    // ------------------------------------------------------------------
    // Menu item handler (always a plain one-item view for the GUI slot)
    // ------------------------------------------------------------------

    /**
     * Menu-facing view of the single slot. It mirrors {@link GeneratorResourceHandler} but always
     * reports a count of one, so the GUI slot renders the plain template item instead of the huge
     * capability count. Implements {@link IndexModifier} so the menu slot can place, swap and
     * pick up the template item directly.
     */
    private final class MenuItemHandler extends SnapshotJournal<ItemStack>
            implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            checkIndex(index);
            return storedItem.isEmpty() ? ItemResource.EMPTY : ItemResource.of(storedItem);
        }

        @Override
        public long getAmountAsLong(int index) {
            checkIndex(index);
            return storedItem.isEmpty() ? 0 : 1;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            checkIndex(index);
            // The generator always holds exactly one template item.
            return 1;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            checkIndex(index);
            // A single item may be inserted while the slot is empty; the slot is locked otherwise.
            return !resource.isEmpty() && !effectivelyInfinite() && storedItem.isEmpty()
                    && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(resource.getItem()));
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0 || !isValid(index, resource)) return 0;
            updateSnapshots(transaction);
            storedItem = resource.toStack(1);
            return 1;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0 || storedItem.isEmpty()
                    || !ItemResource.of(storedItem).equals(resource)) return 0;
            if (effectivelyInfinite()) {
                // Unlimited source: hand out up to one normal stack per call, never deplete.
                return Math.min(amount, resource.getMaxStackSize());
            }
            updateSnapshots(transaction);
            storedItem = ItemStack.EMPTY;
            return 1;
        }

        @Override
        public void set(int index, ItemResource resource, int amount) {
            checkIndex(index);
            storedItem = resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(1);
            markUpdated();
        }

        @Override
        protected ItemStack createSnapshot() {
            return storedItem.copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            storedItem = snapshot.copy();
        }

        @Override
        protected void onRootCommit(ItemStack snapshot) {
            markUpdated();
        }

        private void checkIndex(int index) {
            if (index != 0) throw new IndexOutOfBoundsException(index);
        }
    }
}
