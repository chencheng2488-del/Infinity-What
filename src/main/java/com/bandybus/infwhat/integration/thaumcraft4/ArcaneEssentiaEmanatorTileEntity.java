package com.bandybus.infwhat.integration.thaumcraft4;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import com.bandybus.infwhat.api.IRangePreviewProvider;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileJarFillable;

import java.util.ArrayList;
import java.util.List;

/**
 * Arcane Essentia Emanator tile entity.
 *
 * <p>The block is the source side of the suction model: zero suction, extraction allowed and an
 * unlimited essentia amount. It also tops up Thaumcraft jars inside the configured cubic radius.
 */
@SuppressWarnings("deprecation")
public class ArcaneEssentiaEmanatorTileEntity extends AbstractArcaneMachineTileEntity
        implements IEssentiaTransport, IAspectContainer, IRangePreviewProvider {

    private static final String KEY_REFILL_RADIUS = "RefillRadius";
    private static final String KEY_RANGE_PREVIEW = "RangePreview";
    /** Default cubic radius in blocks. */
    public static final int DEFAULT_REFILL_RADIUS = 1;
    /** Upper bound for the refill radius. */
    public static final int MAX_REFILL_RADIUS = 8;

    private static final int REPORTED_AMOUNT = Integer.MAX_VALUE;

    private int refillRadius = DEFAULT_REFILL_RADIUS;
    private boolean rangePreviewEnabled;
    private final List<int[]> jarTargets = new ArrayList<int[]>();
    private int scanCooldown;
    private int scannedRadius = -1;

    @Override
    protected int defaultOutputRate() {
        return 1;
    }

    @Override
    protected boolean defaultOutputEnabled() {
        return true;
    }

    @Override
    public String getConfigurationType() {
        return "emanator";
    }

    @Override
    protected String getInventoryTranslationKey() {
        return "container.infwhat.arcane_essentia_emanator";
    }

    @Override
    protected boolean isValidSample(ItemStack stack) {
        return ArcaneMachineHelper.isEssentiaSample(stack);
    }

    @Override
    protected Aspect aspectOfSample(ItemStack stack) {
        return ArcaneMachineHelper.essentiaSampleAspect(stack);
    }

    @Override
    protected boolean isValidTarget(ItemStack stack) {
        return ArcaneMachineHelper.canStoreEssentia(stack);
    }

    @Override
    protected boolean fillTargetInPlace() {
        if (this.targetStack == null || this.targetStack.stackSize <= 0) {
            return false;
        }
        if (!ArcaneMachineHelper.fillEssentia(this.targetStack, this.lockedAspect)) {
            return false;
        }
        markUpdated();
        return true;
    }

    @Override
    protected void onProvidingTick() {
        if (isOutputEnabled()) {
            refillJars();
        }
    }

    @Override
    public int getRefillRadius() {
        return this.refillRadius;
    }

    @Override
    public int getMaxRefillRadius() {
        return MAX_REFILL_RADIUS;
    }

    @Override
    public boolean isRangePreviewEnabled() {
        return this.rangePreviewEnabled;
    }

    @Override
    public boolean setRangePreviewEnabled(boolean enabled) {
        if (this.rangePreviewEnabled == enabled) {
            return true;
        }
        this.rangePreviewEnabled = enabled;
        markUpdated();
        return true;
    }

    @Override
    public int getRangePreviewRadius() {
        return this.refillRadius;
    }

    @Override
    public boolean setRefillRadius(int radius) {
        int clamped = Math.min(MAX_REFILL_RADIUS, Math.max(1, radius));
        if (this.refillRadius == clamped) {
            return true;
        }
        this.refillRadius = clamped;
        this.scanCooldown = 0;
        this.scannedRadius = -1;
        markUpdated();
        return true;
    }

    /** Tops up every cached jar inside the configured radius. */
    private void refillJars() {
        if (this.scannedRadius != this.refillRadius || this.scanCooldown <= 0) {
            refreshJarTargets();
            this.scannedRadius = this.refillRadius;
            this.scanCooldown = 20;
        } else {
            this.scanCooldown--;
        }
        for (int[] position : this.jarTargets) {
            int x = position[0];
            int y = position[1];
            int z = position[2];
            if (!this.worldObj.blockExists(x, y, z)) {
                continue;
            }
            TileEntity tile = this.worldObj.getTileEntity(x, y, z);
            if (tile instanceof TileJarFillable) {
                ((TileJarFillable) tile).addToContainer(this.lockedAspect,
                        getOutputItemsPerTick());
            }
        }
    }

    /** Rebuilds the cached list of jars inside the configured cube. */
    private void refreshJarTargets() {
        this.jarTargets.clear();
        int radius = Math.min(MAX_REFILL_RADIUS, Math.max(1, this.refillRadius));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    int x = this.xCoord + dx;
                    int y = this.yCoord + dy;
                    int z = this.zCoord + dz;
                    if (!this.worldObj.blockExists(x, y, z)) {
                        continue;
                    }
                    if (this.worldObj.getTileEntity(x, y, z) instanceof TileJarFillable) {
                        this.jarTargets.add(new int[]{x, y, z});
                    }
                }
            }
        }
    }

    @Override
    protected void writeExtraNBT(NBTTagCompound compound) {
        compound.setInteger(KEY_REFILL_RADIUS, this.refillRadius);
        compound.setBoolean(KEY_RANGE_PREVIEW, this.rangePreviewEnabled);
    }

    @Override
    protected void readExtraNBT(NBTTagCompound compound) {
        this.refillRadius = compound.hasKey(KEY_REFILL_RADIUS, 3)
                ? Math.min(MAX_REFILL_RADIUS,
                Math.max(1, compound.getInteger(KEY_REFILL_RADIUS)))
                : DEFAULT_REFILL_RADIUS;
        this.rangePreviewEnabled = compound.getBoolean(KEY_RANGE_PREVIEW);
        this.scannedRadius = -1;
        this.scanCooldown = 0;
    }

    // ------------------------------------------------------------------
    // IEssentiaTransport (source side of the suction model)
    // ------------------------------------------------------------------

    @Override
    public boolean isConnectable(ForgeDirection side) {
        return true;
    }

    @Override
    public boolean canInputFrom(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean canOutputTo(ForgeDirection side) {
        return isOutputEnabled() && isProviding() && isOutputSideEnabled(side);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // A source does not pull; tubes pull from it.
    }

    @Override
    public Aspect getSuctionType(ForgeDirection side) {
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection side) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        if (!canOutputTo(side) || aspect != this.lockedAspect) {
            return 0;
        }
        return Math.min(Math.max(0, amount), getOutputItemsPerTick());
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection side) {
        return this.lockedAspect;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection side) {
        return isProviding() ? REPORTED_AMOUNT : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ------------------------------------------------------------------
    // IAspectContainer
    // ------------------------------------------------------------------

    @Override
    public AspectList getAspects() {
        if (!isProviding()) {
            return new AspectList();
        }
        return new AspectList().add(this.lockedAspect, REPORTED_AMOUNT);
    }

    @Override
    public void setAspects(AspectList aspects) {
        // The source is read-only.
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return isProviding() && aspect == this.lockedAspect && amount > 0;
    }

    @Override
    public boolean takeFromContainer(AspectList aspects) {
        if (!isProviding() || aspects == null) {
            return false;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect != this.lockedAspect) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return isProviding() && aspect == this.lockedAspect && amount > 0;
    }

    @Override
    public boolean doesContainerContain(AspectList aspects) {
        if (!isProviding() || aspects == null) {
            return false;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect != this.lockedAspect) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return isProviding() && aspect == this.lockedAspect ? REPORTED_AMOUNT : 0;
    }
}
