package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class DMSDisconnectCommand {
    private static final String name = "dmsdisconnect";

    private RaiixDMServer theMod;
    public DMSDisconnectCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSDisconnectCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).then(CommandManager.argument("roomID", StringArgumentType.greedyString()).executes((commandContext) -> {
                    String[] args = new String[1];
                    args[0] = StringArgumentType.getString(commandContext, "roomID");
                    execute(commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), args);
                    return Command.SINGLE_SUCCESS;
                }))
        );
        return this;
    }

    public void execute(MinecraftServer server, Entity sender, String[] args)
    {
        if(args.length < 1) return;
        try {
            theMod.disconnectDMServer(args[0]);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
