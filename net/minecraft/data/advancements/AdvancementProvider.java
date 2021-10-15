package net.minecraft.data.advancements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementProvider implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DebugReportGenerator generator;
    private final List<Consumer<Consumer<Advancement>>> tabs = ImmutableList.of(new TheEndAdvancements(), new HusbandryAdvancements(), new AdventureAdvancements(), new NetherAdvancements(), new StoryAdvancements());

    public AdvancementProvider(DebugReportGenerator root) {
        this.generator = root;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        Set<MinecraftKey> set = Sets.newHashSet();
        Consumer<Advancement> consumer = (advancement) -> {
            if (!set.add(advancement.getName())) {
                throw new IllegalStateException("Duplicate advancement " + advancement.getName());
            } else {
                Path path2 = createPath(path, advancement);

                try {
                    DebugReportProvider.save(GSON, cache, advancement.deconstruct().serializeToJson(), path2);
                } catch (IOException var6) {
                    LOGGER.error("Couldn't save advancement {}", path2, var6);
                }

            }
        };

        for(Consumer<Consumer<Advancement>> consumer2 : this.tabs) {
            consumer2.accept(consumer);
        }

    }

    private static Path createPath(Path rootOutput, Advancement advancement) {
        return rootOutput.resolve("data/" + advancement.getName().getNamespace() + "/advancements/" + advancement.getName().getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Advancements";
    }
}
