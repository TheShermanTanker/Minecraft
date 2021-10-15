package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootTableRegistry extends ResourceDataJson {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = LootSerialization.createLootTableSerializer().create();
    private Map<MinecraftKey, LootTable> tables = ImmutableMap.of();
    private final LootPredicateManager predicateManager;

    public LootTableRegistry(LootPredicateManager conditionManager) {
        super(GSON, "loot_tables");
        this.predicateManager = conditionManager;
    }

    public LootTable getLootTable(MinecraftKey id) {
        return this.tables.getOrDefault(id, LootTable.EMPTY);
    }

    @Override
    protected void apply(Map<MinecraftKey, JsonElement> prepared, IResourceManager manager, GameProfilerFiller profiler) {
        Builder<MinecraftKey, LootTable> builder = ImmutableMap.builder();
        JsonElement jsonElement = prepared.remove(LootTables.EMPTY);
        if (jsonElement != null) {
            LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", (Object)LootTables.EMPTY);
        }

        prepared.forEach((id, json) -> {
            try {
                LootTable lootTable = GSON.fromJson(json, LootTable.class);
                builder.put(id, lootTable);
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse loot table {}", id, var4);
            }

        });
        builder.put(LootTables.EMPTY, LootTable.EMPTY);
        ImmutableMap<MinecraftKey, LootTable> immutableMap = builder.build();
        LootCollector validationContext = new LootCollector(LootContextParameterSets.ALL_PARAMS, this.predicateManager::get, immutableMap::get);
        immutableMap.forEach((id, lootTable) -> {
            validate(validationContext, id, lootTable);
        });
        validationContext.getProblems().forEach((key, value) -> {
            LOGGER.warn("Found validation problem in {}: {}", key, value);
        });
        this.tables = immutableMap;
    }

    public static void validate(LootCollector reporter, MinecraftKey id, LootTable table) {
        table.validate(reporter.setParams(table.getLootContextParameterSet()).enterTable("{" + id + "}", id));
    }

    public static JsonElement serialize(LootTable table) {
        return GSON.toJsonTree(table);
    }

    public Set<MinecraftKey> getIds() {
        return this.tables.keySet();
    }
}
