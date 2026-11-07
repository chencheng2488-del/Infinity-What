package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Server-side controller for Thaumcraft 4 infusion altars under an Ordo Usurpatory Matrix.
 *
 * <p>Why this exists instead of doing the work inside the matrix tile entity: the matrix can be
 * broken while an infusion is still running. Its tile entity is then gone, so a pending release
 * must be owned by a world-level controller.
 *
 * <p>Thaumcraft 4 is accessed by reflection because the integration must not load any Thaumcraft
 * classes when Thaumcraft 4 is absent. The class name and field names below are the only contract.
 *
 * <p>Verified against Thaumcraft 4.2.3.5 {@code thaumcraft.common.tiles.TileInfusionMatrix}:
 * <pre>
 *   public  boolean crafting;
 *   public  int     instability;  // 0 = perfectly stable, 25 = worst
 *   private int     countDelay;   // craftCycle() runs when count % countDelay == 0
 * </pre>
 * The constructor sets {@code countDelay = 10}; {@code craftCycle()} only rewrites it to 10 or 20
 * as part of its own phases. Writing {@code countDelay} every tick therefore changes the effective
 * period, and Thaumcraft restores its own cadence at the next cycle once the override stops.
 *
 * <p>Packet requests are queued and applied on the server thread during the world tick, because
 * the 1.7.10 network handler runs off-thread and there is no scheduled-task API on MinecraftServer.
 */
@SuppressWarnings("unchecked")
public final class ThaumcraftAltarControlManager {

    private static final String TC4_ALTAR_CLASS = "thaumcraft.common.tiles.TileInfusionMatrix";

    /** TC4 has no cycleTime; the altar starts with countDelay = 10. */
    private static final int TC4_BASE_DELAY = 10;

    private static final Map<ControlKey, ControlState> STATES =
            new HashMap<ControlKey, ControlState>();
    private static final Map<Class<?>, AltarReflection> REFLECTION_CACHE =
            new HashMap<Class<?>, AltarReflection>();
    private static final Set<String> REPORTED_PROBLEMS = new HashSet<String>();
    private static final ConcurrentLinkedQueue<SettingsRequest> REQUESTS =
            new ConcurrentLinkedQueue<SettingsRequest>();

    /**
     * 1.7.10's EventBus only registers handlers from an instance; passing a Class object registers
     * nothing at all. This instance is registered on both the FML bus (world ticks) and the Forge
     * bus (world unload).
     */
    public static final ThaumcraftAltarControlManager INSTANCE =
            new ThaumcraftAltarControlManager();

    private ThaumcraftAltarControlManager() {
    }

