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

import java.util.ArrayList;

public class DMSInfoCommand {
    private static final String name = "dmsinfo";

    private RaiixDMServer theMod;
    public DMSInfoCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSInfoCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).then(CommandManager.argument("roomID", StringArgumentType.greedyString()).executes((commandContext) -> {
                    String[] args = new String[1];
                    args[0] = StringArgumentType.getString(commandContext, "roomID");
                    execute(commandContext, commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), args);
                    return Command.SINGLE_SUCCESS;
                }))
        );
        return this;
    }

    public void execute(CommandContext<ServerCommandSource> cc, MinecraftServer server, Entity sender, String[] args)
    {
        if(args.length < 1) return;

        RaiixDMServerRoom room = theMod.theRooms.get(args[0]);
        ArrayList<Text> msg = new ArrayList<>();
        if(room != null)
        {
            msg.add(new TranslatableText(">====| 当前房间("+room.roomID+")的信息 |====<").setStyle(new Style().setColor(Formatting.WHITE)));
            msg.add(new TranslatableText("标题：").setStyle(new Style().setColor(Formatting.GREEN)).append(new TranslatableText(room.roomTitle).setStyle(new Style().setColor(Formatting.WHITE))));
            msg.add(new TranslatableText("主播：").setStyle(new Style().setColor(Formatting.GREEN)).append(new TranslatableText(room.ownerName).setStyle(new Style().setColor(Formatting.WHITE))));
            msg.add(new TranslatableText("人气值：").setStyle(new Style().setColor(Formatting.GREEN)).append(new TranslatableText(Integer.toString(room.viewerNumber)).setStyle(new Style().setColor(Formatting.WHITE))));


        }else
        {
            msg.add(new TranslatableText("未找到(或已断开)房间").setStyle(new Style().setColor(Formatting.WHITE)).append(new TranslatableText(args[0]).setStyle(new Style().setColor(Formatting.GREEN))));
        }
        for(Text t : msg) cc.getSource().sendFeedback(t, false);
    }
}
