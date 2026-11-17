package com.bandybus.infwhat.integration.thaumcraft6;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import java.util.Locale;

/**
 * Server-side helper for the Thaumcraft cheat command. Kept in the integration package so the
 * command class itself never loads any Thaumcraft type when the mod is absent.
 */
public final class ThaumcraftCommandIntegration {

    private ThaumcraftCommandIntegration() {
    }

    /** Unlocks every research entry and marks every aspect as discovered for the player. */
    public static void collectEverything(MinecraftServer server, EntityPlayerMP player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) {
            return;
        }
        // TC6 stores aspect discovery as hidden research keys "!<aspect tag>".
        for (Aspect aspect : Aspect.aspects.values()) {
            knowledge.addResearch("!" + aspect.getTag().toLowerCase(Locale.ROOT));
        }
        // Reuse Thaumcraft's own "research all" implementation so requisites and flags match.
        if (server != null) {
            server.getCommandManager().executeCommand(server,
                    "thaumcraft research " + player.getName() + " all");
        }
        knowledge.sync(player);
    }
}
