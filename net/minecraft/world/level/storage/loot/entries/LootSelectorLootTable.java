package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootSelectorLootTable extends LootSelectorEntry {
    final MinecraftKey name;

    LootSelectorLootTable(MinecraftKey id, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.name = id;
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.REFERENCE;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        LootTable lootTable = context.getLootTable(this.name);
        lootTable.populateLootDirect(context, lootConsumer);
    }

    @Override
    public void validate(LootCollector reporter) {
        if (reporter.hasVisitedTable(this.name)) {
            reporter.reportProblem("Table " + this.name + " is recursively called");
        } else {
            super.validate(reporter);
            LootTable lootTable = reporter.resolveLootTable(this.name);
            if (lootTable == null) {
                reporter.reportProblem("Unknown loot table called " + this.name);
            } else {
                lootTable.validate(reporter.enterTable("->{" + this.name + "}", this.name));
            }

        }
    }

    public static LootSelectorEntry.Builder<?> lootTableReference(MinecraftKey id) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootSelectorLootTable(id, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootSelectorEntry.Serializer<LootSelectorLootTable> {
        @Override
        public void serializeType(JsonObject json, LootSelectorLootTable entry, JsonSerializationContext context) {
            super.serializeType(json, entry, context);
            json.addProperty("name", entry.name.toString());
        }

        @Override
        protected LootSelectorLootTable deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "name"));
            return new LootSelectorLootTable(resourceLocation, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
