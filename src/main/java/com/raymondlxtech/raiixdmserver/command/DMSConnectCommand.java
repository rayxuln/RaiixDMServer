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
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class DMSConnectCommand extends RaiixDMSCommand {
    private static final String name = "dmsconnect";

    public DMSConnectCommand(RaiixDMServer m)
    {
        super(m);
    }

    @Override
    public String getName(){return name;}

    @Override
    public RaiixDMSCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).then(CommandManager.argument("roomID", StringArgumentType.greedyString())
                        .suggests(new SuggestionProvider<ServerCommandSource>() {
                            @Override
                            public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                                for (RaiixDMServerRoom room:theMod.theRooms.values()) {
                                    if(room.state != RaiixDMServerRoom.State.Connected)
                                        builder.suggest(room.roomID);
                                }
                                return builder.buildFuture();
                            }
                        })
                        .executes((commandContext) -> {
                            String[] args = new String[1];
                            args[0] = StringArgumentType.getString(commandContext, "roomID");
                            try {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        execute(commandContext.getSource().getEntity(), args);
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

    @Override
    public void execute(Entity sender, String[] args)
    {
        if(args.length < 1) return;
        String error_msg = theMod.connectBiliBiliDMServer(args[0], sender);
        if(!error_msg.isEmpty())
        {
//            if(sender == null)
//            {
//                theMod.theLogger.error(error_msg);
//            }else
//            {
//                sender.sendMessage(new TranslatableText(error_msg).setStyle(new Style().setColor(Formatting.RED)));
//            }
            sendFeedback(sender, new TranslatableText(error_msg).setStyle(new Style().setColor(Formatting.RED)));
        }
    }
}
