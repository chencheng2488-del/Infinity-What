package com.bandybus.infwhat.integration.thaumcraft6;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side controller for Thaumcraft 6 infusion altars under an Ordo Usurpatory Matrix.
 *
 * <p>Why this exists instead of doing the work inside the matrix tile entity: the matrix can be
 * broken while an infusion is still running. Its tile entity is then gone, so a pending release
 * must be owned by a world-level controller. The controller also keeps the per-tick override in
 * one place, which makes the "stop overriding, do not write defaults" contract explicit.
 *
 * <p>Thaumcraft is accessed by reflection because the integration must not load any Thaumcraft
 * classes when Thaumcraft is absent. The class name and field names below are the only contract.
 *
 * <p>Verified against Thaumcraft 6.1.BETA26 {@code thaumcraft.common.tiles.crafting.TileInfusionMatrix}:
 * <pre>
 *   public  boolean crafting;   // true while an infusion is running
 *   private int     countDelay; // craftCycle() runs when count % countDelay == 0
 *   private int     cycleTime;  // countDelay base is cycleTime / 2 (clamped to >= 1)
 *   public  float   stability;
 *   public  int     stabilityCap;
 * </pre>
 * {@code func_73660_a()} increments {@code count}, optionally recalculates the surroundings,
 * then runs {@code craftCycle()} when crafting and {@code count % countDelay == 0}. Writing
 * {@code countDelay} every tick therefore changes the effective period; Thaumcraft recalculates
 * the field on its own when an infusion ends or the structure changes.
 */
public final class ThaumcraftAltarControlManager {

    private static final String TC6_ALTAR_CLASS =
            "thaumcraft.common.tiles.crafting.TileInfusionMatrix";

    private static final Map<ControlKey, ControlState> STATES =
            new HashMap<ControlKey, ControlState>();
    private static final Map<Class<?>, AltarReflection> REFLECTION_CACHE =
            new HashMap<Class<?>, AltarReflection>();
    private static final Set<String> REPORTED_PROBLEMS = new HashSet<String>();

    private ThaumcraftAltarControlManager() {
    }

    /**
     * Finds the best altar for this controller and binds it. Structure-mode placement wins over
     * range-mode placement, then the nearest altar wins.
     */
    public static BlockPos findAndBind(World world, OrdoUsurpatoryMatrixTileEntity controller) {
        if (world == null || world.isRemote || controller == null || controller.isInvalid()) {
            return null;
        }

        List<AltarCandidate> candidates = new ArrayList<AltarCandidate>();
        for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
            AltarReflection reflection = reflectionFor(tile.getClass());
            if (reflection == null) {
                continue;
            }
            double distanceSq = controller.getPos().distanceSq(
                    tile.getPos().getX() + 0.5D,
                    tile.getPos().getY() + 0.5D,
                    tile.getPos().getZ() + 0.5D);
            if (distanceSq > (double) InfinityWhatConfig.ordoMatrixRange()
                    * (double) InfinityWhatConfig.ordoMatrixRange()) {
                continue;
            }
            boolean structureMode = isInsideAltarFootprint(controller.getPos(), tile.getPos());
            candidates.add(new AltarCandidate(tile.getPos(), structureMode, distanceSq));
        }

        candidates.sort(new Comparator<AltarCandidate>() {
            @Override
            public int compare(AltarCandidate first, AltarCandidate second) {
                if (first.structureMode != second.structureMode) {
                    return first.structureMode ? -1 : 1;
                }
                return Double.compare(first.distanceSq, second.distanceSq);
            }
        });

