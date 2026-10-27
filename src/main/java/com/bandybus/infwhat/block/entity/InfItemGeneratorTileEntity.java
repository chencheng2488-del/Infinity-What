package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch turns the stored item into an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the standard Forge {@link IItemHandler} capability remains available to pipes and
 * import buses on every side.
 *
 * <p>ENGINE LIMIT: {@link ItemStack} stores its count as an {@code int}, so a true
 * {@code Long.MAX_VALUE} count cannot be stored. The internal slot always holds exactly one
 * template item; while infinite the exposed capability reports {@link Integer#MAX_VALUE} (the
 * largest amount the engine can express) so pipes see an effectively unbounded amount, and
 * extraction never depletes. The GUI shows the item name/id instead of an infinity badge.
 */
public class InfItemGeneratorTileEntity extends TileEntity implements ITickable {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";

    public static final int DEFAULT_OUTPUT_RATE = 8;

    private boolean infiniteModeEnabled;
    private boolean outputEnabled;
    private int outputSideMask = (1 << EnumFacing.values().length) - 1;
    private int outputItemsPerTick = DEFAULT_OUTPUT_RATE;

    private final GeneratorItemHandler itemHandler = new GeneratorItemHandler();
    /** Capability view: reports the huge infinite count instead of the single template item. */
    private final IItemHandler capabilityHandler = new GeneratorCapabilityHandler();

    public InfItemGeneratorTileEntity() {
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.capabilityHandler);
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.itemHandler.getStackInSlot(0);
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

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleOutputSide(EnumFacing direction) {
        this.outputSideMask ^= 1 << direction.ordinal();
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
        this.outputSideMask = sideMask & ((1 << EnumFacing.values().length) - 1);
        this.outputItemsPerTick = Math.max(1, amountPerTick);
        this.infiniteModeEnabled = infiniteMode;
        markUpdated();
    }

    private boolean effectivelyInfinite() {
        return this.infiniteModeEnabled
                && !this.itemHandler.getStackInSlot(0).isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (effectivelyInfinite() && this.outputEnabled) {
            this.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (stored.isEmpty() || this.world == null) {
            return;
        }
        int count = this.outputItemsPerTick;
        for (EnumFacing direction : EnumFacing.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            BlockPos neighborPos = this.pos.offset(direction);
            if (!this.world.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity neighbor = this.world.getTileEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            IItemHandler target = neighbor.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
            if (target == null) {
                continue;
            }
            // Copy (so NBT survives); the helper splits the count across the target's slots.
            ItemStack offer = stored.copy();
            offer.setCount(count);
            ItemHandlerHelper.insertItemStacked(target, offer, false);
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (!stored.isEmpty()) {
            compound.setTag(KEY_ITEM, stored.writeToNBT(new NBTTagCompound()));
        }
        compound.setBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_OUTPUT_RATE, this.outputItemsPerTick);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        if (compound.hasKey(KEY_ITEM, 10)) {
            ItemStack loaded = new ItemStack(compound.getCompoundTag(KEY_ITEM));
            if (!loaded.isEmpty()) {
                loaded.setCount(1);
                this.itemHandler.setStackInSlot(0, loaded);
            }
        }
        this.infiniteModeEnabled = compound.getBoolean(KEY_INFINITE_MODE);
        this.outputEnabled = compound.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES)
                : (1 << EnumFacing.values().length) - 1;
        this.outputItemsPerTick = compound.hasKey(KEY_OUTPUT_RATE, 3)
                ? Math.max(1, compound.getInteger(KEY_OUTPUT_RATE))
                : DEFAULT_OUTPUT_RATE;
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 3, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos),
                    this.world.getBlockState(this.pos), 3);
        }
    }

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() == null) {
            return null;
        }
        net.minecraft.util.ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : id.toString();
    }

    // ------------------------------------------------------------------
    // Capability item handler (huge count while infinite)
    // ------------------------------------------------------------------

    private final class GeneratorCapabilityHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored, Integer.MAX_VALUE);
            }
            return stored.copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            if (effectivelyInfinite() || !itemHandler.getStackInSlot(0).isEmpty()) {
                return stack;
            }
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored,
                        Math.min(amount, stored.getMaxStackSize()));
            }
            return itemHandler.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return effectivelyInfinite() ? Integer.MAX_VALUE : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !effectivelyInfinite()
                    && itemHandler.getStackInSlot(0).isEmpty()
                    && InfinityWhatConfig.isItemAllowed(itemId(stack));
        }
    }

    // ------------------------------------------------------------------
    // Internal item handler used by the GUI slot
    // ------------------------------------------------------------------

    private final class GeneratorItemHandler extends ItemStackHandler {

        GeneratorItemHandler() {
            super(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            // The generator always holds exactly one template item.
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && this.getStackInSlot(0).isEmpty()
                    && InfinityWhatConfig.isItemAllowed(itemId(stack));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (effectivelyInfinite() || stack.isEmpty() || !this.getStackInSlot(0).isEmpty()) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack existing = this.getStackInSlot(0);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(existing,
                        Math.min(amount, existing.getMaxStackSize()));
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.world != null && this.world.getTileEntity(this.pos) == this;
    }
}