    /**
     * Finds the best altar for this controller and binds it. Structure-mode placement wins over
     * range-mode placement, then the nearest altar wins.
     */
    public static AltarPos findAndBind(World world, OrdoUsurpatoryMatrixTileEntity controller) {
        if (world == null || world.isRemote || controller == null || controller.isInvalid()) {
            return null;
        }

        List<AltarCandidate> candidates = new ArrayList<AltarCandidate>();
        // Copy first: tile entities may add other tile entities while the world iterates its list.
        Object[] snapshot = world.loadedTileEntityList.toArray();
        for (Object element : snapshot) {
            if (!(element instanceof TileEntity)) {
                continue;
            }
            TileEntity tile = (TileEntity) element;
            AltarReflection reflection = reflectionFor(tile.getClass());
            if (reflection == null) {
                continue;
            }
            double dx = controller.xCoord - tile.xCoord;
            double dy = controller.yCoord - tile.yCoord;
            double dz = controller.zCoord - tile.zCoord;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > (double) InfinityWhatConfig.ordoMatrixRange()
                    * (double) InfinityWhatConfig.ordoMatrixRange()) {
                continue;
            }
            boolean structureMode = isInsideAltarFootprint(controller.xCoord,
                    controller.yCoord, controller.zCoord, tile.xCoord, tile.yCoord,
                    tile.zCoord);
            candidates.add(new AltarCandidate(new AltarPos(tile.xCoord, tile.yCoord,
                    tile.zCoord), structureMode, distanceSq));
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
                                AltarPos altarPos, boolean structureMode, double distanceSq) {
        ControlKey key = new ControlKey(world.provider.dimensionId, altarPos);
        ControlState existing = STATES.get(key);
        if (existing != null && existing.controllerX == controller.xCoord
                && existing.controllerY == controller.yCoord
                && existing.controllerZ == controller.zCoord) {
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
        ControlState state = new ControlState(altarPos, controller.xCoord, controller.yCoord,
                controller.zCoord);
        state.readFrom(controller, structureMode, distanceSq);
        STATES.put(key, state);
        return true;
    }

    /** Called by the tile entity when the player changes a setting. */
    public static void updateSettings(World world, OrdoUsurpatoryMatrixTileEntity controller) {
        if (world == null || world.isRemote || controller == null || !controller.isBound()) {
            return;
        }
        AltarPos altarPos = new AltarPos(controller.getBoundAltarX(), controller.getBoundAltarY(),
                controller.getBoundAltarZ());
        ControlKey key = new ControlKey(world.provider.dimensionId, altarPos);
        ControlState state = STATES.get(key);
        if (state != null && state.controllerX == controller.xCoord
                && state.controllerY == controller.yCoord
                && state.controllerZ == controller.zCoord) {
            state.readFrom(controller, state.structureMode, state.distanceSq);
        }
    }

    /**
     * Called when the controller block is broken. It never writes default altar values; it only
     * marks the state for release. If the altar is crafting, the current settings stay in effect
     * until the infusion finishes.
     */
    public static void release(World world, int x, int y, int z) {
        if (world == null || world.isRemote) {
            return;
        }
        int dimension = world.provider.dimensionId;
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            ControlKey key = entry.getKey();
            ControlState state = entry.getValue();
            if (key.dimension != dimension || state.controllerX != x
                    || state.controllerY != y || state.controllerZ != z) {
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
    public static void unbind(World world, int x, int y, int z) {
        if (world == null || world.isRemote) {
            return;
        }
        int dimension = world.provider.dimensionId;
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            ControlKey key = entry.getKey();
            ControlState state = entry.getValue();
            if (key.dimension != dimension || state.controllerX != x
                    || state.controllerY != y || state.controllerZ != z) {
                continue;
            }
            if (!state.releaseWhenIdle) {
                iterator.remove();
            }
        }
    }

    /**
     * Queues a client edit. The 1.7.10 network handler runs off-thread, so nothing touches the
     * world here; {@link #applyRequests()} runs the edit on the server thread.
     */
    public static void requestSettings(EntityPlayerMP player, int x, int y, int z,
                                       float speedMultiplier, int stabilityMode,
                                       float stabilityValue) {
        REQUESTS.add(new SettingsRequest(player, x, y, z, speedMultiplier, stabilityMode,
                stabilityValue));
    }

    /**
     * Drops every override owned by a dimension that is going away. Without this, a single player
     * session that leaves one world and joins another would keep stale altar positions in memory
     * and could override an unrelated altar that happens to sit on the same coordinates.
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            return;
        }
        int dimension = event.world.provider.dimensionId;
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().dimension == dimension) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            applyRequests();
        }
        if (STATES.isEmpty()) {
            return;
        }

        int dimension = event.world.provider.dimensionId;
        Iterator<Map.Entry<ControlKey, ControlState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, ControlState> entry = iterator.next();
            ControlKey key = entry.getKey();
            ControlState state = entry.getValue();
            if (key.dimension != dimension) {
                continue;
            }

            if (!isAltarLoaded(event.world, state.altarPos)) {
                continue;
            }

            TileEntity altar = event.world.getTileEntity(state.altarPos.x, state.altarPos.y,
                    state.altarPos.z);
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

            // START runs before the altar's own updateEntity, so countDelay is in place when
            // Thaumcraft evaluates count % countDelay. END rewrites instability after the altar's
            // own recalculation for the tick, which is what "hold this value" requires.
            if (event.phase == TickEvent.Phase.START) {
                applySpeed(state, altar, reflection);
            }
            applyStability(state, altar, reflection);
        }
    }

    private static void applyRequests() {
        SettingsRequest request;
        while ((request = REQUESTS.poll()) != null) {
            EntityPlayerMP player = request.player;
            if (player == null || player.worldObj == null) {
                continue;
            }
            if (player.getDistanceSq(request.x + 0.5D, request.y + 0.5D,
                    request.z + 0.5D) > 64.0D) {
                continue;
            }
            TileEntity tile = player.worldObj.getTileEntity(request.x, request.y, request.z);
            if (!(tile instanceof OrdoUsurpatoryMatrixTileEntity)) {
                continue;
            }
            OrdoUsurpatoryMatrixTileEntity.StabilityMode[] modes =
                    OrdoUsurpatoryMatrixTileEntity.StabilityMode.values();
            int mode = Math.max(0, Math.min(modes.length - 1, request.stabilityMode));
            ((OrdoUsurpatoryMatrixTileEntity) tile).setSettings(request.speedMultiplier,
                    modes[mode], request.stabilityValue);
        }
    }

    private static void applySpeed(ControlState state, TileEntity altar,
                                   AltarReflection reflection) {
        if (state.speedMultiplier <= 1.0F) {
            return;
        }
        int delay = (int) Math.floor(TC4_BASE_DELAY / (double) state.speedMultiplier);
        reflection.setCountDelay(altar, Math.max(1, delay));
    }

    private static void applyStability(ControlState state, TileEntity altar,
                                       AltarReflection reflection) {
        if (state.stabilityMode == OrdoUsurpatoryMatrixTileEntity.StabilityMode.VANILLA) {
            return;
        }
        int value;
        if (state.stabilityMode == OrdoUsurpatoryMatrixTileEntity.StabilityMode.FORCE) {
            value = (int) Math.floor(state.stabilityValue);
        } else if (state.stabilityMode
                == OrdoUsurpatoryMatrixTileEntity.StabilityMode.ALWAYS_STABLE) {
            value = 0;
        } else {
            value = 25;
        }
        value = Math.max(0, Math.min(25, value));
        reflection.setInstability(altar, value);
    }

    private static boolean isCrafting(World world, AltarPos altarPos) {
        if (!isAltarLoaded(world, altarPos)) {
            return false;
        }
        TileEntity altar = world.getTileEntity(altarPos.x, altarPos.y, altarPos.z);
        AltarReflection reflection = reflectionFor(altar.getClass());
        return reflection != null && reflection.isCrafting(altar);
    }

    private static boolean isAltarLoaded(World world, AltarPos altarPos) {
        return world != null && altarPos != null
                && world.blockExists(altarPos.x, altarPos.y, altarPos.z)
                && world.getTileEntity(altarPos.x, altarPos.y, altarPos.z) != null;
    }

    private static boolean isInsideAltarFootprint(int controllerX, int controllerY,
                                                  int controllerZ, int altarX, int altarY,
                                                  int altarZ) {
        int dx = Math.abs(controllerX - altarX);
        int dz = Math.abs(controllerZ - altarZ);
        int dy = controllerY - altarY;
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

    /** Immutable altar coordinates; 1.7.10 has no BlockPos. */
    public static final class AltarPos {
        public final int x;
        public final int y;
        public final int z;

        public AltarPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AltarPos)) {
                return false;
            }
            AltarPos pos = (AltarPos) other;
            return this.x == pos.x && this.y == pos.y && this.z == pos.z;
        }

