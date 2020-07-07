package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
                CommandManager.literal(getName()).then(CommandManager.argument("roomID", StringArgumentType.greedyString()).executes((commandContext) -> {
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
