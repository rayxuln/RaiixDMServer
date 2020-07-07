package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import com.raymondlxtech.raiixdmserver.RaiixDMServerRoom;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;

public class DMSRemoveRoomCommand extends RaiixDMSCommand {
    private static final String name = "dmsremoveroom";

    public DMSRemoveRoomCommand(RaiixDMServer m)
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
            theMod.dmsDisconnectCommand.execute(sender, args);
            theMod.theRooms.clear();

            theMod.saveAddedRooms();
            return;
        }

        String roomID = args[0];
        if(!theMod.theRooms.containsKey(roomID)){
            sendFeedbackError(sender, "未找到房间" + roomID + "!");
            return;
        }

        theMod.dmsDisconnectCommand.execute(sender, args);
        theMod.theRooms.remove(roomID);

        theMod.saveAddedRooms();
        sendFeedback(sender, "删除房间"+roomID+"成功！");
    }
}