        for (AltarCandidate candidate : candidates) {
            if (bind(world, controller, candidate.altarPos, candidate.structureMode,
                    candidate.distanceSq)) {
                return candidate.altarPos;
            }
        }
        return null;
    }

    /**
     * Binds or updates the control state for one altar. Returns false when another controller
     * already owns the altar with a better placement.
     */
    private static boolean bind(World world, OrdoUsurpatoryMatrixTileEntity controller,
                                BlockPos altarPos, boolean structureMode, double distanceSq) {
        ControlKey key = new ControlKey(world.provider.getDimension(), altarPos);
        ControlState existing = STATES.get(key);
        if (existing != null && existing.controllerPos.equals(controller.getPos())) {
            // A live controller tile entity is binding again. If the block had been broken and
            // the state kept alive for a running infusion, this proves the controller is back.
            existing.releaseWhenIdle = false;
            existing.readFrom(controller, structureMode, distanceSq);
            return true;
        }
        if (existing != null) {
            if (existing.releaseWhenIdle && isCrafting(world, altarPos)) {
                return false;
            }
            if (existing.structureMode && !structureMode) {
                return false;
            }
            if (existing.structureMode == structureMode
                    && existing.distanceSq <= distanceSq) {
                return false;
            }
        }
        ControlState state = new ControlState(altarPos, controller.getPos());
        state.readFrom(controller, structureMode, distanceSq);
        STATES.put(key, state);
        return true;
    }

    /** Called by the tile entity when the player changes a setting. */
    public static void updateSettings(World world, OrdoUsurpatoryMatrixTileEntity controller) {
        if (world == null || world.isRemote || controller == null) {
            return;
        }
        BlockPos altarPos = controller.getBoundAltarPos();
        if (altarPos == null) {
            return;
        }
        ControlKey key = new ControlKey(world.provider.getDimension(), altarPos);
        ControlState state = STATES.get(key);
        if (state != null && state.controllerPos.equals(controller.getPos())) {
            state.readFrom(controller, state.structureMode, state.distanceSq);
        }
    }

    /**
     * Called when the controller block is broken. It never writes default altar values; it only
     * marks the state for release. If the altar is crafting, the current settings stay in effect
     * until the infusion finishes.
     */
    public static void release(World world, BlockPos controllerPos) {
        if (world == null || world.isRemote || controllerPos == null) {
            return;
        }
        int dimension = world.provider.getDimension();
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            if (entry.getKey().dimension != dimension) {
                continue;
            }
            ControlState state = entry.getValue();
            if (!state.controllerPos.equals(controllerPos)) {
                continue;
            }
            state.releaseWhenIdle = true;
            if (!isAltarLoaded(world, state.altarPos)
                    || !isCrafting(world, state.altarPos)) {
                iterator.remove();
            }
        }
    }

    /** Called when a controller loses its altar (for example, the altar was removed). */
    public static void unbind(World world, BlockPos controllerPos) {
        if (world == null || world.isRemote || controllerPos == null) {
            return;
        }
        int dimension = world.provider.getDimension();
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            if (entry.getKey().dimension != dimension) {
                continue;
            }
            ControlState state = entry.getValue();
            if (!state.controllerPos.equals(controllerPos)) {
                continue;
            }
            if (!state.releaseWhenIdle) {
                iterator.remove();
            }
        }
    }

    /**
     * Drops every override owned by a dimension that is going away. Without this, a single player
     * session that leaves one world and joins another would keep stale altar positions in memory
     * and could override an unrelated altar that happens to sit on the same coordinates.
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            return;
        }
        int dimension = event.getWorld().provider.getDimension();
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().dimension == dimension) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || STATES.isEmpty()) {
            return;
        }

        int dimension = event.world.provider.getDimension();
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            ControlKey key = entry.getKey();
            ControlState state = entry.getValue();
            if (key.dimension != dimension) {
                continue;
            }

            if (!event.world.isBlockLoaded(state.altarPos)) {
                continue;
            }

            TileEntity altar = event.world.getTileEntity(state.altarPos);
            if (altar == null) {
                iterator.remove();
                continue;
            }

            AltarReflection reflection = reflectionFor(altar.getClass());
            if (reflection == null) {
                iterator.remove();
                continue;
            }

            boolean crafting = reflection.isCrafting(altar);
            if (state.releaseWhenIdle && !crafting) {
                iterator.remove();
                continue;
            }

            // START runs before the altar's own tile update, so countDelay is in place when
            // Thaumcraft evaluates count % countDelay. END rewrites stability after Thaumcraft's
            // own replenish/loss for the tick, which is what "hold this value" requires.
            if (event.phase == TickEvent.Phase.START) {
                applySpeed(state, altar, reflection);
            }
            applyStability(state, altar, reflection);
        }
    }

    private static void applySpeed(ControlState state, TileEntity altar,
                                   AltarReflection reflection) {
        if (state.speedMultiplier <= 1.0F) {
            return;
        }
        // Thaumcraft derives countDelay as cycleTime / 2 (integer division, clamped to >= 1).
        // Reusing that exact base keeps speed 1.0 identical to vanilla and makes the multiplier
        // relative to what the altar would do on its own.
        int baseDelay = Math.max(1, reflection.getCycleTime(altar) / 2);
        int delay = (int) Math.floor(baseDelay / (double) state.speedMultiplier);
        reflection.setCountDelay(altar, Math.max(1, delay));
    }

    private static void applyStability(ControlState state, TileEntity altar,
                                       AltarReflection reflection) {
        if (state.stabilityMode == OrdoUsurpatoryMatrixTileEntity.StabilityMode.VANILLA) {
            return;
        }
        int cap = reflection.getStabilityCap(altar);
        float value;
        if (state.stabilityMode == OrdoUsurpatoryMatrixTileEntity.StabilityMode.FORCE) {
            value = state.stabilityValue;
        } else if (state.stabilityMode
                == OrdoUsurpatoryMatrixTileEntity.StabilityMode.ALWAYS_STABLE) {
            value = cap;
        } else {
            value = -100.0F;
        }
        value = Math.max(-100.0F, Math.min((float) cap, value));
        reflection.setStability(altar, value);
    }

    private static boolean isCrafting(World world, BlockPos altarPos) {
        if (world == null || altarPos == null || !world.isBlockLoaded(altarPos)) {
            return false;
        }
        TileEntity altar = world.getTileEntity(altarPos);
        if (altar == null) {
            return false;
        }
        AltarReflection reflection = reflectionFor(altar.getClass());
        return reflection != null && reflection.isCrafting(altar);
    }

    private static boolean isAltarLoaded(World world, BlockPos altarPos) {
        return world != null && altarPos != null && world.isBlockLoaded(altarPos)
                && world.getTileEntity(altarPos) != null;
    }

    private static boolean isInsideAltarFootprint(BlockPos controllerPos, BlockPos altarPos) {
        int dx = Math.abs(controllerPos.getX() - altarPos.getX());
        int dz = Math.abs(controllerPos.getZ() - altarPos.getZ());
        int dy = controllerPos.getY() - altarPos.getY();
        return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 2;
    }

    private static AltarReflection reflectionFor(Class<?> type) {
        AltarReflection cached = REFLECTION_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        AltarReflection reflection = AltarReflection.create(type);
        if (reflection != null) {
            REFLECTION_CACHE.put(type, reflection);
        }
        return reflection;
    }

    private static void reportOnce(String problem) {
        if (REPORTED_PROBLEMS.add(problem)) {
            InfinityWhat.LOGGER.warn("Thaumcraft integration: {}", problem);
        }
    }

    private static final class AltarCandidate {
        private final BlockPos altarPos;
        private final boolean structureMode;
        private final double distanceSq;

        private AltarCandidate(BlockPos altarPos, boolean structureMode, double distanceSq) {
            this.altarPos = altarPos;
            this.structureMode = structureMode;
            this.distanceSq = distanceSq;
        }
    }

    private static final class ControlKey {
        private final int dimension;
        private final BlockPos altarPos;

        private ControlKey(int dimension, BlockPos altarPos) {
            this.dimension = dimension;
            this.altarPos = altarPos;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ControlKey)) {
                return false;
            }
            ControlKey key = (ControlKey) other;
            return this.dimension == key.dimension && this.altarPos.equals(key.altarPos);
        }

        @Override
        public int hashCode() {
            return 31 * this.dimension + this.altarPos.hashCode();
        }
    }

    private static final class ControlState {
        private final BlockPos altarPos;
        private final BlockPos controllerPos;
        private boolean structureMode;
        private double distanceSq;
        private float speedMultiplier = 1.0F;
        private OrdoUsurpatoryMatrixTileEntity.StabilityMode stabilityMode =
                OrdoUsurpatoryMatrixTileEntity.StabilityMode.VANILLA;
        private float stabilityValue;
        private boolean releaseWhenIdle;

        private ControlState(BlockPos altarPos, BlockPos controllerPos) {
            this.altarPos = altarPos;
            this.controllerPos = controllerPos;
        }

        private void readFrom(OrdoUsurpatoryMatrixTileEntity controller, boolean structureMode,
                              double distanceSq) {
            this.structureMode = structureMode;
            this.distanceSq = distanceSq;
            this.speedMultiplier = controller.getSpeedMultiplier();
            this.stabilityMode = controller.getStabilityMode();
            this.stabilityValue = controller.getStabilityValue();
        }
    }

    /**
     * Cached reflective handles for one altar class. Every field is required: a partial altar
     * cannot be controlled correctly, so {@link #create(Class)} refuses it entirely and the
     * reason is logged once.
     */
    private static final class AltarReflection {
        private final Field countDelay;
        private final Field cycleTime;
        private final Field stability;
        private final Field stabilityCap;
        private final Field crafting;

        private AltarReflection(Field countDelay, Field cycleTime, Field stability,
                                Field stabilityCap, Field crafting) {
            this.countDelay = countDelay;
            this.cycleTime = cycleTime;
            this.stability = stability;
            this.stabilityCap = stabilityCap;
            this.crafting = crafting;
        }

        private static AltarReflection create(Class<?> type) {
            if (!TC6_ALTAR_CLASS.equals(type.getName())) {
                return null;
            }
            try {
                return new AltarReflection(
                        findField(type, "countDelay"),
                        findField(type, "cycleTime"),
                        findField(type, "stability"),
                        findField(type, "stabilityCap"),
                        findField(type, "crafting"));
            } catch (NoSuchFieldException exception) {
                reportOnce("missing altar field on " + type.getName() + ": "
                        + exception.getMessage());
                return null;
            }
        }

        private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
            Class<?> current = type;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        }

        private boolean isCrafting(TileEntity altar) {
            try {
                return this.crafting.getBoolean(altar);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot read crafting: " + exception.getMessage());
                return false;
            }
        }

        private int getCycleTime(TileEntity altar) {
            try {
                return this.cycleTime.getInt(altar);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot read cycleTime: " + exception.getMessage());
                return 10;
            }
        }

        private int getStabilityCap(TileEntity altar) {
            try {
                return this.stabilityCap.getInt(altar);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot read stabilityCap: " + exception.getMessage());
                return 25;
            }
        }

        private void setCountDelay(TileEntity altar, int value) {
            try {
                this.countDelay.setInt(altar, value);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot write countDelay: " + exception.getMessage());
            }
        }

        private void setStability(TileEntity altar, float value) {
            try {
                this.stability.setFloat(altar, value);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot write stability: " + exception.getMessage());
            }
        }
    }
}
