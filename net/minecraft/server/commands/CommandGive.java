package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.item.ArgumentItemStack;
import net.minecraft.commands.arguments.item.ArgumentPredicateItemStack;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class CommandGive {
    public static final int MAX_ALLOWED_ITEMSTACKS = 100;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("give").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("item", ArgumentItemStack.item()).executes((context) -> {
            return giveItem(context.getSource(), ArgumentItemStack.getItem(context, "item"), ArgumentEntity.getPlayers(context, "targets"), 1);
        }).then(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(1)).executes((context) -> {
            return giveItem(context.getSource(), ArgumentItemStack.getItem(context, "item"), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "count"));
        })))));
    }

    private static int giveItem(CommandListenerWrapper source, ArgumentPredicateItemStack item, Collection<EntityPlayer> targets, int count) throws CommandSyntaxException {
        int i = item.getItem().getMaxStackSize();
        int j = i * 100;
        if (count > j) {
            source.sendFailureMessage(new ChatMessage("commands.give.failed.toomanyitems", j, item.createItemStack(count, false).getDisplayName()));
            return 0;
        } else {
            for(EntityPlayer serverPlayer : targets) {
                int k = count;

                while(k > 0) {
                    int l = Math.min(i, k);
                    k -= l;
                    ItemStack itemStack = item.createItemStack(l, false);
                    boolean bl = serverPlayer.getInventory().pickup(itemStack);
                    if (bl && itemStack.isEmpty()) {
                        itemStack.setCount(1);
                        EntityItem itemEntity2 = serverPlayer.drop(itemStack, false);
                        if (itemEntity2 != null) {
                            itemEntity2.makeFakeItem();
                        }

                        serverPlayer.level.playSound((EntityHuman)null, serverPlayer.locX(), serverPlayer.locY(), serverPlayer.locZ(), SoundEffects.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                        serverPlayer.containerMenu.broadcastChanges();
                    } else {
                        EntityItem itemEntity = serverPlayer.drop(itemStack, false);
                        if (itemEntity != null) {
                            itemEntity.setNoPickUpDelay();
                            itemEntity.setOwner(serverPlayer.getUniqueID());
                        }
                    }
                }
            }

            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.give.success.single", count, item.createItemStack(count, false).getDisplayName(), targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.give.success.single", count, item.createItemStack(count, false).getDisplayName(), targets.size()), true);
            }

            return targets.size();
        }
    }
}
