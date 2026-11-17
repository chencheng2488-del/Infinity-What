package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftAltarControlManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * The Ordo Usurpatory Matrix tile entity.
 *
 * <p>The tile entity is intentionally only a settings holder and a binding retry timer. The
 * actual per-tick override is owned by {@link ThaumcraftAltarControlManager}, because the matrix
 * block can be broken while an infusion is still running.
 */
public class OrdoUsurpatoryMatrixTileEntity extends TileEntity
        implements ITickable, IArcaneConfigurable {

    public enum StabilityMode {
        VANILLA,
        FORCE,
        ALWAYS_STABLE,
        ALWAYS_UNSTABLE
    }

    private static final String KEY_SPEED = "Speed";
    private static final String KEY_STABILITY_MODE = "StabilityMode";
    private static final String KEY_STABILITY_VALUE = "StabilityValue";
    private static final String KEY_BOUND_ALTAR = "BoundAltar";

    private static final int BIND_RETRY_INTERVAL = 20;

    private float speedMultiplier = 1.0F;
    private StabilityMode stabilityMode = StabilityMode.VANILLA;
    private float stabilityValue;
    private int tickCounter;

    @Nullable
    private BlockPos boundAltarPos;

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (this.tickCounter++ % BIND_RETRY_INTERVAL != 0) {
            return;
        }
        refreshBinding();
    }

    private void refreshBinding() {
        BlockPos altarPos = ThaumcraftAltarControlManager.findAndBind(this.world, this);
        if (altarPos != null) {
            if (!altarPos.equals(this.boundAltarPos)) {
                this.boundAltarPos = altarPos;
                markUpdated();
            }
            ThaumcraftAltarControlManager.updateSettings(this.world, this);
            return;
        }
        if (this.boundAltarPos != null) {
            ThaumcraftAltarControlManager.unbind(this.world, this.pos);
            this.boundAltarPos = null;
            markUpdated();
        }
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

    @Nullable
    public BlockPos getBoundAltarPos() {
        return this.boundAltarPos;
    }

    public boolean isBound() {
        return this.boundAltarPos != null;
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
        ThaumcraftAltarControlManager.updateSettings(this.world, this);
    }

    /** Clamps a speed value from NBT or from a client packet; NaN/infinity falls back to 1.0. */
    private static float clampSpeed(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 1.0F;
        }
        return Math.max(1.0F, Math.min((float) InfinityWhatConfig.ordoMatrixMaxSpeed(), value));
    }

    /** Clamps a stability value from NBT or from a client packet; NaN/infinity falls back to 0. */
    private static float clampStability(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(-100.0F,
                Math.min((float) InfinityWhatConfig.ordoMatrixMaxStability(), value));
    }

    private void markUpdated() {
        markDirty();
        if (this.world != null) {
            net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
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
        this.boundAltarPos = compound.hasKey(KEY_BOUND_ALTAR)
                ? readPos(compound.getCompoundTag(KEY_BOUND_ALTAR))
                : null;
    }

    private static BlockPos readPos(NBTTagCompound tag) {
        return new BlockPos(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
    }

    private static NBTTagCompound writePos(BlockPos pos) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("X", pos.getX());
        tag.setInteger("Y", pos.getY());
        tag.setInteger("Z", pos.getZ());
        return tag;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat(KEY_SPEED, this.speedMultiplier);
        compound.setInteger(KEY_STABILITY_MODE, this.stabilityMode.ordinal());
        compound.setFloat(KEY_STABILITY_VALUE, this.stabilityValue);
        if (this.boundAltarPos != null) {
            compound.setTag(KEY_BOUND_ALTAR, writePos(this.boundAltarPos));
        }
        return compound;
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
}
