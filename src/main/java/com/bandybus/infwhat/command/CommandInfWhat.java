package com.bandybus.infwhat.command;

import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftCommandIntegration;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Loader;

/**
 * The {@code /infwhat} command. Currently exposes a single cheat action that unlocks all
 * Thaumcraft 6 research and all aspect discoveries at once.
 */
public class CommandInfWhat extends CommandBase {

    private static final String THAUMCRAFT_MODID = "thaumcraft";
    private static final String ALL_COLLECT = "thaumcraftallcollect";

    @Override
    public String getName() {
        return "infwhat";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/infwhat thaumcraftallcollect [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length >= 1 && ALL_COLLECT.equalsIgnoreCase(args[0])) {
            if (!Loader.isModLoaded(THAUMCRAFT_MODID)) {
                throw new CommandException(
                        "command.infwhat.thaumcraftallcollect.not_loaded");
            }
            EntityPlayerMP target = resolveTarget(server, sender, args);
            ThaumcraftCommandIntegration.collectEverything(server, target);
            sender.sendMessage(new TextComponentTranslation(
                    "command.infwhat.thaumcraftallcollect.success", target.getName()));
            return;
        }
        throw new CommandException("command.infwhat.usage");
    }

    private static EntityPlayerMP resolveTarget(MinecraftServer server, ICommandSender sender,
                                                String[] args) throws CommandException {
        if (args.length >= 2) {
            return getPlayer(server, sender, args[1]);
        }
        if (sender instanceof EntityPlayerMP) {
            return (EntityPlayerMP) sender;
        }
        throw new CommandException("command.infwhat.thaumcraftallcollect.no_player");
    }
}
