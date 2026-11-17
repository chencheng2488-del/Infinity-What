package com.bandybus.infwhat.integration.thaumcraft6;

import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.core.SourceOutputSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.api.aspects.Aspect;

/**
 * Shared inventory, lock and active-output logic for the two Thaumcraft 6 arcane machines.
 *
 * <p>Subclasses only provide the sample predicate, the aspect reader, the target predicate and the
 * per-tick action that distinguishes an aspect replicator from an essentia source.
 */
public abstract class AbstractArcaneMachineTileEntity extends TileEntity
        implements IInventory, ITickable, IArcaneConfigurable {

    protected static final String KEY_SAMPLE = "Sample";
    protected static final String KEY_TARGET = "Target";
    protected static final String KEY_ASPECT = "LockedAspect";
    protected static final String KEY_INFINITE = "Infinite";
    protected static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    protected static final String KEY_OUTPUT_SIDES = "OutputSides";
    protected static final String KEY_OUTPUT_RATE = "OutputRate";

    /** Upper bound for the configurable output rate. */
    public static final int MAX_OUTPUT_RATE = 1_000_000;

    protected ItemStack sampleStack = ItemStack.EMPTY;
    protected ItemStack targetStack = ItemStack.EMPTY;
    protected Aspect lockedAspect;
    protected boolean infinite;
    /** Shared output switch, face mask and per-face rate. */
    protected final SourceOutputSettings output;
    /**
     * Neighbours must re-read this machine once its saved state is known. AE grids are formed a
     * moment after the chunk loads, so the announcement is repeated over a short settle window
     * instead of being sent once, where it could arrive before the grid exists.
     */
    private static final int LOAD_ANNOUNCE_REPEATS = 3;
    private static final int LOAD_ANNOUNCE_INTERVAL = 20;
    private int loadAnnounceRepeats = LOAD_ANNOUNCE_REPEATS;
    private int loadAnnounceCountdown = 1;

    protected AbstractArcaneMachineTileEntity() {
        this.output = new SourceOutputSettings(defaultOutputRate(), 1L, MAX_OUTPUT_RATE);
        this.output.setEnabled(defaultOutputEnabled());
    }

    /** Output rate used when the machine is first placed. */
    protected abstract int defaultOutputRate();

    /** Whether active output starts enabled. */
    protected abstract boolean defaultOutputEnabled();

    /** Kind of arcane machine, used to match configuration shard profiles. */
    @Override
    public abstract String getConfigurationType();

    /** Inventory name translation key. */
    protected abstract String getInventoryTranslationKey();

    /** Whether the stack can be used as the locked sample. */
    protected abstract boolean isValidSample(ItemStack stack);

    /** Aspect encoded by the sample stack. */
    protected abstract Aspect aspectOfSample(ItemStack stack);

    /** Whether the stack can receive the locked aspect. */
    protected abstract boolean isValidTarget(ItemStack stack);

    /** Fills the whole target stack in place; returns true when it changed. */
    protected abstract boolean fillTargetInPlace();

    /** Called every tick while the machine is providing, after the target stack was filled. */
    protected abstract void onProvidingTick();

    /** Extra NBT written by subclasses. */
    protected void writeExtraNBT(NBTTagCompound compound) {
    }

    /** Extra NBT read by subclasses. */
    protected void readExtraNBT(NBTTagCompound compound) {
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    public ItemStack getSampleStack() {
        return this.sampleStack.isEmpty() ? ItemStack.EMPTY : this.sampleStack.copy();
    }

    public ItemStack getTargetStack() {
        return this.targetStack.isEmpty() ? ItemStack.EMPTY : this.targetStack.copy();
    }

    public Aspect getLockedAspect() {
        return this.lockedAspect;
    }

    public boolean isInfinite() {
        return this.infinite;
    }

    /**
     * Enables or disables infinite production. The switch may be turned on before a sample is
     * inserted; the aspect locks automatically once one is placed.
     */
    public boolean setInfiniteEnabled(boolean enabled) {
        if (this.infinite == enabled) {
            return true;
        }
        boolean wasProviding = isProviding();
        this.infinite = enabled;
        markUpdated();
        notifyNeighborsIfProvidingChanged(wasProviding);
        return true;
    }

    /** Whether the machine is actually supplying (switch on and an aspect locked). */
    public boolean isProviding() {
        return this.infinite && this.lockedAspect != null;
    }

    public boolean isOutputEnabled() {
        return this.output.isEnabled();
    }

    public void setOutputEnabled(boolean enabled) {
        if (this.output.isEnabled() == enabled) {
            return;
        }
        this.output.setEnabled(enabled);
        markUpdated();
    }

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return this.output.isSideEnabled(direction);
    }

    public void toggleOutputSide(EnumFacing direction) {
        this.output.toggleSide(direction);
        markUpdated();
    }

    public int getOutputItemsPerTick() {
        return (int) this.output.getRate();
    }

    public void setOutputItemsPerTick(int amount) {
        if (this.output.getRate() == amount) {
            return;
        }
        this.output.setRate(amount);
        markUpdated();
    }

    public int getOutputSideMask() {
        return this.output.getSideMask();
    }

    public void setOutputSideMask(int mask) {
        int clamped = mask & SourceOutputSettings.ALL_SIDES;
        if (this.output.getSideMask() == clamped) {
            return;
        }
        this.output.setSideMask(clamped);
        markUpdated();
    }

    /** Jar refill radius; 0 for machines without a radius setting. */
    public int getRefillRadius() {
        return 0;
    }

    /** Upper bound of the jar refill radius; 0 for machines without a radius setting. */
    public int getMaxRefillRadius() {
        return 0;
    }

    /** Applies a jar refill radius; returns false for machines without a radius setting. */
    public boolean setRefillRadius(int radius) {
        return false;
    }

    /** Whether the world-space range box is permanently shown; false for machines without one. */
    public boolean isRangePreviewEnabled() {
        return false;
    }

    /** Toggles the world-space range box; returns false for machines without one. */
    public boolean setRangePreviewEnabled(boolean enabled) {
        return false;
    }

    @Override
    public NBTTagCompound writeConfiguration() {
        NBTTagCompound configuration = new NBTTagCompound();
        configuration.setBoolean(KEY_OUTPUT_ENABLED, isOutputEnabled());
        configuration.setInteger(KEY_OUTPUT_SIDES, getOutputSideMask());
        configuration.setInteger(KEY_OUTPUT_RATE, getOutputItemsPerTick());
        if (getMaxRefillRadius() > 0) {
            configuration.setInteger("RefillRadius", getRefillRadius());
            configuration.setBoolean("RangePreview", isRangePreviewEnabled());
        }
        return configuration;
    }

    @Override
    public void applyConfiguration(NBTTagCompound configuration) {
        if (configuration == null) {
            return;
        }
        setOutputEnabled(configuration.getBoolean(KEY_OUTPUT_ENABLED));
        if (configuration.hasKey(KEY_OUTPUT_SIDES)) {
            setOutputSideMask(configuration.getInteger(KEY_OUTPUT_SIDES));
        }
        if (configuration.hasKey(KEY_OUTPUT_RATE)) {
            setOutputItemsPerTick(configuration.getInteger(KEY_OUTPUT_RATE));
        }
        if (getMaxRefillRadius() > 0) {
            if (configuration.hasKey("RefillRadius")) {
                setRefillRadius(configuration.getInteger("RefillRadius"));
            }
            if (configuration.hasKey("RangePreview")) {
                setRangePreviewEnabled(configuration.getBoolean("RangePreview"));
            }
        }
    }

    // ------------------------------------------------------------------
    // Server tick
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (this.loadAnnounceRepeats > 0) {
            if (--this.loadAnnounceCountdown <= 0) {
                this.loadAnnounceRepeats--;
                this.loadAnnounceCountdown = LOAD_ANNOUNCE_INTERVAL;
                if (isProviding()) {
                    notifyNeighbors();
                }
            }
        }
        if (!this.infinite || this.lockedAspect == null) {
            return;
        }
        fillTargetInPlace();
        onProvidingTick();
    }

    /** Pushes up to the configured amount from the target stack into enabled neighbouring inventories. */
    protected final void pushTargetOutput() {
        if (this.targetStack.isEmpty() || this.targetStack.getCount() <= 0) {
            return;
        }
        boolean changed = false;
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (this.targetStack.getCount() <= 0) {
                break;
            }
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            if (!this.world.isBlockLoaded(this.pos.offset(direction))) {
                continue;
            }
            TileEntity neighbor = this.world.getTileEntity(this.pos.offset(direction));
            if (!(neighbor instanceof IInventory)) {
                continue;
            }
            int moved = insertInto((IInventory) neighbor, this.targetStack,
                    (int) this.output.getRate());
            if (moved > 0) {
                changed = true;
            }
        }
        if (this.targetStack.getCount() <= 0) {
            this.targetStack = ItemStack.EMPTY;
        }
        if (changed) {
            markUpdated();
        }
    }

    /** Merges up to {@code limit} items of {@code source} into {@code target}, consuming the source. */
    private static int insertInto(IInventory target, ItemStack source, int limit) {
        int moved = 0;
        for (int slot = 0; slot < target.getSizeInventory() && moved < limit; slot++) {
            if (!target.isItemValidForSlot(slot, source)) {
                continue;
            }
            int slotLimit = Math.min(target.getInventoryStackLimit(), source.getMaxStackSize());
            ItemStack current = target.getStackInSlot(slot);
            if (current.isEmpty()) {
                int amount = Math.min(Math.min(limit - moved, slotLimit), source.getCount());
                if (amount <= 0) {
                    continue;
                }
                ItemStack placed = source.copy();
                placed.setCount(amount);
                target.setInventorySlotContents(slot, placed);
                source.shrink(amount);
                moved += amount;
            } else if (current.isItemEqual(source)
                    && ItemStack.areItemStackTagsEqual(current, source)) {
                int space = Math.min(slotLimit, current.getMaxStackSize()) - current.getCount();
                int amount = Math.min(Math.min(space, limit - moved), source.getCount());
                if (amount <= 0) {
                    continue;
                }
                current.grow(amount);
                source.shrink(amount);
                moved += amount;
                target.setInventorySlotContents(slot, current);
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------
    // IInventory
    // ------------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index == 0) {
            return this.sampleStack;
        }
        if (index == 1) {
            return this.targetStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (index < 0 || index > 1 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (index == 0 && isProviding()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = index == 0 ? this.sampleStack : this.targetStack;
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, source.getCount());
        ItemStack result = source.copy();
        result.setCount(taken);
        source.shrink(taken);
        if (source.getCount() <= 0) {
            if (index == 0) {
                this.sampleStack = ItemStack.EMPTY;
                this.lockedAspect = null;
            } else {
                this.targetStack = ItemStack.EMPTY;
            }
        }
        markUpdated();
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index == 0 && isProviding()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = getStackInSlot(index);
        if (index == 0) {
            this.sampleStack = ItemStack.EMPTY;
            this.lockedAspect = null;
        } else if (index == 1) {
            this.targetStack = ItemStack.EMPTY;
        }
        markUpdated();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == 0) {
            if (isProviding()) {
                return;
            }
            if (stack.isEmpty()) {
                this.sampleStack = ItemStack.EMPTY;
                this.lockedAspect = null;
            } else {
                this.sampleStack = stack.copy();
                this.lockedAspect = aspectOfSample(stack);
            }
            markUpdated();
            if (isProviding()) {
                notifyNeighbors();
            }
            return;
        }
        if (index == 1) {
            this.targetStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            markUpdated();
        }
    }

    @Override
    public String getName() {
        return getInventoryTranslationKey();
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return isUsable() && player.getDistanceSq(this.pos.getX() + 0.5D,
                this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 0) {
            return !isProviding() && isValidSample(stack);
        }
        return index == 1 && isValidTarget(stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return this.sampleStack.isEmpty() && this.targetStack.isEmpty();
    }

    @Override
    public void clear() {
        this.sampleStack = ItemStack.EMPTY;
        this.targetStack = ItemStack.EMPTY;
    }

    public boolean isUsable() {
        return this.world != null && this.world.getTileEntity(this.pos) == this;
    }

    // ------------------------------------------------------------------
    // Item handler capability (pipes and AE2 import/export buses)
    // ------------------------------------------------------------------

    private final IItemHandler itemHandler = new ArcaneItemHandler();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.itemHandler;
        }
        return super.getCapability(capability, facing);
    }

    /** Two-slot handler: slot 0 is the locked sample, slot 1 is the processing target. */
    private final class ArcaneItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return AbstractArcaneMachineTileEntity.this.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot > 1) {
                return stack;
            }
            if (!isItemValidForSlot(slot, stack)) {
                return stack;
            }
            ItemStack current = getStackInSlot(slot);
            int limit = Math.min(getInventoryStackLimit(), stack.getMaxStackSize());
            if (!current.isEmpty()) {
                if (!ItemStack.areItemsEqual(current, stack)
                        || !ItemStack.areItemStackTagsEqual(current, stack)) {
                    return stack;
                }
                limit = Math.min(limit, current.getMaxStackSize());
            }
            int space = limit - (current.isEmpty() ? 0 : current.getCount());
            if (space <= 0) {
                return stack;
            }
            int amount = Math.min(stack.getCount(), space);
            if (!simulate) {
                if (current.isEmpty()) {
                    ItemStack placed = stack.copy();
                    placed.setCount(amount);
                    setInventorySlotContents(slot, placed);
                } else {
                    ItemStack merged = current.copy();
                    merged.grow(amount);
                    setInventorySlotContents(slot, merged);
                }
            }
            ItemStack remaining = stack.copy();
            remaining.shrink(amount);
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot > 1) {
                return ItemStack.EMPTY;
            }
            if (slot == 0 && isProviding()) {
                return ItemStack.EMPTY;
            }
            ItemStack current = getStackInSlot(slot);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int taken = Math.min(amount, current.getCount());
            ItemStack result = current.copy();
            result.setCount(taken);
            if (!simulate) {
                decrStackSize(slot, taken);
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Math.min(getInventoryStackLimit(), 64);
        }
    }

    // ------------------------------------------------------------------
    // Persistence and sync
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        writeStack(compound, KEY_SAMPLE, this.sampleStack);
        writeStack(compound, KEY_TARGET, this.targetStack);
        if (this.lockedAspect != null) {
            compound.setString(KEY_ASPECT, this.lockedAspect.getTag());
        }
        compound.setBoolean(KEY_INFINITE, this.infinite);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.output.isEnabled());
        compound.setInteger(KEY_OUTPUT_SIDES, this.output.getSideMask());
        compound.setInteger(KEY_OUTPUT_RATE, (int) this.output.getRate());
        writeExtraNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.sampleStack = readStack(compound, KEY_SAMPLE);
        this.targetStack = readStack(compound, KEY_TARGET);
        this.lockedAspect = compound.hasKey(KEY_ASPECT)
                ? Aspect.getAspect(compound.getString(KEY_ASPECT)) : null;
        this.infinite = compound.getBoolean(KEY_INFINITE);
        this.output.setEnabled(compound.hasKey(KEY_OUTPUT_ENABLED, 1)
                ? compound.getBoolean(KEY_OUTPUT_ENABLED) : defaultOutputEnabled());
        this.output.setSideMask(compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : SourceOutputSettings.ALL_SIDES);
        this.output.setRate(compound.hasKey(KEY_OUTPUT_RATE, 3)
                ? compound.getInteger(KEY_OUTPUT_RATE) : defaultOutputRate());
        readExtraNBT(compound);
    }

    private static void writeStack(NBTTagCompound compound, String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            compound.setTag(key, stack.writeToNBT(new NBTTagCompound()));
        }
    }

    private static ItemStack readStack(NBTTagCompound compound, String key) {
        if (!compound.hasKey(key, 10)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(compound.getCompoundTag(key));
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    /** Tells adjacent blocks that this machine became (or stopped being) an essentia source. */
    private void notifyNeighbors() {
        if (this.world != null && !this.world.isRemote) {
            this.world.notifyNeighborsOfStateChange(this.pos, getBlockType(), true);
        }
    }

    private void notifyNeighborsIfProvidingChanged(boolean wasProviding) {
        if (wasProviding != isProviding()) {
            notifyNeighbors();
        }
    }

    /** Marks the chunk dirty and pushes a block update to nearby clients. */
    protected void markUpdated() {
        markDirty();
        if (this.world != null && !this.world.isRemote) {
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos),
                    this.world.getBlockState(this.pos), 3);
        }
    }
}
