package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.crafting.IRecipe;

public class CommandRecipe {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.recipe.give.failed"));
    private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.recipe.take.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("recipe").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("give").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("recipe", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.ALL_RECIPES).executes((context) -> {
            return giveRecipes(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), Collections.singleton(ArgumentMinecraftKeyRegistered.getRecipe(context, "recipe")));
        })).then(net.minecraft.commands.CommandDispatcher.literal("*").executes((context) -> {
            return giveRecipes(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), context.getSource().getServer().getCraftingManager().getRecipes());
        })))).then(net.minecraft.commands.CommandDispatcher.literal("take").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("recipe", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.ALL_RECIPES).executes((context) -> {
            return takeRecipes(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), Collections.singleton(ArgumentMinecraftKeyRegistered.getRecipe(context, "recipe")));
        })).then(net.minecraft.commands.CommandDispatcher.literal("*").executes((context) -> {
            return takeRecipes(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), context.getSource().getServer().getCraftingManager().getRecipes());
        })))));
    }

    private static int giveRecipes(CommandListenerWrapper source, Collection<EntityPlayer> targets, Collection<IRecipe<?>> recipes) throws CommandSyntaxException {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            i += serverPlayer.discoverRecipes(recipes);
        }

        if (i == 0) {
            throw ERROR_GIVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.recipe.give.success.single", recipes.size(), targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.recipe.give.success.multiple", recipes.size(), targets.size()), true);
            }

            return i;
        }
    }

    private static int takeRecipes(CommandListenerWrapper source, Collection<EntityPlayer> targets, Collection<IRecipe<?>> recipes) throws CommandSyntaxException {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            i += serverPlayer.undiscoverRecipes(recipes);
        }

        if (i == 0) {
            throw ERROR_TAKE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.recipe.take.success.single", recipes.size(), targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.recipe.take.success.multiple", recipes.size(), targets.size()), true);
            }

            return i;
        }
    }
}
