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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class DMSSetCommand extends RaiixDMSCommand {
    private static final String name = "dmsset";

    public DMSSetCommand(RaiixDMServer m)
    {
        super(m);
    }

    @Override
    public String getName(){return name;}

    @Override
    public RaiixDMSCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName())
                        .then(CommandManager.argument("roomID", StringArgumentType.string())
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .executes((commandContext) -> {
                                                    String[] args = new String[3];
                                                    args[0] = StringArgumentType.getString(commandContext, "roomID");
                                                    args[1] = StringArgumentType.getString(commandContext, "key");
                                                    args[2] = StringArgumentType.getString(commandContext, "value");
                                                    execute(commandContext.getSource().getEntity(), args);
                                                    return Command.SINGLE_SUCCESS;
                                                }))))

        );
        return this;
    }

    @Override
    public void execute(Entity sender, String[] args)
    {
        if(args.length < 3) return;

        if(args[0].equals("all"))
        {
            Set<Map.Entry<String, RaiixDMServerRoom>> kr = theMod.theRooms.entrySet();
            for(Map.Entry<String, RaiixDMServerRoom> r : kr)
            {
                args[0] = r.getKey();
                execute(sender, args);
            }
            args[0] = "default";
            execute(sender, args);
        }else if(args[0].equals("default"))
        {
            String key = args[1];
            String value = args[2];

            Config roomConfig = theMod.theConfigHelper.getConfig();
            roomConfig.set(key, value);
            sendFeedback(sender, "已将默认" + args[0] + " \"" + args[1] +"\" 的值设为 \""+ args[2] + "\"");
        }
        else
        {
            String key = args[1];
            String value = args[2];
            Config roomConfig = theMod.theConfigHelper.getConfig().roomConfigs.get(args[0]);
            if(roomConfig == null)
            {
                roomConfig = new Config();
                roomConfig.copyFrom(theMod.theConfigHelper.getConfig(), false);
                theMod.theConfigHelper.getConfig().roomConfigs.put(args[0], roomConfig);
            }
            roomConfig.set(key, value);
            sendFeedback(sender, "已将房间" + args[0] + " \"" + args[1] +"\" 的值设为 \""+ args[2] + "\"");
        }

        theMod.theConfigHelper.saveConfig();
    }
}
