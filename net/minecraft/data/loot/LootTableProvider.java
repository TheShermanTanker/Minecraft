package net.minecraft.data.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableRegistry;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootTableProvider implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private final DebugReportGenerator generator;
    private final List<Pair<Supplier<Consumer<BiConsumer<MinecraftKey, LootTable.Builder>>>, LootContextParameterSet>> subProviders = ImmutableList.of(Pair.of(FishingLoot::new, LootContextParameterSets.FISHING), Pair.of(ChestLoot::new, LootContextParameterSets.CHEST), Pair.of(EntityLoot::new, LootContextParameterSets.ENTITY), Pair.of(BlockLoot::new, LootContextParameterSets.BLOCK), Pair.of(PiglinBarterLoot::new, LootContextParameterSets.PIGLIN_BARTER), Pair.of(GiftLoot::new, LootContextParameterSets.GIFT));

    public LootTableProvider(DebugReportGenerator root) {
        this.generator = root;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        Map<MinecraftKey, LootTable> map = Maps.newHashMap();
        this.subProviders.forEach((pair) -> {
            pair.getFirst().get().accept((resourceLocation, builder) -> {
                if (map.put(resourceLocation, builder.setParamSet(pair.getSecond()).build()) != null) {
                    throw new IllegalStateException("Duplicate loot table " + resourceLocation);
                }
            });
        });
        LootCollector validationContext = new LootCollector(LootContextParameterSets.ALL_PARAMS, (resourceLocationx) -> {
            return null;
        }, map::get);

        for(MinecraftKey resourceLocation : Sets.difference(LootTables.all(), map.keySet())) {
            validationContext.reportProblem("Missing built-in table: " + resourceLocation);
        }

        map.forEach((resourceLocationx, lootTable) -> {
            LootTableRegistry.validate(validationContext, resourceLocationx, lootTable);
        });
        Multimap<String, String> multimap = validationContext.getProblems();
        if (!multimap.isEmpty()) {
            multimap.forEach((string, string2) -> {
                LOGGER.warn("Found validation problem in {}: {}", string, string2);
            });
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            map.forEach((resourceLocationx, lootTable) -> {
                Path path2 = createPath(path, resourceLocationx);

                try {
                    DebugReportProvider.save(GSON, cache, LootTableRegistry.serialize(lootTable), path2);
                } catch (IOException var6) {
                    LOGGER.error("Couldn't save loot table {}", path2, var6);
                }

            });
        }
    }

    private static Path createPath(Path rootOutput, MinecraftKey lootTableId) {
        return rootOutput.resolve("data/" + lootTableId.getNamespace() + "/loot_tables/" + lootTableId.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "LootTables";
    }
}
