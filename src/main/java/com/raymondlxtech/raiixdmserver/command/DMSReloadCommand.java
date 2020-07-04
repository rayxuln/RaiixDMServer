package com.raymondlxtech.raiixdmserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import com.raymondlxtech.raiixdmserver.RaiixDMServerRoom;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class DMSReloadCommand {
    private static final String name = "dmsreload";

    private RaiixDMServer theMod;
    public DMSReloadCommand(RaiixDMServer m)
    {
        theMod = m;
    }

    public String getName(){return name;}

    public DMSReloadCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).requires((source)->source.hasPermissionLevel(4)).executes((commandContext) -> {
                    execute(commandContext, commandContext.getSource().getMinecraftServer(), commandContext.getSource().getEntity(), null);
                    return Command.SINGLE_SUCCESS;
                })
        );
        return this;
    }

    public void execute(CommandContext<ServerCommandSource> cc, MinecraftServer server, Entity sender, String[] args)
    {
        theMod.theConfigHelper.loadConfig();
        cc.getSource().sendFeedback(new TranslatableText("已重新加载RaiixDMServer的配置文件").setStyle(Style.EMPTY.withColor(Formatting.WHITE)), true);
    }
}
