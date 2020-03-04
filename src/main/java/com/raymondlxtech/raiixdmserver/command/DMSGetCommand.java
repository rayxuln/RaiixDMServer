package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import com.raymondlxtech.raiixdmserver.RaiixDMServerRoom;
import com.raymondlxtech.raiixdmserver.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.Set;

public class DMSGetCommand {
    private static final String name = "dmsget";

    private RaiixDMServer theMod;
    public DMSGetCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSGetCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName())
                        .then(CommandManager.argument("roomID", StringArgumentType.string())
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .executes((commandContext) -> {
                                            String[] args = new String[3];
                                            args[0] = StringArgumentType.getString(commandContext, "roomID");
                                            args[1] = StringArgumentType.getString(commandContext, "key");
                                            execute(commandContext, commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), args);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                ))
        );
        return this;
    }

    public void execute(CommandContext<ServerCommandSource> cc, MinecraftServer server, Entity sender, String[] args)
    {
        if(args.length < 2) return;


        if(args[0].equals("default"))
        {
            String key = args[1];

            Config roomConfig = theMod.theConfigHelper.getConfig();
            String value = roomConfig.get(key);
            sendFeedBack(cc,
                    new TranslatableText("默认" + args[0] + " \"" + key +"\" 的值为 \"").setStyle(new Style().setColor(Formatting.WHITE))
                    .append(new TranslatableText(value).setStyle(new Style().setColor(Formatting.GOLD)))
                    .append(new TranslatableText("\"").setStyle(new Style().setColor(Formatting.WHITE)))
            );
        }
        else
        {
            String key = args[1];
            Config roomConfig = theMod.theConfigHelper.getConfig().roomConfigs.get(args[0]);
            if(roomConfig == null)
            {
                sendFeedBack(cc, new TranslatableText("未找到房间" + args[0] + " 的配置信息!").setStyle(new Style().setColor(Formatting.RED)));
                return;
            }
            String value = roomConfig.get(key);
            sendFeedBack(cc, "房间" + args[0] + " \"" + key +"\" 的值设为 \""+ value + "\"");
        }
    }

    public void sendFeedBack(CommandContext<ServerCommandSource> cc, Text msg)
    {
        cc.getSource().sendFeedback(msg, false);
    }
    public void sendFeedBack(CommandContext<ServerCommandSource> cc, String msg)
    {

        sendFeedBack(cc, new TranslatableText(msg));
    }
}
