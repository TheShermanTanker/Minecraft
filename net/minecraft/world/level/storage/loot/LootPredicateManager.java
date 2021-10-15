package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootPredicateManager extends ResourceDataJson {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = LootSerialization.createConditionSerializer().create();
    private Map<MinecraftKey, LootItemCondition> conditions = ImmutableMap.of();

    public LootPredicateManager() {
        super(GSON, "predicates");
    }

    @Nullable
    public LootItemCondition get(MinecraftKey id) {
        return this.conditions.get(id);
    }

    @Override
    protected void apply(Map<MinecraftKey, JsonElement> prepared, IResourceManager manager, GameProfilerFiller profiler) {
        Builder<MinecraftKey, LootItemCondition> builder = ImmutableMap.builder();
        prepared.forEach((id, json) -> {
            try {
                if (json.isJsonArray()) {
                    LootItemCondition[] lootItemConditions = GSON.fromJson(json, LootItemCondition[].class);
                    builder.put(id, new LootPredicateManager.CompositePredicate(lootItemConditions));
                } else {
                    LootItemCondition lootItemCondition = GSON.fromJson(json, LootItemCondition.class);
                    builder.put(id, lootItemCondition);
                }
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse loot table {}", id, var4);
            }

        });
        Map<MinecraftKey, LootItemCondition> map = builder.build();
        LootCollector validationContext = new LootCollector(LootContextParameterSets.ALL_PARAMS, map::get, (id) -> {
            return null;
        });
        map.forEach((id, condition) -> {
            condition.validate(validationContext.enterCondition("{" + id + "}", id));
        });
        validationContext.getProblems().forEach((string, string2) -> {
            LOGGER.warn("Found validation problem in {}: {}", string, string2);
        });
        this.conditions = map;
    }

    public Set<MinecraftKey> getKeys() {
        return Collections.unmodifiableSet(this.conditions.keySet());
    }

    static class CompositePredicate implements LootItemCondition {
        private final LootItemCondition[] terms;
        private final Predicate<LootTableInfo> composedPredicate;

        CompositePredicate(LootItemCondition[] lootItemConditions) {
            this.terms = lootItemConditions;
            this.composedPredicate = LootItemConditions.andConditions(lootItemConditions);
        }

        @Override
        public final boolean test(LootTableInfo lootContext) {
            return this.composedPredicate.test(lootContext);
        }

        @Override
        public void validate(LootCollector reporter) {
            LootItemCondition.super.validate(reporter);

            for(int i = 0; i < this.terms.length; ++i) {
                this.terms[i].validate(reporter.forChild(".term[" + i + "]"));
            }

        }

        @Override
        public LootItemConditionType getType() {
            throw new UnsupportedOperationException();
        }
    }
}
