package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.CommandDispatcher;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public abstract class RaiixDMSCommand{

    public RaiixDMSCommand(RaiixDMServer m){theMod = m;}

    protected RaiixDMServer theMod;

    public abstract String getName();

    public abstract RaiixDMSCommand registry(CommandDispatcher theDispatcher);

    public abstract void execute(Entity sender, String[] args);

    public void sendFeedback(Entity sender, Text msg)
    {
        if(sender != null)
            sender.sendSystemMessage(msg, Util.NIL_UUID);
        else if(theMod.theMinecraftServer != null)
            theMod.theMinecraftServer.getPlayerManager().broadcast(msg, MessageType.CHAT, Util.NIL_UUID);
    }
    public void sendFeedback(Entity sender, String msg)
    {
        sendFeedback(sender, new TranslatableText(msg));
    }
    public void sendFeedbackError(Entity sender, String msg){
        sendFeedback(sender, new TranslatableText(msg).setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }
}
