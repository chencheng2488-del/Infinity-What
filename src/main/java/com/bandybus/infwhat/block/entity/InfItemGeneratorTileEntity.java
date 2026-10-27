package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch turns the stored item into an
 * unlimited source. Active output is configurable per side; the standard Forge item handler
 * capability stays available to pipes and hoppers on every side.
 *
 * <p>1.8.9 represents empty item stacks with {@code null}, so every read path is null-checked.
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
    private final IItemHandler capabilityHandler = new GeneratorCapabilityHandler();

    public InfItemGeneratorTileEntity() {
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.capabilityHandler;
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

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
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        return this.infiniteModeEnabled && stored != null && stored.stackSize > 0
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
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        if (effectivelyInfinite() && this.outputEnabled) {
            this.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (stored == null || stored.stackSize <= 0 || this.worldObj == null) {
            return;
        }
        int count = this.outputItemsPerTick;
        for (EnumFacing direction : EnumFacing.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            net.minecraft.util.BlockPos neighborPos = this.pos.offset(direction);
            if (!this.worldObj.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            IItemHandler target = neighbor.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
            if (target == null) {
                continue;
            }
            ItemStack offer = stored.copy();
            offer.stackSize = count;
            ItemHandlerHelper.insertItemStacked(target, offer, false);
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (stored != null && stored.stackSize > 0) {
            compound.setTag(KEY_ITEM, stored.writeToNBT(new NBTTagCompound()));
        }
        compound.setBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_OUTPUT_RATE, this.outputItemsPerTick);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.itemHandler.setStackInSlot(0, null);
        if (compound.hasKey(KEY_ITEM, 10)) {
            ItemStack loaded = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(KEY_ITEM));
            if (loaded != null && loaded.stackSize > 0) {
                loaded.stackSize = 1;
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
    public net.minecraft.network.Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(this.pos, 3, this.serializeNBT());
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.pos);
        }
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ResourceLocation id = (ResourceLocation) Item.itemRegistry.getNameForObject(stack.getItem());
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
                return null;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored == null || stored.stackSize <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored, Integer.MAX_VALUE);
            }
            return stored.copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack == null || stack.stackSize <= 0) {
                return stack;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (effectivelyInfinite() || (stored != null && stored.stackSize > 0)) {
                return stack;
            }
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return null;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored == null || stored.stackSize <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored,
                        Math.min(amount, stored.getMaxStackSize()));
            }
            return itemHandler.extractItem(0, amount, simulate);
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
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (effectivelyInfinite() || stack == null || stack.stackSize <= 0) {
                return stack;
            }
            ItemStack stored = this.getStackInSlot(0);
            if (stored != null && stored.stackSize > 0) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return null;
            }
            ItemStack existing = this.getStackInSlot(0);
            if (existing == null || existing.stackSize <= 0) {
                return null;
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

    /** Checks the configured item filter for a stack (used by the container slot). */
    public static boolean isAllowed(ItemStack stack) {
        return InfinityWhatConfig.isItemAllowed(itemId(stack));
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.worldObj != null && this.worldObj.getTileEntity(this.pos) == this;
    }
}
