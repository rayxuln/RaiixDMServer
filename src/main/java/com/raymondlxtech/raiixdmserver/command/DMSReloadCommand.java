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

public class DMSReloadCommand extends RaiixDMSCommand {
    private static final String name = "dmsreload";

    public DMSReloadCommand(RaiixDMServer m)
    {
        super(m);
    }

    @Override
    public String getName(){return name;}

    @Override
    public RaiixDMSCommand registry(CommandDispatcher theDispatcher)
    {
        theDispatcher.register(
                CommandManager.literal(getName()).requires((source)->source.hasPermissionLevel(4)).executes((commandContext) -> {
                    execute(commandContext.getSource().getEntity(), null);
                    return Command.SINGLE_SUCCESS;
                })
        );
        return this;
    }

    @Override
    public void execute(Entity sender, String[] args)
    {
        theMod.theConfigHelper.loadConfig();
        sendFeedback(sender, new TranslatableText("已重新加载RaiixDMServer的配置文件").setStyle(new Style().setColor(Formatting.WHITE)));
    }
}
