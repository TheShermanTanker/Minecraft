package net.minecraft.advancements.critereon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.LootSerialization;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootDeserializationContext {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftKey id;
    private final LootPredicateManager predicateManager;
    private final Gson predicateGson = LootSerialization.createConditionSerializer().create();

    public LootDeserializationContext(MinecraftKey advancementId, LootPredicateManager conditionManager) {
        this.id = advancementId;
        this.predicateManager = conditionManager;
    }

    public final LootItemCondition[] deserializeConditions(JsonArray array, String key, LootContextParameterSet contextType) {
        LootItemCondition[] lootItemConditions = this.predicateGson.fromJson(array, LootItemCondition[].class);
        LootCollector validationContext = new LootCollector(contextType, this.predicateManager::get, (resourceLocation) -> {
            return null;
        });

        for(LootItemCondition lootItemCondition : lootItemConditions) {
            lootItemCondition.validate(validationContext);
            validationContext.getProblems().forEach((string2, string3) -> {
                LOGGER.warn("Found validation problem in advancement trigger {}/{}: {}", key, string2, string3);
            });
        }

        return lootItemConditions;
    }

    public MinecraftKey getAdvancementId() {
        return this.id;
    }
}
