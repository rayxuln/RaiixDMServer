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
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DMSReconnectCommand extends RaiixDMSCommand {
    private static final String name = "dmsreconnect";

    public DMSReconnectCommand(RaiixDMServer m)
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
                                for (String roomID:theMod.theRooms.keySet()) {
                                    builder.suggest(roomID);
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

        if(args[0].equals("all"))
        {
            Set<Map.Entry<String, RaiixDMServerRoom>> rooms = theMod.theRooms.entrySet();
            for(Map.Entry<String, RaiixDMServerRoom> kr : rooms)
            {
                if(!kr.getKey().equals("all"))
                {
                    args[0] = kr.getKey();
                    execute(sender, args);
                }
            }
        }else
        {
            theMod.dmsDisconnectCommand.execute(sender, args);
            theMod.dmsConnectCommand.execute(sender, args);
        }
    }
}
