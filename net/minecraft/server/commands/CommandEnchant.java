package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEnchantment;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;

public class CommandEnchant {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((entityName) -> {
        return new ChatMessage("commands.enchant.failed.entity", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType((entityName) -> {
        return new ChatMessage("commands.enchant.failed.itemless", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType((itemName) -> {
        return new ChatMessage("commands.enchant.failed.incompatible", itemName);
    });
    private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType((level, maxLevel) -> {
        return new ChatMessage("commands.enchant.failed.level", level, maxLevel);
    });
    private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(new ChatMessage("commands.enchant.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("enchant").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("enchantment", ArgumentEnchantment.enchantment()).executes((context) -> {
            return enchant(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentEnchantment.getEnchantment(context, "enchantment"), 1);
        }).then(net.minecraft.commands.CommandDispatcher.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return enchant(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentEnchantment.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"));
        })))));
    }

    private static int enchant(CommandListenerWrapper source, Collection<? extends Entity> targets, Enchantment enchantment, int level) throws CommandSyntaxException {
        if (level > enchantment.getMaxLevel()) {
            throw ERROR_LEVEL_TOO_HIGH.create(level, enchantment.getMaxLevel());
        } else {
            int i = 0;

            for(Entity entity : targets) {
                if (entity instanceof EntityLiving) {
                    EntityLiving livingEntity = (EntityLiving)entity;
                    ItemStack itemStack = livingEntity.getItemInMainHand();
                    if (!itemStack.isEmpty()) {
                        if (enchantment.canEnchant(itemStack) && EnchantmentManager.isEnchantmentCompatible(EnchantmentManager.getEnchantments(itemStack).keySet(), enchantment)) {
                            itemStack.addEnchantment(enchantment, level);
                            ++i;
                        } else if (targets.size() == 1) {
                            throw ERROR_INCOMPATIBLE.create(itemStack.getItem().getName(itemStack).getString());
                        }
                    } else if (targets.size() == 1) {
                        throw ERROR_NO_ITEM.create(livingEntity.getDisplayName().getString());
                    }
                } else if (targets.size() == 1) {
                    throw ERROR_NOT_LIVING_ENTITY.create(entity.getDisplayName().getString());
                }
            }

            if (i == 0) {
                throw ERROR_NOTHING_HAPPENED.create();
            } else {
                if (targets.size() == 1) {
                    source.sendMessage(new ChatMessage("commands.enchant.success.single", enchantment.getFullname(level), targets.iterator().next().getScoreboardDisplayName()), true);
                } else {
                    source.sendMessage(new ChatMessage("commands.enchant.success.multiple", enchantment.getFullname(level), targets.size()), true);
                }

                return i;
            }
        }
    }
}
