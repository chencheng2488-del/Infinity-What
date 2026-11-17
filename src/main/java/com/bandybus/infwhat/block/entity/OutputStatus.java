package com.bandybus.infwhat.block.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

/**
 * Shared output pacing and status feedback for both generator block entities.
 * Retry backoff keeps full/rejecting targets from being queried every tick, and
 * action-bar messages are throttled to avoid spamming the player viewing the GUI.
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
     * Sends one throttled action-bar message to every player currently viewing this block's GUI.
     * The caller supplies the menu check because item and fluid menus are different types.
     */
    public void notifyViewers(BlockEntity blockEntity,
                              Predicate<AbstractContainerMenu> viewerMenu,
                              int outcome) {
        if (outcome == OK || outcome == NONE || this.messageCooldown > 0) {
            return;
        }
        String key = null;
        if (outcome == FULL) {
            key = "message.infwhat.status.target_full";
        } else if (outcome == REJECTED) {
            key = "message.infwhat.status.target_rejected";
        } else if (outcome == NO_SIDES) {
            key = "message.infwhat.status.no_sides";
        }
        if (key == null || !(blockEntity.getLevel() instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel) blockEntity.getLevel();
        Component message = new TranslatableComponent(key).withStyle(ChatFormatting.RED);
        boolean sent = false;
        for (ServerPlayer player : level.players()) {
            if (viewerMenu.test(player.containerMenu)) {
                player.displayClientMessage(message, true);
                sent = true;
            }
        }
        if (sent) {
            this.messageCooldown = MESSAGE_INTERVAL;
        }
    }
}
