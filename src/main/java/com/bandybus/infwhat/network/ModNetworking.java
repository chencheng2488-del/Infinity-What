package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Network channel for GUI edits. Rates may exceed a container button id range, so they travel as
 * dedicated packets; redstone mode and batch size have their own messages as well.
 */
public final class ModNetworking {

    private static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(InfinityWhat.MODID);

    private ModNetworking() {
    }

    public static void register() {
        INSTANCE.registerMessage(SetOutputRateMessage.Handler.class,
                SetOutputRateMessage.class, 0, Side.SERVER);
        INSTANCE.registerMessage(OutputRateResultMessage.Handler.class,
                OutputRateResultMessage.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(SetRedstoneMessage.Handler.class,
                SetRedstoneMessage.class, 2, Side.SERVER);
        INSTANCE.registerMessage(SetBatchMessage.Handler.class,
                SetBatchMessage.class, 3, Side.SERVER);
        INSTANCE.registerMessage(SetOrdoSettingsMessage.Handler.class,
                SetOrdoSettingsMessage.class, 4, Side.SERVER);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, long amount) {
        sendOutputRate(windowId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, int side, long amount) {
        INSTANCE.sendToServer(new SetOutputRateMessage(windowId, fluidGenerator, side, amount));
    }

    public static void sendRedstone(int windowId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        INSTANCE.sendToServer(new SetRedstoneMessage(
                windowId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
    }

    public static void sendBatch(int windowId, boolean fluidGenerator, int size) {
        INSTANCE.sendToServer(new SetBatchMessage(windowId, fluidGenerator, size));
    }

    /** Sends an Ordo Usurpatory Matrix settings edit from the client to the server. */
    public static void sendOrdoSettings(int x, int y, int z, float speedMultiplier,
                                        int stabilityMode, float stabilityValue) {
        INSTANCE.sendToServer(new SetOrdoSettingsMessage(x, y, z, speedMultiplier,
                stabilityMode, stabilityValue));
    }

    static SimpleNetworkWrapper channel() {
        return INSTANCE;
    }

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
