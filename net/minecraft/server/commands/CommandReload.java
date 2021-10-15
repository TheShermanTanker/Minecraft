package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.world.level.storage.SaveData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandReload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void reloadPacks(Collection<String> dataPacks, CommandListenerWrapper source) {
        source.getServer().reloadResources(dataPacks).exceptionally((throwable) -> {
            LOGGER.warn("Failed to execute reload", throwable);
            source.sendFailureMessage(new ChatMessage("commands.reload.failure"));
            return null;
        });
    }

    private static Collection<String> discoverNewPacks(ResourcePackRepository dataPackManager, SaveData saveProperties, Collection<String> enabledDataPacks) {
        dataPackManager.reload();
        Collection<String> collection = Lists.newArrayList(enabledDataPacks);
        Collection<String> collection2 = saveProperties.getDataPackConfig().getDisabled();

        for(String string : dataPackManager.getAvailableIds()) {
            if (!collection2.contains(string) && !collection.contains(string)) {
                collection.add(string);
            }
        }

        return collection;
    }

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("reload").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            CommandListenerWrapper commandSourceStack = context.getSource();
            MinecraftServer minecraftServer = commandSourceStack.getServer();
            ResourcePackRepository packRepository = minecraftServer.getResourcePackRepository();
            SaveData worldData = minecraftServer.getSaveData();
            Collection<String> collection = packRepository.getSelectedIds();
            Collection<String> collection2 = discoverNewPacks(packRepository, worldData, collection);
            commandSourceStack.sendMessage(new ChatMessage("commands.reload.success"), true);
            reloadPacks(collection2, commandSourceStack);
            return 0;
        }));
    }
}
