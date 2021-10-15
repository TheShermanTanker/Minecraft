package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.item.ArgumentItemPredicate;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class CommandClear {
    private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType((playerName) -> {
        return new ChatMessage("clear.failed.single", playerName);
    });
    private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType((playerCount) -> {
        return new ChatMessage("clear.failed.multiple", playerCount);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("clear").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return clearInventory(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException()), (stack) -> {
                return true;
            }, -1);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return clearInventory(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), (stack) -> {
                return true;
            }, -1);
        }).then(net.minecraft.commands.CommandDispatcher.argument("item", ArgumentItemPredicate.itemPredicate()).executes((context) -> {
            return clearInventory(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentItemPredicate.getItemPredicate(context, "item"), -1);
        }).then(net.minecraft.commands.CommandDispatcher.argument("maxCount", IntegerArgumentType.integer(0)).executes((context) -> {
            return clearInventory(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentItemPredicate.getItemPredicate(context, "item"), IntegerArgumentType.getInteger(context, "maxCount"));
        })))));
    }

    private static int clearInventory(CommandListenerWrapper source, Collection<EntityPlayer> targets, Predicate<ItemStack> item, int maxCount) throws CommandSyntaxException {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            i += serverPlayer.getInventory().clearOrCountMatchingItems(item, maxCount, serverPlayer.inventoryMenu.getCraftSlots());
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.slotsChanged(serverPlayer.getInventory());
        }

        if (i == 0) {
            if (targets.size() == 1) {
                throw ERROR_SINGLE.create(targets.iterator().next().getDisplayName());
            } else {
                throw ERROR_MULTIPLE.create(targets.size());
            }
        } else {
            if (maxCount == 0) {
                if (targets.size() == 1) {
                    source.sendMessage(new ChatMessage("commands.clear.test.single", i, targets.iterator().next().getScoreboardDisplayName()), true);
                } else {
                    source.sendMessage(new ChatMessage("commands.clear.test.multiple", i, targets.size()), true);
                }
            } else if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.clear.success.single", i, targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.clear.success.multiple", i, targets.size()), true);
            }

            return i;
        }
    }
}
