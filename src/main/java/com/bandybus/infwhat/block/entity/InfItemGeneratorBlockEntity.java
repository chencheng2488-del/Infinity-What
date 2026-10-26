package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import javax.annotation.Nullable;


/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch makes the stored item an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the standard Forge {@link IItemHandler} capability remains available to pipes/import
 * buses on every side.
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

    private final GeneratorItemHandler itemHandler = new GeneratorItemHandler();
    /**
     * Capability view of the single slot. It reports the huge infinite count and lets pipes/AE2
     * pull from the block, while {@link GeneratorItemHandler} keeps the GUI facing a plain
     * one-item slot.
     */
    private final IItemHandler capabilityHandler = new CapabilityItemHandler();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    public IItemHandler getCapabilityHandler() {
        return this.capabilityHandler;
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.itemHandler.getInternalStack();
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
                && !this.itemHandler.getInternalStack().isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
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
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        if (effectivelyInfinite() && this.outputEnabled) {
            pushToNeighbors();
        }
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose a Forge
     * {@link IItemHandler} (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     */
    private void pushToNeighbors() {
        ItemStack stored = this.itemHandler.getInternalStack();
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
            IItemHandler target = this.level.getCapability(Capabilities.ItemHandler.BLOCK,
                    neighborPos, direction.getOpposite());
            if (target == null) {
                continue;
            }
            // Copy (so NBT/components survive); the helper splits large counts across target slots.
            ItemStack offer = stored.copy();
            offer.setCount(count);
            ItemHandlerHelper.insertItemStacked(target, offer, false);
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ItemStack stored = this.itemHandler.getInternalStack();
        if (!stored.isEmpty()) {
            tag.put(KEY_ITEM, stored.save(registries));
        }
        tag.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.setInternalEmpty();
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.itemHandler.setInternalStack(loaded.copyWithCount(1));
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

    @Nullable
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
    // Capability item handler (huge count while infinite)
    // ------------------------------------------------------------------

    /**
     * The {@link IItemHandler} given to pipes and AE2 storage buses. While the generator is
     * infinite it reports {@link Integer#MAX_VALUE} for the single slot and never depletes on
     * extraction, so the network sees an effectively unbounded amount instead of one item.
     * Insertion is always limited to one template item.
     */
    private final class CapabilityItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getInternalStack();
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // ItemStack#count is an int, so Integer.MAX_VALUE is the largest amount the
                // engine can express; AE2 reads this count and shows the slot as unbounded.
                return stored.copyWithCount(Integer.MAX_VALUE);
            }
            return stored.copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            // Only one item is ever accepted, and never while the infinite source is active.
            if (effectivelyInfinite() || !itemHandler.getInternalStack().isEmpty()) {
                return stack;
            }
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getInternalStack();
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never deplete, hand out up to one normal stack per call.
                return stored.copyWithCount(Math.min(amount, stored.getMaxStackSize()));
            }
            return itemHandler.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return effectivelyInfinite() ? Integer.MAX_VALUE : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !effectivelyInfinite() && itemHandler.getInternalStack().isEmpty()
                    && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }

    // ------------------------------------------------------------------
    // Item handler
    // ------------------------------------------------------------------

    private final class GeneratorItemHandler extends ItemStackHandler {

        GeneratorItemHandler() {
            super(1);
        }

        ItemStack getInternalStack() {
            return this.stacks.get(0);
        }

        void setInternalStack(ItemStack stack) {
            this.stacks.set(0, stack);
        }

        void setInternalEmpty() {
            this.stacks.set(0, ItemStack.EMPTY);
        }

        @Override
        public int getSlotLimit(int slot) {
            // The generator always holds exactly one template item.
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // A single item may be inserted while the slot is empty; the slot is locked otherwise.
            return slot == 0 && this.stacks.get(0).isEmpty()
                    && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (effectivelyInfinite() || stack.isEmpty() || !this.stacks.get(0).isEmpty()) {
                return stack;
            }
            // super honours getSlotLimit (1) and isItemValid, so at most one unit is stored,
            // and it fires onContentsChanged (which marks the block entity dirty) itself.
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack existing = this.stacks.get(0);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: hand out up to one normal stack per call, never deplete.
                return existing.copyWithCount(Math.min(amount, existing.getMaxStackSize()));
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    }
}
