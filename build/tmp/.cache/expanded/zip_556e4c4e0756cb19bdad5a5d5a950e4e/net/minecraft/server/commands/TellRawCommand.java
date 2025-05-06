package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class TellRawCommand {
    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("tellraw")
                .requires(p_139068_ -> p_139068_.hasPermission(2))
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("message", ComponentArgument.textComponent(pContext)).executes(p_390113_ -> {
                            int i = 0;

                            for (ServerPlayer serverplayer : EntityArgument.getPlayers(p_390113_, "targets")) {
                                serverplayer.sendSystemMessage(ComponentArgument.getResolvedComponent(p_390113_, "message", serverplayer), false);
                                i++;
                            }

                            return i;
                        }))
                )
        );
    }
}