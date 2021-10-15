package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootSelectorEmpty extends LootSelectorEntry {
    LootSelectorEmpty(int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.EMPTY;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
    }

    public static LootSelectorEntry.Builder<?> emptyItem() {
        return simpleBuilder(LootSelectorEmpty::new);
    }

    public static class Serializer extends LootSelectorEntry.Serializer<LootSelectorEmpty> {
        @Override
        public LootSelectorEmpty deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            return new LootSelectorEmpty(i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
