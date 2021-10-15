package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemModifierManager extends ResourceDataJson {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = LootSerialization.createFunctionSerializer().create();
    private final LootPredicateManager predicateManager;
    private final LootTableRegistry lootTables;
    private Map<MinecraftKey, LootItemFunction> functions = ImmutableMap.of();

    public ItemModifierManager(LootPredicateManager lootConditionManager, LootTableRegistry lootManager) {
        super(GSON, "item_modifiers");
        this.predicateManager = lootConditionManager;
        this.lootTables = lootManager;
    }

    @Nullable
    public LootItemFunction get(MinecraftKey id) {
        return this.functions.get(id);
    }

    public LootItemFunction get(MinecraftKey id, LootItemFunction fallback) {
        return this.functions.getOrDefault(id, fallback);
    }

    @Override
    protected void apply(Map<MinecraftKey, JsonElement> prepared, IResourceManager manager, GameProfilerFiller profiler) {
        Builder<MinecraftKey, LootItemFunction> builder = ImmutableMap.builder();
        prepared.forEach((id, json) -> {
            try {
                if (json.isJsonArray()) {
                    LootItemFunction[] lootItemFunctions = GSON.fromJson(json, LootItemFunction[].class);
                    builder.put(id, new ItemModifierManager.FunctionSequence(lootItemFunctions));
                } else {
                    LootItemFunction lootItemFunction = GSON.fromJson(json, LootItemFunction.class);
                    builder.put(id, lootItemFunction);
                }
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse item modifier {}", id, var4);
            }

        });
        Map<MinecraftKey, LootItemFunction> map = builder.build();
        LootCollector validationContext = new LootCollector(LootContextParameterSets.ALL_PARAMS, this.predicateManager::get, this.lootTables::getLootTable);
        map.forEach((id, lootItemFunction) -> {
            lootItemFunction.validate(validationContext);
        });
        validationContext.getProblems().forEach((string, string2) -> {
            LOGGER.warn("Found item modifier validation problem in {}: {}", string, string2);
        });
        this.functions = map;
    }

    public Set<MinecraftKey> getKeys() {
        return Collections.unmodifiableSet(this.functions.keySet());
    }

    static class FunctionSequence implements LootItemFunction {
        protected final LootItemFunction[] functions;
        private final BiFunction<ItemStack, LootTableInfo, ItemStack> compositeFunction;

        public FunctionSequence(LootItemFunction[] functions) {
            this.functions = functions;
            this.compositeFunction = LootItemFunctions.compose(functions);
        }

        @Override
        public ItemStack apply(ItemStack itemStack, LootTableInfo lootContext) {
            return this.compositeFunction.apply(itemStack, lootContext);
        }

        @Override
        public LootItemFunctionType getType() {
            throw new UnsupportedOperationException();
        }
    }
}
