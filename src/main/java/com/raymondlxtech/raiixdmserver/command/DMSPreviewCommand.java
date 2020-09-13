package com.raymondlxtech.raiixdmserver.command;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DMSPreviewCommand extends RaiixDMSCommand {
    private static final String name = "dmspreview";

    String[] previewTypes = {"chat", "gift"};

    public DMSPreviewCommand(RaiixDMServer m)
    {
        super(m);
    }

    @Override
    public String getName(){return name;}

    @Override
    public RaiixDMSCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).then(CommandManager.argument("type", StringArgumentType.string())
                        .suggests(new SuggestionProvider<ServerCommandSource>() {
                            @Override
                            public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                for(int i=0;i<previewTypes.length;++i)
                                {
                                    builder.suggest(previewTypes[i]);
                                }
                                return builder.buildFuture();
                            }
                        })
                        .then(CommandManager.argument("roomID", StringArgumentType.string())
                                .suggests(new SuggestionProvider<ServerCommandSource>() {
                                    @Override
                                    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                        // Get room config
                                        Config rc = theMod.theConfigHelper.getConfig();
                                        for(String roomID : rc.roomConfigs.keySet())
                                        {
                                            builder.suggest(roomID);
                                        }

                                        return builder.buildFuture();
                                    }
                                })
                                .executes((commandContext) -> {
                                    String[] args = new String[2];
                                    args[0] = StringArgumentType.getString(commandContext, "type");
                                    args[1] = StringArgumentType.getString(commandContext, "roomID");
                                    execute(commandContext.getSource().getEntity(), args);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .executes((commandContext) -> {
                            String[] args = new String[1];
                            args[0] = StringArgumentType.getString(commandContext, "type");
                            execute(commandContext.getSource().getEntity(), args);
                            return Command.SINGLE_SUCCESS;
                        }))
        );
        return this;
    }

    @Override
    public void execute(Entity sender, String[] args)
    {
        if(args.length < 1) return;

        Text msg = null;
        if(args[0].equals("chat"))
        {
            // Get room config
            Config rc = theMod.theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(args.length > 1){
                String roomID = args[1];
                if(rc.roomConfigs.get(roomID) != null)
                {
                    rc = rc.roomConfigs.get(roomID);
                    for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                    {
                        mapStr.put(e.getKey(), e.getValue());
                    }
                }
            }
            mapStr.put("uLevel", "233");
            mapStr.put("danmuAuthur", "小电视");
            mapStr.put("danmuMsg", "哈喽哈喽，这是一条测试弹幕～");
            mapStr.put("roomTitle", "今晚大老虎");
            mapStr.put("roomOwner", "测试者");

            // Parse styled msg
            msg = RaiixDMServer.mapStringToStyledText(rc.chat_dm_style, mapStr);
        }else if(args[0].equals("gift"))
        {
            // Get room config
            Config rc = theMod.theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(args.length > 1){
                String roomID = args[1];
                if(rc.roomConfigs.get(roomID) != null)
                {
                    rc = rc.roomConfigs.get(roomID);
                    for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                    {
                        mapStr.put(e.getKey(), e.getValue());
                    }
                }
            }
            mapStr.put("danmuAuthur", "小电视");
            mapStr.put("num", "233");
            mapStr.put("actionName", "塞");
            mapStr.put("giftName", "纯金小铜人");
            mapStr.put("roomTitle", "不错哟");
            mapStr.put("roomOwner", "测试者2");

            // Parse styled msg
            msg = RaiixDMServer.mapStringToStyledText(rc.gift_dm_style, mapStr);
        }
        if(msg != null)
        {
            sendFeedback(sender, msg);
        }
    }
}

