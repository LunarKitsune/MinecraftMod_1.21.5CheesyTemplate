package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeCommand {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.give.failed"));
    private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.take.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("recipe")
                .requires(p_138205_ -> p_138205_.hasPermission(2))
                .then(
                    Commands.literal("give")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                        .executes(
                                            p_358619_ -> giveRecipes(
                                                p_358619_.getSource(),
                                                EntityArgument.getPlayers(p_358619_, "targets"),
                                                Collections.singleton(ResourceKeyArgument.getRecipe(p_358619_, "recipe"))
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("*")
                                        .executes(
                                            p_138217_ -> giveRecipes(
                                                p_138217_.getSource(),
                                                EntityArgument.getPlayers(p_138217_, "targets"),
                                                p_138217_.getSource().getServer().getRecipeManager().getRecipes()
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("take")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
                                        .executes(
                                            p_358618_ -> takeRecipes(
                                                p_358618_.getSource(),
                                                EntityArgument.getPlayers(p_358618_, "targets"),
                                                Collections.singleton(ResourceKeyArgument.getRecipe(p_358618_, "recipe"))
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("*")
                                        .executes(
                                            p_138203_ -> takeRecipes(
                                                p_138203_.getSource(),
                                                EntityArgument.getPlayers(p_138203_, "targets"),
                                                p_138203_.getSource().getServer().getRecipeManager().getRecipes()
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int giveRecipes(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Collection<RecipeHolder<?>> pRecipes) throws CommandSyntaxException {
        int i = 0;

        for (ServerPlayer serverplayer : pTargets) {
            i += serverplayer.awardRecipes(pRecipes);
        }

        if (i == 0) {
            throw ERROR_GIVE_FAILED.create();
        } else {
            if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable("commands.recipe.give.success.single", pRecipes.size(), pTargets.iterator().next().getDisplayName()), true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.recipe.give.success.multiple", pRecipes.size(), pTargets.size()), true);
            }

            return i;
        }
    }

    private static int takeRecipes(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Collection<RecipeHolder<?>> pRecipes) throws CommandSyntaxException {
        int i = 0;

        for (ServerPlayer serverplayer : pTargets) {
            i += serverplayer.resetRecipes(pRecipes);
        }

        if (i == 0) {
            throw ERROR_TAKE_FAILED.create();
        } else {
            if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable("commands.recipe.take.success.single", pRecipes.size(), pTargets.iterator().next().getDisplayName()), true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.recipe.take.success.multiple", pRecipes.size(), pTargets.size()), true);
            }

            return i;
        }
    }
}