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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DMSGetCommand extends RaiixDMSCommand {
    private static final String name = "dmsget";

    public DMSGetCommand(RaiixDMServer m)
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
                                .suggests(new SuggestionProvider<ServerCommandSource>() {
                                    @Override
                                    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                        for(String roomID:theMod.theConfigHelper.getConfig().roomConfigs.keySet()){
                                            builder.suggest(roomID);
                                        }
                                        builder.suggest("default");
                                        return builder.buildFuture();
                                    }
                                })
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .suggests(new SuggestionProvider<ServerCommandSource>() {
                                            @Override
                                            public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                                String roomID = StringArgumentType.getString(context, "roomID");
                                                if(roomID.equals("default")){
                                                    for(String p:theMod.theConfigHelper.getConfig().getProperties()){
                                                        builder.suggest(p);
                                                    }
                                                }else{
                                                    Config roomConfig = theMod.theConfigHelper.getConfig().roomConfigs.get(roomID);
                                                    if(roomConfig != null){
                                                        for(String p:roomConfig.getProperties()){
                                                            builder.suggest(p);
                                                        }
                                                    }else{
                                                        builder.suggest("<Wrong Room ID>");
                                                    }
                                                }
                                                return builder.buildFuture();
                                            }
                                        })
                                        .executes((commandContext) -> {
                                            String[] args = new String[3];
                                            args[0] = StringArgumentType.getString(commandContext, "roomID");
                                            args[1] = StringArgumentType.getString(commandContext, "key");
                                            execute(commandContext.getSource().getEntity(), args);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                ))
        );
        return this;
    }

    @Override
    public void execute(Entity sender, String[] args)
    {
        if(args.length < 2) return;


        if(args[0].equals("default"))
        {
            String key = args[1];

            Config roomConfig = theMod.theConfigHelper.getConfig();
            String value = roomConfig.get(key);
            sendFeedback(sender,
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
                sendFeedback(sender, new TranslatableText("未找到房间" + args[0] + " 的配置信息!").setStyle(new Style().setColor(Formatting.RED)));
                return;
            }
            String value = roomConfig.get(key);
            sendFeedback(sender, "房间" + args[0] + " \"" + key +"\" 的值设为 \""+ value + "\"");
        }
    }
}