        @Override
        public int hashCode() {
            return (31 * this.x + this.y) * 31 + this.z;
        }
    }

    private static final class AltarCandidate {
        private final AltarPos altarPos;
        private final boolean structureMode;
        private final double distanceSq;

        private AltarCandidate(AltarPos altarPos, boolean structureMode, double distanceSq) {
            this.altarPos = altarPos;
            this.structureMode = structureMode;
            this.distanceSq = distanceSq;
        }
    }

    private static final class ControlKey {
        private final int dimension;
        private final AltarPos altarPos;

        private ControlKey(int dimension, AltarPos altarPos) {
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
        private final AltarPos altarPos;
        private final int controllerX;
        private final int controllerY;
        private final int controllerZ;
        private boolean structureMode;
        private double distanceSq;
        private float speedMultiplier = 1.0F;
        private OrdoUsurpatoryMatrixTileEntity.StabilityMode stabilityMode =
                OrdoUsurpatoryMatrixTileEntity.StabilityMode.VANILLA;
        private float stabilityValue;
        private boolean releaseWhenIdle;

        private ControlState(AltarPos altarPos, int controllerX, int controllerY,
                             int controllerZ) {
            this.altarPos = altarPos;
            this.controllerX = controllerX;
            this.controllerY = controllerY;
            this.controllerZ = controllerZ;
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

    private static final class SettingsRequest {
        private final EntityPlayerMP player;
        private final int x;
        private final int y;
        private final int z;
        private final float speedMultiplier;
        private final int stabilityMode;
        private final float stabilityValue;

        private SettingsRequest(EntityPlayerMP player, int x, int y, int z,
                                float speedMultiplier, int stabilityMode,
                                float stabilityValue) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
            this.speedMultiplier = speedMultiplier;
            this.stabilityMode = stabilityMode;
            this.stabilityValue = stabilityValue;
        }
    }

    /**
     * Cached reflective handles for one altar class. Every field is required: a partial altar
     * cannot be controlled correctly, so {@link #create(Class)} refuses it entirely and the
     * reason is logged once.
     */
    private static final class AltarReflection {
        private final Field countDelay;
        private final Field instability;
        private final Field crafting;

        private AltarReflection(Field countDelay, Field instability, Field crafting) {
            this.countDelay = countDelay;
            this.instability = instability;
            this.crafting = crafting;
        }

        private static AltarReflection create(Class<?> type) {
            if (!TC4_ALTAR_CLASS.equals(type.getName())) {
                return null;
            }
            try {
                return new AltarReflection(
                        findField(type, "countDelay"),
                        findField(type, "instability"),
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

        private void setCountDelay(TileEntity altar, int value) {
            try {
                this.countDelay.setInt(altar, value);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot write countDelay: " + exception.getMessage());
            }
        }

        private void setInstability(TileEntity altar, int value) {
            try {
                this.instability.setInt(altar, value);
            } catch (IllegalAccessException exception) {
                reportOnce("cannot write instability: " + exception.getMessage());
            }
        }
    }
}
