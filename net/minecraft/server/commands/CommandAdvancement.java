package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandException;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;

public class CommandAdvancement {
    private static final SuggestionProvider<CommandListenerWrapper> SUGGEST_ADVANCEMENTS = (context, builder) -> {
        Collection<Advancement> collection = context.getSource().getServer().getAdvancementData().getAdvancements();
        return ICompletionProvider.suggestResource(collection.stream().map(Advancement::getName), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("advancement").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("grant").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("only").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.ONLY));
        }).then(net.minecraft.commands.CommandDispatcher.argument("criterion", StringArgumentType.greedyString()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement").getCriteria().keySet(), builder);
        }).executes((context) -> {
            return performCriterion(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), StringArgumentType.getString(context, "criterion"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.FROM));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("until").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.UNTIL));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("through").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.THROUGH));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("everything").executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.GRANT, context.getSource().getServer().getAdvancementData().getAdvancements());
        })))).then(net.minecraft.commands.CommandDispatcher.literal("revoke").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("only").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.ONLY));
        }).then(net.minecraft.commands.CommandDispatcher.argument("criterion", StringArgumentType.greedyString()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement").getCriteria().keySet(), builder);
        }).executes((context) -> {
            return performCriterion(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), StringArgumentType.getString(context, "criterion"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.FROM));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("until").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.UNTIL));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("through").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(ArgumentMinecraftKeyRegistered.getAdvancement(context, "advancement"), CommandAdvancement.Filter.THROUGH));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("everything").executes((context) -> {
            return perform(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), CommandAdvancement.Action.REVOKE, context.getSource().getServer().getAdvancementData().getAdvancements());
        })))));
    }

    private static int perform(CommandListenerWrapper source, Collection<EntityPlayer> targets, CommandAdvancement.Action operation, Collection<Advancement> selection) {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            i += operation.perform(serverPlayer, selection);
        }

        if (i == 0) {
            if (selection.size() == 1) {
                if (targets.size() == 1) {
                    throw new CommandException(new ChatMessage(operation.getKey() + ".one.to.one.failure", selection.iterator().next().getChatComponent(), targets.iterator().next().getScoreboardDisplayName()));
                } else {
                    throw new CommandException(new ChatMessage(operation.getKey() + ".one.to.many.failure", selection.iterator().next().getChatComponent(), targets.size()));
                }
            } else if (targets.size() == 1) {
                throw new CommandException(new ChatMessage(operation.getKey() + ".many.to.one.failure", selection.size(), targets.iterator().next().getScoreboardDisplayName()));
            } else {
                throw new CommandException(new ChatMessage(operation.getKey() + ".many.to.many.failure", selection.size(), targets.size()));
            }
        } else {
            if (selection.size() == 1) {
                if (targets.size() == 1) {
                    source.sendMessage(new ChatMessage(operation.getKey() + ".one.to.one.success", selection.iterator().next().getChatComponent(), targets.iterator().next().getScoreboardDisplayName()), true);
                } else {
                    source.sendMessage(new ChatMessage(operation.getKey() + ".one.to.many.success", selection.iterator().next().getChatComponent(), targets.size()), true);
                }
            } else if (targets.size() == 1) {
                source.sendMessage(new ChatMessage(operation.getKey() + ".many.to.one.success", selection.size(), targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage(operation.getKey() + ".many.to.many.success", selection.size(), targets.size()), true);
            }

            return i;
        }
    }

    private static int performCriterion(CommandListenerWrapper source, Collection<EntityPlayer> targets, CommandAdvancement.Action operation, Advancement advancement, String criterion) {
        int i = 0;
        if (!advancement.getCriteria().containsKey(criterion)) {
            throw new CommandException(new ChatMessage("commands.advancement.criterionNotFound", advancement.getChatComponent(), criterion));
        } else {
            for(EntityPlayer serverPlayer : targets) {
                if (operation.performCriterion(serverPlayer, advancement, criterion)) {
                    ++i;
                }
            }

            if (i == 0) {
                if (targets.size() == 1) {
                    throw new CommandException(new ChatMessage(operation.getKey() + ".criterion.to.one.failure", criterion, advancement.getChatComponent(), targets.iterator().next().getScoreboardDisplayName()));
                } else {
                    throw new CommandException(new ChatMessage(operation.getKey() + ".criterion.to.many.failure", criterion, advancement.getChatComponent(), targets.size()));
                }
            } else {
                if (targets.size() == 1) {
                    source.sendMessage(new ChatMessage(operation.getKey() + ".criterion.to.one.success", criterion, advancement.getChatComponent(), targets.iterator().next().getScoreboardDisplayName()), true);
                } else {
                    source.sendMessage(new ChatMessage(operation.getKey() + ".criterion.to.many.success", criterion, advancement.getChatComponent(), targets.size()), true);
                }

                return i;
            }
        }
    }

    private static List<Advancement> getAdvancements(Advancement advancement, CommandAdvancement.Filter selection) {
        List<Advancement> list = Lists.newArrayList();
        if (selection.parents) {
            for(Advancement advancement2 = advancement.getParent(); advancement2 != null; advancement2 = advancement2.getParent()) {
                list.add(advancement2);
            }
        }

        list.add(advancement);
        if (selection.children) {
            addChildren(advancement, list);
        }

        return list;
    }

    private static void addChildren(Advancement parent, List<Advancement> childList) {
        for(Advancement advancement : parent.getChildren()) {
            childList.add(advancement);
            addChildren(advancement, childList);
        }

    }

    static enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(EntityPlayer player, Advancement advancement) {
                AdvancementProgress advancementProgress = player.getAdvancementData().getProgress(advancement);
                if (advancementProgress.isDone()) {
                    return false;
                } else {
                    for(String string : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancementData().grantCriteria(advancement, string);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(EntityPlayer player, Advancement advancement, String criterion) {
                return player.getAdvancementData().grantCriteria(advancement, criterion);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(EntityPlayer player, Advancement advancement) {
                AdvancementProgress advancementProgress = player.getAdvancementData().getProgress(advancement);
                if (!advancementProgress.hasProgress()) {
                    return false;
                } else {
                    for(String string : advancementProgress.getAwardedCriteria()) {
                        player.getAdvancementData().revokeCritera(advancement, string);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(EntityPlayer player, Advancement advancement, String criterion) {
                return player.getAdvancementData().revokeCritera(advancement, criterion);
            }
        };

        private final String key;

        Action(String string2) {
            this.key = "commands.advancement." + string2;
        }

        public int perform(EntityPlayer player, Iterable<Advancement> advancements) {
            int i = 0;

            for(Advancement advancement : advancements) {
                if (this.perform(player, advancement)) {
                    ++i;
                }
            }

            return i;
        }

        protected abstract boolean perform(EntityPlayer player, Advancement advancement);

        protected abstract boolean performCriterion(EntityPlayer player, Advancement advancement, String criterion);

        protected String getKey() {
            return this.key;
        }
    }

    static enum Filter {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        final boolean parents;
        final boolean children;

        private Filter(boolean before, boolean after) {
            this.parents = before;
            this.children = after;
        }
    }
}
