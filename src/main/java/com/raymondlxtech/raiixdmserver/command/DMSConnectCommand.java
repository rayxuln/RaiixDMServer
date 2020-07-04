package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class DMSConnectCommand {
    private static final String name = "dmsconnect";

    private RaiixDMServer theMod;
    public DMSConnectCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSConnectCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).then(CommandManager.argument("roomID", StringArgumentType.greedyString()).executes((commandContext) -> {
                    String[] args = new String[1];
                    args[0] = StringArgumentType.getString(commandContext, "roomID");
                    try {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                execute(commandContext, commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), args);
                            }
                        }).start();
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return Command.SINGLE_SUCCESS;
                }))
        );
        return this;
    }

    public void execute(CommandContext<ServerCommandSource> cc, MinecraftServer server, Entity sender, String[] args)
    {
        if(args.length < 1) return;
        String error_msg = theMod.connectBiliBiliDMServer(args[0], sender, server);
        if(!error_msg.isEmpty())
        {
//            if(sender == null)
//            {
//                theMod.theLogger.error(error_msg);
//            }else
//            {
//                sender.sendMessage(new TranslatableText(error_msg).setStyle(new Style().setColor(Formatting.RED)));
//            }
            cc.getSource().sendFeedback(new TranslatableText(error_msg).setStyle(Style.EMPTY.withColor(Formatting.RED)), true);
        }
    }
}
