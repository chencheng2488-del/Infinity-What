package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * The Ordo Usurpatory Matrix tile entity.
 *
 * <p>The tile entity is intentionally only a settings holder and a binding retry timer. The
 * actual per-tick override is owned by {@link ThaumcraftAltarControlManager}, because the matrix
 * block can be broken while an infusion is still running.
 */
public class OrdoUsurpatoryMatrixTileEntity extends TileEntity implements IArcaneConfigurable {

    public enum StabilityMode {
        VANILLA,
        FORCE,
        ALWAYS_STABLE,
        ALWAYS_UNSTABLE
    }

    private static final String KEY_SPEED = "Speed";
    private static final String KEY_STABILITY_MODE = "StabilityMode";
    private static final String KEY_STABILITY_VALUE = "StabilityValue";
    private static final String KEY_HAS_BOUND_ALTAR = "HasBoundAltar";
    private static final String KEY_BOUND_X = "BoundX";
    private static final String KEY_BOUND_Y = "BoundY";
    private static final String KEY_BOUND_Z = "BoundZ";

    private static final int BIND_RETRY_INTERVAL = 20;

    private float speedMultiplier = 1.0F;
    private StabilityMode stabilityMode = StabilityMode.VANILLA;
    private float stabilityValue;
    private int tickCounter;

    private boolean hasBoundAltar;
    private int boundAltarX;
    private int boundAltarY;
    private int boundAltarZ;

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        if (this.tickCounter++ % BIND_RETRY_INTERVAL != 0) {
            return;
        }
        refreshBinding();
    }

    private void refreshBinding() {
        ThaumcraftAltarControlManager.AltarPos altarPos =
                ThaumcraftAltarControlManager.findAndBind(this.worldObj, this);
        if (altarPos != null) {
            if (!isBoundTo(altarPos)) {
                this.hasBoundAltar = true;
                this.boundAltarX = altarPos.x;
                this.boundAltarY = altarPos.y;
                this.boundAltarZ = altarPos.z;
                markUpdated();
            }
            ThaumcraftAltarControlManager.updateSettings(this.worldObj, this);
            return;
        }
        if (this.hasBoundAltar) {
            ThaumcraftAltarControlManager.unbind(this.worldObj, this.xCoord, this.yCoord,
                    this.zCoord);
            this.hasBoundAltar = false;
            markUpdated();
        }
    }

    private boolean isBoundTo(ThaumcraftAltarControlManager.AltarPos altarPos) {
        return this.hasBoundAltar && this.boundAltarX == altarPos.x
                && this.boundAltarY == altarPos.y && this.boundAltarZ == altarPos.z;
    }

    public float getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public StabilityMode getStabilityMode() {
        return this.stabilityMode;
    }

    public float getStabilityValue() {
        return this.stabilityValue;
    }

    /** Upper bound for the FORCE value, shared with the GUI. */
    public static float maxStabilityValue() {
        return (float) InfinityWhatConfig.ordoMatrixMaxStability();
    }

    public boolean isBound() {
        return this.hasBoundAltar;
    }

    public int getBoundAltarX() {
        return this.boundAltarX;
    }

    public int getBoundAltarY() {
        return this.boundAltarY;
    }

    public int getBoundAltarZ() {
        return this.boundAltarZ;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The altar binding is positional and is deliberately left out of the profile, so a copied
     * matrix never points another matrix at someone else's altar.
     */
    @Override
    public String getConfigurationType() {
        return "ordo";
    }

    @Override
    public NBTTagCompound writeConfiguration() {
        NBTTagCompound configuration = new NBTTagCompound();
        configuration.setFloat("Speed", this.speedMultiplier);
        configuration.setInteger("StabilityMode", this.stabilityMode.ordinal());
        configuration.setFloat("StabilityValue", this.stabilityValue);
        return configuration;
    }

    @Override
    public void applyConfiguration(NBTTagCompound configuration) {
        if (configuration == null) {
            return;
        }
        StabilityMode[] modes = StabilityMode.values();
        int modeIndex = Math.max(0, Math.min(modes.length - 1,
                configuration.getInteger("StabilityMode")));
        setSettings(configuration.getFloat("Speed"), modes[modeIndex],
                configuration.getFloat("StabilityValue"));
    }

    public void setSettings(float speedMultiplier, StabilityMode mode, float stabilityValue) {
        float clampedSpeed = clampSpeed(speedMultiplier);
        float clampedValue = clampStability(stabilityValue);
        if (this.speedMultiplier == clampedSpeed && this.stabilityMode == mode
                && this.stabilityValue == clampedValue) {
            return;
        }
        this.speedMultiplier = clampedSpeed;
        this.stabilityMode = mode;
        this.stabilityValue = clampedValue;
        markUpdated();
        ThaumcraftAltarControlManager.updateSettings(this.worldObj, this);
    }

    /** Clamps a speed value from NBT or from a client packet; NaN/infinity falls back to 1.0. */
    private static float clampSpeed(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 1.0F;
        }
        return Math.max(1.0F, Math.min((float) InfinityWhatConfig.ordoMatrixMaxSpeed(), value));
    }

    /** Clamps an instability value from NBT or from a client packet; TC4 uses 0..25. */
    private static float clampStability(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F,
                Math.min((float) InfinityWhatConfig.ordoMatrixMaxStability(), value));
    }

    private void markUpdated() {
        markDirty();
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.speedMultiplier = clampSpeed(compound.getFloat(KEY_SPEED));
        int mode = compound.getInteger(KEY_STABILITY_MODE);
        StabilityMode[] modes = StabilityMode.values();
        this.stabilityMode = modes[Math.max(0, Math.min(modes.length - 1, mode))];
        this.stabilityValue = clampStability(compound.getFloat(KEY_STABILITY_VALUE));
        this.hasBoundAltar = compound.getBoolean(KEY_HAS_BOUND_ALTAR);
        this.boundAltarX = compound.getInteger(KEY_BOUND_X);
        this.boundAltarY = compound.getInteger(KEY_BOUND_Y);
        this.boundAltarZ = compound.getInteger(KEY_BOUND_Z);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat(KEY_SPEED, this.speedMultiplier);
        compound.setInteger(KEY_STABILITY_MODE, this.stabilityMode.ordinal());
        compound.setFloat(KEY_STABILITY_VALUE, this.stabilityValue);
        compound.setBoolean(KEY_HAS_BOUND_ALTAR, this.hasBoundAltar);
        compound.setInteger(KEY_BOUND_X, this.boundAltarX);
        compound.setInteger(KEY_BOUND_Y, this.boundAltarY);
        compound.setInteger(KEY_BOUND_Z, this.boundAltarZ);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        writeToNBT(compound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
}
