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
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class DMSDisconnectCommand extends RaiixDMSCommand {
    private static final String name = "dmsdisconnect";

    public DMSDisconnectCommand(RaiixDMServer m)
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
                                builder.suggest("all");
                                for (RaiixDMServerRoom room:theMod.theRooms.values()) {
                                    if(room != null && room.state == RaiixDMServerRoom.State.Connected)
                                        builder.suggest(room.roomID);
                                }
                                return builder.buildFuture();
                            }
                        })
                        .executes((commandContext) -> {
                            String[] args = new String[1];
                            args[0] = StringArgumentType.getString(commandContext, "roomID");
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
        try {
            String roomID = args[0];
            theMod.disconnectDMServer(args[0]);
            if(!roomID.equals("all"))
                theMod.theRooms.put(roomID, null);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
