package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;

public class CommandTag {
    private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.tag.add.failed"));
    private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.tag.remove.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("tag").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("name", StringArgumentType.word()).executes((context) -> {
            return addTag(context.getSource(), ArgumentEntity.getEntities(context, "targets"), StringArgumentType.getString(context, "name"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("name", StringArgumentType.word()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(getTags(ArgumentEntity.getEntities(context, "targets")), builder);
        }).executes((context) -> {
            return removeTag(context.getSource(), ArgumentEntity.getEntities(context, "targets"), StringArgumentType.getString(context, "name"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listTags(context.getSource(), ArgumentEntity.getEntities(context, "targets"));
        }))));
    }

    private static Collection<String> getTags(Collection<? extends Entity> entities) {
        Set<String> set = Sets.newHashSet();

        for(Entity entity : entities) {
            set.addAll(entity.getScoreboardTags());
        }

        return set;
    }

    private static int addTag(CommandListenerWrapper source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity.addScoreboardTag(tag)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_ADD_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.tag.add.success.single", tag, targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.tag.add.success.multiple", tag, targets.size()), true);
            }

            return i;
        }
    }

    private static int removeTag(CommandListenerWrapper source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity.removeScoreboardTag(tag)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_REMOVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.tag.remove.success.single", tag, targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.tag.remove.success.multiple", tag, targets.size()), true);
            }

            return i;
        }
    }

    private static int listTags(CommandListenerWrapper source, Collection<? extends Entity> targets) {
        Set<String> set = Sets.newHashSet();

        for(Entity entity : targets) {
            set.addAll(entity.getScoreboardTags());
        }

        if (targets.size() == 1) {
            Entity entity2 = targets.iterator().next();
            if (set.isEmpty()) {
                source.sendMessage(new ChatMessage("commands.tag.list.single.empty", entity2.getScoreboardDisplayName()), false);
            } else {
                source.sendMessage(new ChatMessage("commands.tag.list.single.success", entity2.getScoreboardDisplayName(), set.size(), ChatComponentUtils.formatList(set)), false);
            }
        } else if (set.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.tag.list.multiple.empty", targets.size()), false);
        } else {
            source.sendMessage(new ChatMessage("commands.tag.list.multiple.success", targets.size(), set.size(), ChatComponentUtils.formatList(set)), false);
        }

        return set.size();
    }
}
