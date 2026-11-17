package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Network channel for GUI edits and tile entity synchronisation.
 *
 * <p>1.6.4 has no {@code SimpleNetworkWrapper}: every message travels as a
 * {@link Packet250CustomPayload} with a one byte opcode followed by a {@link DataOutputStream}
 * payload. Rates may exceed a container button id range, so they travel as dedicated packets;
 * redstone mode and batch size have their own messages as well.
 *
 * <p>1.6.4 also lacks a generic tile-entity description packet. Each machine therefore pushes its
 * full NBT state on this channel whenever it changes, and answers an explicit sync request when a
 * player opens its GUI.
 */
public final class ModNetworking implements IPacketHandler {

    private static final String CHANNEL = "infwhat";

    private static final byte OP_SET_RATE = 0;
    private static final byte OP_RATE_RESULT = 1;
    private static final byte OP_SET_REDSTONE = 2;
    private static final byte OP_SET_BATCH = 3;
    private static final byte OP_SET_ORDO = 4;
    private static final byte OP_SYNC_TILE = 5;
    private static final byte OP_REQUEST_SYNC = 6;

    private static final double SYNC_RANGE = 64.0D;

    private static final ModNetworking INSTANCE = new ModNetworking();

    private ModNetworking() {
    }

    public static void register() {
        NetworkRegistry.instance().registerChannel(INSTANCE, CHANNEL);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, long amount) {
        sendOutputRate(windowId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, int side, long amount) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_SET_RATE);
            out.writeInt(windowId);
            out.writeBoolean(fluidGenerator);
            out.writeByte(side);
            out.writeLong(amount);
            sendToServer(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot encode an output rate packet", exception);
        }
    }

    public static void sendRedstone(int windowId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_SET_REDSTONE);
            out.writeInt(windowId);
            out.writeBoolean(fluidGenerator);
            out.writeByte(mode);
            out.writeInt(pulseOnTicks);
            out.writeInt(pulseOffTicks);
            sendToServer(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot encode a redstone packet", exception);
        }
    }

    public static void sendBatch(int windowId, boolean fluidGenerator, int size) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_SET_BATCH);
            out.writeInt(windowId);
            out.writeBoolean(fluidGenerator);
            out.writeInt(size);
            sendToServer(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot encode a batch packet", exception);
        }
    }

    /** Sends an Ordo Usurpatory Matrix settings edit from the client to the server. */
    public static void sendOrdoSettings(int x, int y, int z, float speedMultiplier,
                                        int stabilityMode, float stabilityValue) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_SET_ORDO);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
            out.writeFloat(speedMultiplier);
            out.writeInt(stabilityMode);
            out.writeFloat(stabilityValue);
            sendToServer(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot encode an ordo packet", exception);
        }
    }

    /** Pushes the machine state to every player near the block. Server side only. */
    public static void syncTile(TileEntity tile) {
        if (tile == null || tile.getWorldObj() == null || tile.getWorldObj().isRemote) {
            return;
        }
        byte[] payload = encodeTile(tile);
        if (payload != null) {
            PacketDispatcher.sendPacketToAllAround(tile.xCoord + 0.5D, tile.yCoord + 0.5D,
                    tile.zCoord + 0.5D, SYNC_RANGE, tile.getWorldObj().provider.dimensionId,
                    PacketDispatcher.getPacket(CHANNEL, payload));
        }
    }

    /** Asks the server for the current machine state; called when a GUI is opened. */
    public static void requestTileSync(TileEntity tile) {
        if (tile == null) {
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_REQUEST_SYNC);
            out.writeInt(tile.xCoord);
            out.writeInt(tile.yCoord);
            out.writeInt(tile.zCoord);
            sendToServer(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot encode a sync request", exception);
        }
    }

    private static void sendToServer(byte[] payload) {
        PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket(CHANNEL, payload));
    }

    private static void sendToPlayer(byte[] payload, EntityPlayerMP player) {
        // FML adds the Player interface to EntityPlayerMP at runtime, but the compile-time MCP
        // jar has no such patch, so the vanilla per-player send path is used directly.
        player.playerNetServerHandler.sendPacketToPlayer(
                PacketDispatcher.getPacket(CHANNEL, payload));
    }

    private static byte[] encodeTile(TileEntity tile) {
        try {
            NBTTagCompound compound = new NBTTagCompound();
            tile.writeToNBT(compound);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_SYNC_TILE);
            out.writeInt(tile.xCoord);
            out.writeInt(tile.yCoord);
            out.writeInt(tile.zCoord);
            net.minecraft.nbt.CompressedStreamTools.write(compound, out);
            return bytes.toByteArray();
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet,
                             Player player) {
        if (!CHANNEL.equals(packet.channel) || packet.data == null) {
            return;
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.data));
        try {
            byte opcode = in.readByte();
            if (opcode == OP_RATE_RESULT) {
                handleRateResult(in);
                return;
            }
            if (opcode == OP_SYNC_TILE) {
                handleTileSync(in);
                return;
            }
            if (!(player instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP sender = (EntityPlayerMP) player;
            switch (opcode) {
                case OP_SET_RATE:
                    handleSetRate(in, sender);
                    break;
                case OP_SET_REDSTONE:
                    handleSetRedstone(in, sender);
                    break;
                case OP_SET_BATCH:
                    handleSetBatch(in, sender);
                    break;
                case OP_SET_ORDO:
                    handleSetOrdo(in, sender);
                    break;
                case OP_REQUEST_SYNC:
                    handleRequestSync(in, sender);
                    break;
                default:
                    break;
            }
        } catch (IOException exception) {
            // A malformed packet is dropped; the client never gets a partial state.
        }
    }

    // ------------------------------------------------------------------
    // Server side
    // ------------------------------------------------------------------

    private void handleSetRate(DataInputStream in, EntityPlayerMP player) throws IOException {
        int windowId = in.readInt();
        boolean fluidGenerator = in.readBoolean();
        int side = in.readByte();
        long amount = in.readLong();

        boolean success = false;
        long acceptedAmount = 0L;
        RateRejectReason reason = RateRejectReason.REJECTED;
        boolean validSide = side >= -1 && side < ForgeDirection.VALID_DIRECTIONS.length;

        if (validSide && validMenu(player, windowId)) {
            Container menu = player.openContainer;
            if (fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
                InfFluidGeneratorTileEntity tile =
                        ((InfFluidGeneratorContainer) menu).getTileEntity();
                reason = fluidRejectReason(tile, side, amount);
                if (reason == RateRejectReason.NONE) {
                    if (side < 0) {
                        success = tile.setUniformSideRateMb(amount);
                    } else {
                        success = tile.setSideRateMb(
                                ForgeDirection.VALID_DIRECTIONS[side], amount);
                    }
                    if (success) {
                        acceptedAmount = side < 0
                                ? tile.getUniformSideRateMb()
                                : tile.getSideRateMb(ForgeDirection.VALID_DIRECTIONS[side]);
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            } else if (!fluidGenerator && menu instanceof InfItemGeneratorContainer) {
                InfItemGeneratorTileEntity tile =
                        ((InfItemGeneratorContainer) menu).getTileEntity();
                reason = itemRejectReason(tile, side, amount);
                if (reason == RateRejectReason.NONE) {
                    if (side < 0) {
                        success = tile.setUniformSideRate((int) amount);
                    } else {
                        success = tile.setSideRate(ForgeDirection.VALID_DIRECTIONS[side],
                                (int) amount);
                    }
                    if (success) {
                        acceptedAmount = side < 0
                                ? tile.getUniformSideRate()
                                : tile.getSideRate(ForgeDirection.VALID_DIRECTIONS[side]);
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            }
        }
        sendRateResult(player, windowId, fluidGenerator, side, success, acceptedAmount, reason);
    }

    private void handleSetRedstone(DataInputStream in, EntityPlayerMP player) throws IOException {
        int windowId = in.readInt();
        boolean fluidGenerator = in.readBoolean();
        int mode = in.readByte();
        int pulseOnTicks = in.readInt();
        int pulseOffTicks = in.readInt();
        if (!validMenu(player, windowId)) {
            return;
        }
        applyRedstone(player.openContainer, fluidGenerator, mode, pulseOnTicks, pulseOffTicks);
    }

    private void handleSetBatch(DataInputStream in, EntityPlayerMP player) throws IOException {
        int windowId = in.readInt();
        boolean fluidGenerator = in.readBoolean();
        int size = in.readInt();
        if (!validMenu(player, windowId)) {
            return;
        }
        applyBatch(player.openContainer, fluidGenerator, size);
    }

    private void handleSetOrdo(DataInputStream in, EntityPlayerMP player) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        float speedMultiplier = in.readFloat();
        int stabilityMode = in.readInt();
        float stabilityValue = in.readFloat();
        if (ThaumcraftIntegration.isActive()) {
            // The edit is queued and applied on the world tick, so the network thread never
            // touches a tile entity.
            ThaumcraftAltarControlManager.requestSettings(player, x, y, z, speedMultiplier,
                    stabilityMode, stabilityValue);
        }
    }

    private void handleRequestSync(DataInputStream in, EntityPlayerMP player) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        if (player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > SYNC_RANGE * SYNC_RANGE) {
            return;
        }
        TileEntity tile = player.worldObj.getBlockTileEntity(x, y, z);
        if (tile == null) {
            return;
        }
        byte[] payload = encodeTile(tile);
        if (payload != null) {
            sendToPlayer(payload, player);
        }
    }

    // ------------------------------------------------------------------
    // Client side
    // ------------------------------------------------------------------

    private void handleRateResult(DataInputStream in) throws IOException {
        int windowId = in.readInt();
        boolean fluidGenerator = in.readBoolean();
        int side = in.readByte();
        boolean success = in.readBoolean();
        long amount = in.readLong();
        int reason = in.readByte();
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            com.bandybus.infwhat.client.network.ClientPacketHandler.handleRateResult(windowId,
                    fluidGenerator, side, success, amount, RateRejectReason.byId(reason));
        }
    }

    private void handleTileSync(DataInputStream in) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        NBTTagCompound compound = net.minecraft.nbt.CompressedStreamTools.read(in);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            com.bandybus.infwhat.client.network.ClientPacketHandler.applyTileSync(x, y, z,
                    compound);
        }
    }

    private void sendRateResult(EntityPlayerMP player, int windowId, boolean fluidGenerator,
                                int side, boolean success, long amount,
                                RateRejectReason reason) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(OP_RATE_RESULT);
            out.writeInt(windowId);
            out.writeBoolean(fluidGenerator);
            out.writeByte(side);
            out.writeBoolean(success);
            out.writeLong(amount);
            out.writeByte(reason.ordinal());
            sendToPlayer(bytes.toByteArray(), player);
        } catch (IOException exception) {
            // The UI simply keeps its previous value when the acknowledgement cannot be sent.
        }
    }

    // ------------------------------------------------------------------
    // Shared validation
    // ------------------------------------------------------------------

    static RateRejectReason fluidRejectReason(InfFluidGeneratorTileEntity tile, int side, long amount) {
        if (amount < 0L || amount > InfFluidGeneratorTileEntity.MAX_OUTPUT_RATE_MB) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxFluidRatePerSideMb()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledFluidSideCount(tile);
        } else {
            ForgeDirection direction = ForgeDirection.VALID_DIRECTIONS[side];
            newTotal = tile.getTotalEffectiveRateMb();
            if (tile.isOutputSideEnabled(direction)) {
                newTotal = newTotal - tile.getEffectiveSideRateMb(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxFluidTotalRateMb()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    static int enabledFluidSideCount(InfFluidGeneratorTileEntity tile) {
        int count = 0;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (tile.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }

    static RateRejectReason itemRejectReason(InfItemGeneratorTileEntity tile, int side, long amount) {
        if (amount < 0L || amount > Integer.MAX_VALUE) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxItemRatePerSide()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledItemSideCount(tile);
        } else {
            ForgeDirection direction = ForgeDirection.VALID_DIRECTIONS[side];
            newTotal = tile.getTotalEffectiveRate();
            if (tile.isOutputSideEnabled(direction)) {
                newTotal = newTotal - tile.getEffectiveSideRate(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxItemTotalRate()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    static int enabledItemSideCount(InfItemGeneratorTileEntity tile) {
        int count = 0;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (tile.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }

    static boolean validMenu(EntityPlayerMP player, int windowId) {
        Container menu = player.openContainer;
        return menu != null && windowId == menu.windowId && menu.canInteractWith(player);
    }

    static void applyRedstone(Container menu, boolean fluidGenerator, int mode,
                              int pulseOnTicks, int pulseOffTicks) {
        if (mode < 0 || mode >= RedstoneMode.values().length) {
            return;
        }
        RedstoneMode redstoneMode = RedstoneMode.byId(mode);
        if (fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
            InfFluidGeneratorTileEntity tile =
                    ((InfFluidGeneratorContainer) menu).getTileEntity();
            if (tile.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                tile.setRedstoneMode(redstoneMode);
            }
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorContainer) {
            InfItemGeneratorTileEntity tile =
                    ((InfItemGeneratorContainer) menu).getTileEntity();
            if (tile.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                tile.setRedstoneMode(redstoneMode);
            }
        }
    }

    static void applyBatch(Container menu, boolean fluidGenerator, int size) {
        if (fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
            ((InfFluidGeneratorContainer) menu).getTileEntity().setBatchSize(size);
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorContainer) {
            ((InfItemGeneratorContainer) menu).getTileEntity().setBatchSize(size);
        }
    }
}
