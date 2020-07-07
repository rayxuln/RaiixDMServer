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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DMSSetCommand extends RaiixDMSCommand {
    private static final String name = "dmsset";

    public DMSSetCommand(RaiixDMServer m)
    {
        super(m);
    }

    public List<String> getRoomConfigProperties(String roomID){
        List<String> res = new LinkedList<>();
        if(roomID.equals("default") || roomID.equals("all")){
            res.addAll(theMod.theConfigHelper.getConfig().getProperties());
        }else{
            Config roomConfig = theMod.theConfigHelper.getConfig().roomConfigs.get(roomID);
            if(roomConfig != null){
                res.addAll(roomConfig.getProperties());
            }else{
                res.addAll(theMod.theConfigHelper.getConfig().getProperties());
            }
        }
        return res;
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
                                        for(String roomID:theMod.theRooms.keySet())
                                        {
                                            builder.suggest(roomID);
                                        }
                                        builder.suggest("default");
                                        builder.suggest("all");
                                        return builder.buildFuture();
                                    }
                                })
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .suggests(new SuggestionProvider<ServerCommandSource>() {
                                            @Override
                                            public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                                String roomID = StringArgumentType.getString(context, "roomID");
                                                for(String p:getRoomConfigProperties(roomID)){
                                                    builder.suggest(p);
                                                }
                                                return builder.buildFuture();
                                            }
                                        })
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .suggests(new SuggestionProvider<ServerCommandSource>() {
                                                    @Override
                                                    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                                        String roomID = StringArgumentType.getString(context, "roomID");
                                                        String propertyName = StringArgumentType.getString(context, "key");
                                                        List<String> properties = getRoomConfigProperties(roomID);
                                                        if(properties.contains(propertyName))
                                                        {
                                                            for(String p:theMod.theConfigHelper.getConfig().getPropertyOptionValues(propertyName))
                                                            {
                                                                builder.suggest(p);
                                                            }
                                                        }else{
                                                            builder.suggest("<Custom>");
                                                        }
                                                        return builder.buildFuture();
                                                    }
                                                })
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
