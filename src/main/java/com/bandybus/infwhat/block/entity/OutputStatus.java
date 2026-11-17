package com.bandybus.infwhat.block.entity;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.function.Predicate;

/**
 * Shared output pacing and status feedback for both generator tile entities.
 * Retry backoff keeps full/rejecting targets from being queried every tick, and
 * chat messages are throttled to avoid spamming the player viewing the GUI.
 */
public final class OutputStatus {
    public static final int NONE = 0;
    public static final int OK = 1;
    public static final int FULL = 2;
    public static final int REJECTED = 3;
    public static final int NO_SIDES = 4;

    private static final int RETRY_INTERVAL = 20;
    private static final int MESSAGE_INTERVAL = 100;

    private int retryCooldown;
    private int messageCooldown;

    /** Advances both cooldowns; call once per server tick. */
    public void tick() {
        if (this.retryCooldown > 0) {
            this.retryCooldown--;
        }
        if (this.messageCooldown > 0) {
            this.messageCooldown--;
        }
    }

    public boolean canAttempt() {
        return this.retryCooldown <= 0;
    }

    /** Successful output resumes the normal every-tick/batch cadence; anything else backs off. */
    public void record(int outcome) {
        this.retryCooldown = outcome == OK ? 0 : RETRY_INTERVAL;
    }

    /** Clears the retry backoff so the next tick attempts output again. */
    public void wake() {
        this.retryCooldown = 0;
    }

    public static int merge(int current, int next) {
        if (current == OK || next == OK) {
            return OK;
        }
        if (current == FULL || next == FULL) {
            return FULL;
        }
        if (current == REJECTED || next == REJECTED) {
            return REJECTED;
        }
        return NONE;
    }

    /**
     * Sends one throttled chat line to every player currently viewing this block's GUI.
     * The caller supplies the container check because the fluid and item menus are different types.
     */
    public void notifyViewers(Predicate<Container> viewerMenu, int outcome) {
        if (outcome == OK || outcome == NONE || this.messageCooldown > 0) {
            return;
        }
        String key;
        switch (outcome) {
            case FULL:
                key = "message.infwhat.status.target_full";
                break;
            case REJECTED:
                key = "message.infwhat.status.target_rejected";
                break;
            case NO_SIDES:
                key = "message.infwhat.status.no_sides";
                break;
            default:
                return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        boolean sent = false;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player != null && viewerMenu.test(player.openContainer)) {
                player.sendStatusMessage(new TextComponentTranslation(key)
                        .setStyle(new Style().setColor(TextFormatting.RED)), true);
                sent = true;
            }
        }
        if (sent) {
            this.messageCooldown = MESSAGE_INTERVAL;
        }
    }
}
