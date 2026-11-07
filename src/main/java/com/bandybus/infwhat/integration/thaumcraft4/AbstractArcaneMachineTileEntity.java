package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.core.SourceOutputSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.aspects.Aspect;

/**
 * Shared inventory, lock and active-output logic for the two Thaumcraft 4 arcane machines.
 *
 * <p>Subclasses only provide the sample predicate, the aspect reader, the target predicate and the
 * per-tick action that distinguishes an aspect replicator from an essentia source.
 */
public abstract class AbstractArcaneMachineTileEntity extends TileEntity
        implements IInventory, IArcaneConfigurable {

    protected static final String KEY_SAMPLE = "Sample";
    protected static final String KEY_TARGET = "Target";
    protected static final String KEY_ASPECT = "LockedAspect";
    protected static final String KEY_INFINITE = "Infinite";
    protected static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    protected static final String KEY_OUTPUT_SIDES = "OutputSides";
    protected static final String KEY_OUTPUT_RATE = "OutputRate";

    /** Upper bound for the configurable output rate. */
    public static final int MAX_OUTPUT_RATE = 1_000_000;

    protected ItemStack sampleStack;
    protected ItemStack targetStack;
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
        return this.sampleStack == null ? null : this.sampleStack.copy();
    }

    public ItemStack getTargetStack() {
        return this.targetStack == null ? null : this.targetStack.copy();
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

    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return this.output.isSideEnabled(direction);
    }

    public void toggleOutputSide(ForgeDirection direction) {
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
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
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
        ItemStack source = this.targetStack;
        if (source == null || source.stackSize <= 0) {
            return;
        }
        boolean changed = false;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (source.stackSize <= 0) {
                break;
            }
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            int x = this.xCoord + direction.offsetX;
            int y = this.yCoord + direction.offsetY;
            int z = this.zCoord + direction.offsetZ;
            if (!this.worldObj.blockExists(x, y, z)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(x, y, z);
            if (!(neighbor instanceof IInventory)) {
                continue;
            }
            int moved = insertInto((IInventory) neighbor, source, (int) this.output.getRate());
            if (moved > 0) {
                changed = true;
            }
        }
        if (source.stackSize <= 0) {
            this.targetStack = null;
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
            if (current == null) {
                int amount = Math.min(Math.min(limit - moved, slotLimit), source.stackSize);
                if (amount <= 0) {
                    continue;
                }
                ItemStack placed = source.copy();
                placed.stackSize = amount;
                target.setInventorySlotContents(slot, placed);
                source.stackSize -= amount;
                moved += amount;
            } else if (current.isItemEqual(source)
                    && ItemStack.areItemStackTagsEqual(current, source)) {
                int space = Math.min(slotLimit, current.getMaxStackSize()) - current.stackSize;
                int amount = Math.min(Math.min(space, limit - moved), source.stackSize);
                if (amount <= 0) {
                    continue;
                }
                current.stackSize += amount;
                source.stackSize -= amount;
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
        return null;
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (index < 0 || index > 1 || amount <= 0) {
            return null;
        }
        if (index == 0 && isProviding()) {
            return null;
        }
        ItemStack source = index == 0 ? this.sampleStack : this.targetStack;
        if (source == null || source.stackSize <= 0) {
            return null;
        }
        int taken = Math.min(amount, source.stackSize);
        ItemStack result = source.copy();
        result.stackSize = taken;
        source.stackSize -= taken;
        if (source.stackSize <= 0) {
            if (index == 0) {
                this.sampleStack = null;
                this.lockedAspect = null;
            } else {
                this.targetStack = null;
            }
        }
        markUpdated();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == 0) {
            if (isProviding()) {
                return;
            }
            if (stack == null) {
                this.sampleStack = null;
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
            this.targetStack = stack == null ? null : stack.copy();
            markUpdated();
        }
    }

    @Override
    public String getInventoryName() {
        return getInventoryTranslationKey();
    }

    @Override
    public boolean isCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return isUsable() && player.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D,
                this.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openChest() {
    }

    @Override
    public void closeChest() {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 0) {
            return !isProviding() && isValidSample(stack);
        }
        return index == 1 && isValidTarget(stack);
    }

    public boolean isUsable() {
        return this.worldObj != null
                && this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this;
    }

    // ------------------------------------------------------------------
    // Persistence and sync
    // ------------------------------------------------------------------

    @Override
    public void writeToNBT(NBTTagCompound compound) {
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
        if (stack != null && stack.stackSize > 0) {
            compound.setTag(key, stack.writeToNBT(new NBTTagCompound()));
        }
    }

    private static ItemStack readStack(NBTTagCompound compound, String key) {
        if (!compound.hasKey(key, 10)) {
            return null;
        }
        ItemStack stack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(key));
        return stack != null && stack.stackSize > 0 ? stack : null;
    }

    @Override
    public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    /** Tells adjacent blocks that this machine became (or stopped being) an essentia source. */
    private void notifyNeighbors() {
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord,
                    getBlockType());
        }
    }

    private void notifyNeighborsIfProvidingChanged(boolean wasProviding) {
        if (wasProviding != isProviding()) {
            notifyNeighbors();
        }
    }

    /** Marks the chunk dirty and pushes a block update to nearby clients. */
    protected void markUpdated() {
        this.markDirty();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }
}
