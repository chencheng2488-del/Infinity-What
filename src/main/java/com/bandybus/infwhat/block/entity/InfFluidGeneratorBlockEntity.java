package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;
import com.bandybus.infwhat.fluid.InfFluidKind;
import net.minecraftforge.common.util.Constants;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Stores a single fluid kind (water or lava) plus an amount in millibuckets. Forge 25 for
 * Minecraft 1.13.2 ships a placeholder fluid registry and FluidStack, so this tank is fully
 * self-contained: buckets are handled directly and no Forge fluid capability is exposed.
 * When the infinite source is enabled, inserting any allowed fluid immediately turns it into
 * an unlimited source; the GUI presents the conceptual amount as {@code Long.MAX_VALUE} buckets.
 */
public class InfFluidGeneratorBlockEntity extends TileEntity
        implements IInteractionObject, ITickable {

    private static final String KEY_TANK = "Tank";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_REDSTONE_MODE = "RedstoneMode";
    private static final String KEY_PULSE_ON = "PulseOnTicks";
    private static final String KEY_PULSE_OFF = "PulseOffTicks";
    private static final String KEY_BATCH_SIZE = "BatchSize";
    /** Legacy single-rate keys, only read for 2.1.x saves. */
    private static final String KEY_LEGACY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_LEGACY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    private static final String KEY_DATA_VERSION = "InfDataVersion";
    private static final int DATA_VERSION = 2;
    private static final int SIDE_COUNT = EnumFacing.values().length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();
    private static final long DEFAULT_OUTPUT_RATE_MB = 1_000L;
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    @Nullable
    private InfFluidKind tankKind = null;
    private int tankAmount;
    private boolean infinite = false;
    private boolean outputEnabled = false;
    private int outputSideMask = ALL_SIDES;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private int pulseOnTicks = DEFAULT_PULSE_TICKS;
    private int pulseOffTicks = DEFAULT_PULSE_TICKS;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int batchCounter;
    private final OutputStatus outputStatus = new OutputStatus();
    private int pushStartSide;
    /** Per-side output rate in mB/tick; 0 means the side produces nothing. */
    private final long[] sideRates = new long[SIDE_COUNT];
    /** Last non-zero rate per side, so toggling a side back on restores a useful value. */
    private final long[] lastNonZeroRates = new long[SIDE_COUNT];

    public InfFluidGeneratorBlockEntity() {
        super(ModBlockEntities.INF_FLUID_GENERATOR);
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE_MB);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE_MB);
    }

    private static String[] createSideRateKeys() {
        EnumFacing[] directions = EnumFacing.values();
        String[] keys = new String[directions.length];
        for (EnumFacing direction : directions) {
            String name = direction.getName();
            keys[direction.getIndex()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static int sideIndex(EnumFacing direction) {
        return direction.getIndex();
    }

    // ------------------------------------------------------------------
    // Identity / container
    // ------------------------------------------------------------------

    @Override
    public ITextComponent getName() {
        return new TextComponentTranslation("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getCustomName() {
        return null;
    }

    @Override
    public String getGuiID() {
        return InfinityWhat.MOD_ID + ":inf_fluid_generator";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new InfFluidGeneratorMenu(playerInventory, this);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    /** The stored fluid kind, or {@code null} when the tank is empty. */
    @Nullable
    public InfFluidKind getStoredKind() {
        return this.tankKind;
    }

    public int getTankAmount() {
        return this.tankAmount;
    }

    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    /**
     * The persisted {@code infinite} flag is only honoured while the balance switch is enabled,
     * so turning {@code balance.enableInfiniteSource} off also disables generators that were
     * already placed and converted to infinite sources.
     */
    private boolean effectivelyInfinite() {
        return this.infinite && this.tankKind != null && InfinityWhatConfig.infiniteEnabled();
    }

    /** Display amount: Long.MAX_VALUE buckets when infinite, otherwise the stored mB amount. */
    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : (this.tankKind == null ? 0L : this.tankAmount);
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
        return (this.outputSideMask & (1 << direction.getIndex())) != 0;
    }

    public void toggleOutputSide(EnumFacing direction) {
        int index = sideIndex(direction);
        boolean enabling = !isOutputSideEnabled(direction);
        long restoredRate = this.sideRates[index];
        if (enabling && restoredRate <= 0L) {
            restoredRate = this.lastNonZeroRates[index] > 0L
                    ? this.lastNonZeroRates[index] : DEFAULT_OUTPUT_RATE_MB;
        }
        if (enabling
                && getTotalEffectiveRateMb() + restoredRate > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return;
        }
        this.outputSideMask ^= 1 << index;
        if (enabling && this.sideRates[index] <= 0L) {
            this.sideRates[index] = restoredRate;
        }
        markUpdated();
    }

    /** Rate configured for one side, in mB/tick. 0 means the side produces nothing. */
    public long getSideRateMb(EnumFacing direction) {
        return this.sideRates[sideIndex(direction)];
    }

    /** Rate actually used for one side; a disabled side always reports 0. */
    public long getEffectiveSideRateMb(EnumFacing direction) {
        return isOutputSideEnabled(direction) ? this.sideRates[sideIndex(direction)] : 0L;
    }

    /** Sum of the effective per-side rates. */
    public long getTotalEffectiveRateMb() {
        long total = 0L;
        for (int i = 0; i < this.sideRates.length; i++) {
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += this.sideRates[i];
            }
        }
        return total;
    }

    /** The common rate when all six sides share one value, otherwise -1. */
    public long getUniformSideRateMb() {
        long first = this.sideRates[0];
        for (int i = 1; i < this.sideRates.length; i++) {
            if (this.sideRates[i] != first) {
                return -1L;
            }
        }
        return first;
    }

    public boolean hasMixedSideRates() {
        return getUniformSideRateMb() < 0L;
    }

    /**
     * Sets one side's rate. Returns false without changing anything when the value is outside
     * the server limits or would push the total above the configured cap.
     */
    public boolean setSideRateMb(EnumFacing direction, long millibuckets) {
        if (millibuckets < 0L || millibuckets > InfinityWhatConfig.maxFluidRatePerSideMb()) {
            return false;
        }
        int index = sideIndex(direction);
        if (this.sideRates[index] == millibuckets) {
            return true;
        }
        long newTotal = getTotalEffectiveRateMb();
        if (isOutputSideEnabled(direction)) {
            newTotal = newTotal - getEffectiveSideRateMb(direction) + millibuckets;
        }
        if (newTotal > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        this.sideRates[index] = millibuckets;
        if (millibuckets > 0L) {
            this.lastNonZeroRates[index] = millibuckets;
        }
        markUpdated();
        return true;
    }

    /** Applies one rate to all six sides, as the 2.2 phase-1 GUI does. */
    public boolean setUniformSideRateMb(long millibuckets) {
        long[] rates = new long[this.sideRates.length];
        Arrays.fill(rates, millibuckets);
        return applyFullConfiguration(this.outputEnabled, this.outputSideMask, rates);
    }

    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    public RedstoneMode getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode == null || this.redstoneMode == mode) {
            return;
        }
        this.redstoneMode = mode;
        markUpdated();
    }

    public int getPulseOnTicks() {
        return this.pulseOnTicks;
    }

    public int getPulseOffTicks() {
        return this.pulseOffTicks;
    }

    /** Sets both pulse phases; returns false without changing anything when out of range. */
    public boolean setPulseTicks(int onTicks, int offTicks) {
        if (onTicks < MIN_PULSE_TICKS || onTicks > MAX_PULSE_TICKS
                || offTicks < MIN_PULSE_TICKS || offTicks > MAX_PULSE_TICKS) {
            return false;
        }
        if (this.pulseOnTicks == onTicks && this.pulseOffTicks == offTicks) {
            return true;
        }
        this.pulseOnTicks = onTicks;
        this.pulseOffTicks = offTicks;
        markUpdated();
        return true;
    }

    /**
     * Whether the redstone gate currently allows output. {@code PULSE} ignores the incoming
     * signal and runs on its own work/stop cycle; the cycle is offset by the block position so
     * neighbouring generators do not all fire on the same tick.
     */
    public boolean isRedstoneAllowed() {
        if (this.world == null) {
            return this.redstoneMode == RedstoneMode.ALWAYS;
        }
        switch (this.redstoneMode) {
            case HIGH:
                return this.world.isBlockPowered(this.pos);
            case LOW:
                return !this.world.isBlockPowered(this.pos);
            case PULSE:
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(
                        this.world.getGameTime() + this.pos.toLong(), period);
                return phase < this.pulseOnTicks;
            case ALWAYS:
            default:
                return true;
        }
    }

    /**
     * Comparator strength: 0 while disabled or gated, otherwise 1-15 proportional to the
     * configured output in buckets per tick.
     */
    public int getComparatorOutput() {
        if (!this.outputEnabled || !isRedstoneAllowed()) {
            return 0;
        }
        long total = getTotalEffectiveRateMb();
        if (total <= 0L) {
            return 0;
        }
        long buckets = Math.max(1L, total / 1_000L);
        return (int) Math.min(15L, buckets);
    }

    /**
     * Replaces the whole output configuration in one step. Everything is validated before any
     * field is written, so a rejected paste can never leave a half-applied configuration.
     */
    public boolean applyFullConfiguration(boolean enabled, int sideMask, long[] rates) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        int mask = sideMask & ALL_SIDES;
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        long[] validated = new long[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0L || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((mask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        this.outputEnabled = enabled;
        this.outputSideMask = mask;
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0L) {
                this.lastNonZeroRates[i] = validated[i];
            }
        }
        markUpdated();
        return true;
    }

    /** Legacy global-rate entry point kept for old cards; all six sides get the same rate. */
    public boolean applyOutputConfiguration(boolean enabled, int sideMask, long rateMb) {
        long[] rates = new long[this.sideRates.length];
        Arrays.fill(rates, Math.max(1L, rateMb));
        return applyFullConfiguration(enabled, sideMask, rates);
    }

    /** Applies only the output switch and side mask; rates and redstone stay put. */
    public boolean applyOutputState(boolean enabled, int sideMask) {
        int mask = sideMask & ALL_SIDES;
        if (this.outputEnabled == enabled && this.outputSideMask == mask) {
            return true;
        }
        this.outputEnabled = enabled;
        this.outputSideMask = mask;
        markUpdated();
        return true;
    }

    /** Applies only per-side rates, validated against the current side mask and server limits. */
    public boolean applyRates(long[] rates) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        long[] validated = new long[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0L || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0L) {
                this.lastNonZeroRates[i] = validated[i];
            }
        }
        markUpdated();
        return true;
    }

    /** Applies only the redstone mode and pulse timing. */
    public boolean applyRedstoneConfiguration(RedstoneMode mode, int onTicks, int offTicks) {
        if (mode == null || onTicks < MIN_PULSE_TICKS || onTicks > MAX_PULSE_TICKS
                || offTicks < MIN_PULSE_TICKS || offTicks > MAX_PULSE_TICKS) {
            return false;
        }
        this.redstoneMode = mode;
        this.pulseOnTicks = onTicks;
        this.pulseOffTicks = offTicks;
        markUpdated();
        return true;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    /** Sets how many ticks of output are grouped into one burst; 1 keeps per-tick output. */
    public boolean setBatchSize(int size) {
        if (size < MIN_BATCH_SIZE || size > MAX_BATCH_SIZE) {
            return false;
        }
        if (this.batchSize == size) {
            return true;
        }
        this.batchSize = size;
        this.batchCounter = 0;
        markUpdated();
        return true;
    }

    public void clearFluid() {
        if (this.tankKind == null && !this.infinite) {
            return;
        }
        this.tankKind = null;
        this.tankAmount = 0;
        this.infinite = false;
        markUpdated();
    }

    public float getFillRatio() {
        if (effectivelyInfinite()) {
            return 1.0F;
        }
        if (this.tankKind == null) {
            return 0.0F;
        }
        int capacity = InfinityWhatConfig.finiteCapacity();
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tankAmount / (float) capacity);
    }

    /**
     * Bucket-level fill used by the GUI and the block interaction. Forge 25's fluid registry is
     * a placeholder, so the tank accepts a known fluid kind and an amount of millibuckets.
     */
    public int fillTank(InfFluidKind kind, int amount, boolean doFill) {
        if (kind == null || amount <= 0 || isDisallowed(kind)) {
            return 0;
        }
        if (this.tankKind != null && this.tankKind != kind) {
            return 0;
        }
        if (InfinityWhatConfig.infiniteEnabled()) {
            if (doFill) {
                this.tankKind = kind;
                this.tankAmount = 1;
                this.infinite = true;
                markUpdated();
            }
            return amount;
        }
        int capacity = InfinityWhatConfig.finiteCapacity();
        int current = this.tankKind == null ? 0 : this.tankAmount;
        int accepted = Math.min(capacity - current, amount);
        if (accepted <= 0) {
            return 0;
        }
        if (doFill) {
            this.tankKind = kind;
            this.tankAmount = current + accepted;
            markUpdated();
        }
        return accepted;
    }

    /** Drains at most {@code amount} mB; an infinite source never depletes. */
    public int drainTank(int amount, boolean doDrain) {
        if (this.tankKind == null || amount <= 0) {
            return 0;
        }
        if (effectivelyInfinite()) {
            return amount;
        }
        int drained = Math.min(amount, this.tankAmount);
        if (doDrain) {
            this.tankAmount -= drained;
            if (this.tankAmount <= 0) {
                this.tankKind = null;
                this.tankAmount = 0;
                this.infinite = false;
            }
            markUpdated();
        }
        return drained;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.tankKind = null;
        this.tankAmount = 0;
        if (tag.hasKey("TankKind", Constants.NBT.TAG_STRING)) {
            this.tankKind = InfFluidKind.byName(tag.getString("TankKind"));
            this.tankAmount = Math.max(0, tag.getInteger("TankAmount"));
        } else if (tag.hasKey(KEY_TANK, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound tankTag = tag.getCompoundTag(KEY_TANK);
            String name = tankTag.hasKey("FluidName", Constants.NBT.TAG_STRING)
                    ? tankTag.getString("FluidName") : "";
            this.tankKind = InfFluidKind.byName(name);
            this.tankAmount = Math.max(0, tankTag.getInteger("Amount"));
        }
        if (this.tankKind == null || this.tankAmount <= 0) {
            this.tankKind = null;
            this.tankAmount = 0;
        }
        this.infinite = tag.getBoolean(KEY_INFINITE) && this.tankKind != null;
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.redstoneMode = RedstoneMode.byId(tag.getInteger(KEY_REDSTONE_MODE));
        this.pulseOnTicks = clampPulseTicks(tag, KEY_PULSE_ON);
        this.pulseOffTicks = clampPulseTicks(tag, KEY_PULSE_OFF);
        this.batchSize = Math.max(MIN_BATCH_SIZE,
                Math.min(MAX_BATCH_SIZE, tag.getInteger(KEY_BATCH_SIZE)));
        this.batchCounter = 0;
        loadOutputRates(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.tankKind != null && this.tankAmount > 0) {
            NBTTagCompound tankTag = new NBTTagCompound();
            tankTag.setString("FluidName", this.tankKind.getName());
            tankTag.setInteger("Amount", this.tankAmount);
            tag.setTag(KEY_TANK, tankTag);
            tag.setString("TankKind", this.tankKind.getName());
            tag.setInteger("TankAmount", this.tankAmount);
        }
        tag.setInteger(KEY_DATA_VERSION, DATA_VERSION);
        tag.setBoolean(KEY_INFINITE, this.infinite);
        tag.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.setInteger(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        tag.setInteger(KEY_PULSE_ON, this.pulseOnTicks);
        tag.setInteger(KEY_PULSE_OFF, this.pulseOffTicks);
        tag.setInteger(KEY_BATCH_SIZE, this.batchSize);
        for (EnumFacing direction : EnumFacing.values()) {
            int index = sideIndex(direction);
            tag.setLong(SIDE_RATE_KEYS[index], this.sideRates[index]);
        }
        return tag;
    }

    private static int clampPulseTicks(NBTTagCompound tag, String key) {
        if (!tag.hasKey(key, Constants.NBT.TAG_INT)) {
            return DEFAULT_PULSE_TICKS;
        }
        return Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, tag.getInteger(key)));
    }

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse readFromNBT), so it must stay
     * side-effect free.
     */
    private void loadOutputRates(NBTTagCompound tag) {
        int dataVersion = tag.hasKey(KEY_DATA_VERSION, Constants.NBT.TAG_INT)
                ? tag.getInteger(KEY_DATA_VERSION) : 1;
        int legacyMask = (tag.hasKey(KEY_OUTPUT_SIDES, Constants.NBT.TAG_INT)
                ? tag.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        long legacyRate = readLegacyRate(tag);

        long[] loaded = new long[this.sideRates.length];
        for (EnumFacing direction : EnumFacing.values()) {
            int index = sideIndex(direction);
            loaded[index] = dataVersion >= DATA_VERSION
                    ? (tag.hasKey(SIDE_RATE_KEYS[index], Constants.NBT.TAG_LONG)
                            ? tag.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? (tag.hasKey(KEY_OUTPUT_SIDES, Constants.NBT.TAG_INT)
                        ? tag.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES)
                : legacyMask) & ALL_SIDES;
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        for (int i = 0; i < loaded.length; i++) {
            loaded[i] = Math.max(0L, Math.min(maxPerSide, loaded[i]));
        }
        clampTotalRate(loaded);
        for (int i = 0; i < loaded.length; i++) {
            this.sideRates[i] = loaded[i];
            this.lastNonZeroRates[i] = loaded[i] > 0L ? loaded[i] : DEFAULT_OUTPUT_RATE_MB;
        }
    }

    /** Reads the 2.1.x global rate, including the older preset-index format. */
    private static long readLegacyRate(NBTTagCompound tag) {
        if (tag.hasKey(KEY_LEGACY_OUTPUT_RATE_MB, Constants.NBT.TAG_LONG)) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    tag.getLong(KEY_LEGACY_OUTPUT_RATE_MB)));
        }
        if (tag.hasKey(KEY_LEGACY_OUTPUT_RATE_INDEX, Constants.NBT.TAG_INT)) {
            int[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
                    10_000_000, 100_000_000, 1_000_000_000};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    tag.getInteger(KEY_LEGACY_OUTPUT_RATE_INDEX)));
            return legacyRates[index];
        }
        return DEFAULT_OUTPUT_RATE_MB;
    }

    /** Reduces enabled sides in a fixed order when a lowered config cap is below the saved total. */
    private void clampTotalRate(long[] rates) {
        long maxTotal = InfinityWhatConfig.maxFluidTotalRateMb();
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total <= maxTotal) {
            return;
        }
        long overflow = total - maxTotal;
        for (int i = 0; i < rates.length && overflow > 0; i++) {
            if ((this.outputSideMask & (1 << i)) == 0) {
                continue;
            }
            long reduction = Math.min(rates[i], overflow);
            rates[i] -= reduction;
            overflow -= reduction;
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(
                this.pos, 1, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net,
                             net.minecraft.network.play.server.SPacketUpdateTileEntity packet) {
        // 1.13's default is a no-op, so without this the client keeps stale tank/button state.
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            this.world.notifyBlockUpdate(this.pos, this.getBlockState(), this.getBlockState(),
                    Constants.BlockFlags.DEFAULT);
        }
    }

    private boolean isDisallowed(InfFluidKind kind) {
        return kind != null && !InfinityWhatConfig.isFluidAllowedName(kind.getName());
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid handlers
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.outputStatus.tick();
    }

    /** Clears the retry backoff so the next tick attempts output again (neighbor/config change). */
    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(Container menu) {
        return menu instanceof InfFluidGeneratorMenu
                && ((InfFluidGeneratorMenu) menu).getBlockEntity() == this;
    }




}
