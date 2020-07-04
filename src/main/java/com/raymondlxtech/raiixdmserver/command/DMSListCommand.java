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

import java.util.*;

public class DMSListCommand {
    private static final String name = "dmslist";

    private RaiixDMServer theMod;
    public DMSListCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSListCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).executes((commandContext) -> {
                    execute(commandContext, commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), null);
                    return Command.SINGLE_SUCCESS;
                })
        );
        return this;
    }

    public void execute(CommandContext<ServerCommandSource> cc, MinecraftServer server, Entity sender, String[] args)
    {
        Set<Map.Entry<String, RaiixDMServerRoom>> rooms = theMod.theRooms.entrySet();
        ArrayList<Text> msg = new ArrayList<>();
        msg.add(new TranslatableText(">====| 列出房间 |====<"));
        msg.add(
                new TranslatableText("当前已添加的房间(").setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                        .append(new TranslatableText(Integer.toString(rooms.size())).setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                        .append(new TranslatableText("): ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
        );
        for(Map.Entry<String, RaiixDMServerRoom> kr : rooms)
        {
            RaiixDMServerRoom r = kr.getValue();
            if(r == null)
            {
                msg.add(
                        new TranslatableText("[").setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                                .append(new TranslatableText(kr.getKey()).setStyle(Style.EMPTY.withColor(Formatting.RED)))
                                .append(new TranslatableText("] 已断开！").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                );
            }else
            {
                msg.add(
                        new TranslatableText("[").setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                                .append(new TranslatableText(r.roomID).setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                                .append(new TranslatableText("]" + r.roomTitle + " - " + r.ownerName).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                );
            }
        }

        for(Text t : msg) cc.getSource().sendFeedback(t, false);
    }
}
