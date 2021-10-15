package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.packs.repository.ResourcePackLoader;
import net.minecraft.server.packs.repository.ResourcePackRepository;

public class CommandDatapack {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PACK = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.datapack.unknown", name);
    });
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_ENABLED = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.datapack.enable.failed", name);
    });
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_DISABLED = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.datapack.disable.failed", name);
    });
    private static final SuggestionProvider<CommandListenerWrapper> SELECTED_PACKS = (context, builder) -> {
        return ICompletionProvider.suggest(context.getSource().getServer().getResourcePackRepository().getSelectedIds().stream().map(StringArgumentType::escapeIfRequired), builder);
    };
    private static final SuggestionProvider<CommandListenerWrapper> UNSELECTED_PACKS = (context, builder) -> {
        ResourcePackRepository packRepository = context.getSource().getServer().getResourcePackRepository();
        Collection<String> collection = packRepository.getSelectedIds();
        return ICompletionProvider.suggest(packRepository.getAvailableIds().stream().filter((name) -> {
            return !collection.contains(name);
        }).map(StringArgumentType::escapeIfRequired), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("datapack").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("enable").then(net.minecraft.commands.CommandDispatcher.argument("name", StringArgumentType.string()).suggests(UNSELECTED_PACKS).executes((context) -> {
            return enablePack(context.getSource(), getPack(context, "name", true), (profiles, profile) -> {
                profile.getDefaultPosition().insert(profiles, profile, (profilex) -> {
                    return profilex;
                }, false);
            });
        }).then(net.minecraft.commands.CommandDispatcher.literal("after").then(net.minecraft.commands.CommandDispatcher.argument("existing", StringArgumentType.string()).suggests(SELECTED_PACKS).executes((context) -> {
            return enablePack(context.getSource(), getPack(context, "name", true), (profiles, profile) -> {
                profiles.add(profiles.indexOf(getPack(context, "existing", false)) + 1, profile);
            });
        }))).then(net.minecraft.commands.CommandDispatcher.literal("before").then(net.minecraft.commands.CommandDispatcher.argument("existing", StringArgumentType.string()).suggests(SELECTED_PACKS).executes((context) -> {
            return enablePack(context.getSource(), getPack(context, "name", true), (profiles, profile) -> {
                profiles.add(profiles.indexOf(getPack(context, "existing", false)), profile);
            });
        }))).then(net.minecraft.commands.CommandDispatcher.literal("last").executes((context) -> {
            return enablePack(context.getSource(), getPack(context, "name", true), List::add);
        })).then(net.minecraft.commands.CommandDispatcher.literal("first").executes((context) -> {
            return enablePack(context.getSource(), getPack(context, "name", true), (profiles, profile) -> {
                profiles.add(0, profile);
            });
        })))).then(net.minecraft.commands.CommandDispatcher.literal("disable").then(net.minecraft.commands.CommandDispatcher.argument("name", StringArgumentType.string()).suggests(SELECTED_PACKS).executes((context) -> {
            return disablePack(context.getSource(), getPack(context, "name", false));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listPacks(context.getSource());
        }).then(net.minecraft.commands.CommandDispatcher.literal("available").executes((context) -> {
            return listAvailablePacks(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("enabled").executes((context) -> {
            return listEnabledPacks(context.getSource());
        }))));
    }

    private static int enablePack(CommandListenerWrapper source, ResourcePackLoader container, CommandDatapack.Inserter packAdder) throws CommandSyntaxException {
        ResourcePackRepository packRepository = source.getServer().getResourcePackRepository();
        List<ResourcePackLoader> list = Lists.newArrayList(packRepository.getSelectedPacks());
        packAdder.apply(list, container);
        source.sendMessage(new ChatMessage("commands.datapack.modify.enable", container.getChatLink(true)), true);
        CommandReload.reloadPacks(list.stream().map(ResourcePackLoader::getId).collect(Collectors.toList()), source);
        return list.size();
    }

    private static int disablePack(CommandListenerWrapper source, ResourcePackLoader container) {
        ResourcePackRepository packRepository = source.getServer().getResourcePackRepository();
        List<ResourcePackLoader> list = Lists.newArrayList(packRepository.getSelectedPacks());
        list.remove(container);
        source.sendMessage(new ChatMessage("commands.datapack.modify.disable", container.getChatLink(true)), true);
        CommandReload.reloadPacks(list.stream().map(ResourcePackLoader::getId).collect(Collectors.toList()), source);
        return list.size();
    }

    private static int listPacks(CommandListenerWrapper source) {
        return listEnabledPacks(source) + listAvailablePacks(source);
    }

    private static int listAvailablePacks(CommandListenerWrapper source) {
        ResourcePackRepository packRepository = source.getServer().getResourcePackRepository();
        packRepository.reload();
        Collection<? extends ResourcePackLoader> collection = packRepository.getSelectedPacks();
        Collection<? extends ResourcePackLoader> collection2 = packRepository.getAvailablePacks();
        List<ResourcePackLoader> list = collection2.stream().filter((profile) -> {
            return !collection.contains(profile);
        }).collect(Collectors.toList());
        if (list.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.datapack.list.available.none"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.datapack.list.available.success", list.size(), ChatComponentUtils.formatList(list, (profile) -> {
                return profile.getChatLink(false);
            })), false);
        }

        return list.size();
    }

    private static int listEnabledPacks(CommandListenerWrapper source) {
        ResourcePackRepository packRepository = source.getServer().getResourcePackRepository();
        packRepository.reload();
        Collection<? extends ResourcePackLoader> collection = packRepository.getSelectedPacks();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.datapack.list.enabled.none"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.datapack.list.enabled.success", collection.size(), ChatComponentUtils.formatList(collection, (profile) -> {
                return profile.getChatLink(true);
            })), false);
        }

        return collection.size();
    }

    private static ResourcePackLoader getPack(CommandContext<CommandListenerWrapper> context, String name, boolean enable) throws CommandSyntaxException {
        String string = StringArgumentType.getString(context, name);
        ResourcePackRepository packRepository = context.getSource().getServer().getResourcePackRepository();
        ResourcePackLoader pack = packRepository.getPack(string);
        if (pack == null) {
            throw ERROR_UNKNOWN_PACK.create(string);
        } else {
            boolean bl = packRepository.getSelectedPacks().contains(pack);
            if (enable && bl) {
                throw ERROR_PACK_ALREADY_ENABLED.create(string);
            } else if (!enable && !bl) {
                throw ERROR_PACK_ALREADY_DISABLED.create(string);
            } else {
                return pack;
            }
        }
    }

    interface Inserter {
        void apply(List<ResourcePackLoader> profiles, ResourcePackLoader profile) throws CommandSyntaxException;
    }
}
