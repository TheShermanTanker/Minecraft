package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootSelectorDynamic extends LootSelectorEntry {
    final MinecraftKey name;

    LootSelectorDynamic(MinecraftKey name, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.name = name;
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.DYNAMIC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        context.addDynamicDrops(this.name, lootConsumer);
    }

    public static LootSelectorEntry.Builder<?> dynamicEntry(MinecraftKey name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootSelectorDynamic(name, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootSelectorEntry.Serializer<LootSelectorDynamic> {
        @Override
        public void serializeType(JsonObject json, LootSelectorDynamic entry, JsonSerializationContext context) {
            super.serializeType(json, entry, context);
            json.addProperty("name", entry.name.toString());
        }

        @Override
        protected LootSelectorDynamic deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "name"));
            return new LootSelectorDynamic(resourceLocation, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
