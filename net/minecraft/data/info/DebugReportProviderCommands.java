package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;

public class DebugReportProviderCommands implements DebugReportProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private final DebugReportGenerator generator;

    public DebugReportProviderCommands(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        Path path = this.generator.getOutputFolder().resolve("reports/commands.json");
        CommandDispatcher<CommandListenerWrapper> commandDispatcher = (new net.minecraft.commands.CommandDispatcher(net.minecraft.commands.CommandDispatcher.ServerType.ALL)).getDispatcher();
        DebugReportProvider.save(GSON, cache, ArgumentRegistry.serializeNodeToJson(commandDispatcher, commandDispatcher.getRoot()), path);
    }

    @Override
    public String getName() {
        return "Command Syntax";
    }
}
