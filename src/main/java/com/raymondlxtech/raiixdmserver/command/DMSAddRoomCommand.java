package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import com.raymondlxtech.raiixdmserver.RaiixDMServerRoom;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;

public class DMSAddRoomCommand extends RaiixDMSCommand {
    private static final String name = "dmsaddroom";

    public DMSAddRoomCommand(RaiixDMServer m)
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

        String roomID = args[0];
        if(theMod.theRooms.get(roomID) != null){
            sendFeedbackError(sender, "房间"+roomID+"已经存在！");
            return;
        }

        RaiixDMServerRoom room = new RaiixDMServerRoom();
        room.roomID = roomID;
        theMod.theRooms.put(roomID, room);

        theMod.saveAddedRooms();
        sendFeedback(sender, "添加房间" + roomID + "成功！");
    }
}
