package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItem extends LootSelectorEntry {
    final Item item;

    LootItem(Item item, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.item = item;
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.ITEM;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        lootConsumer.accept(new ItemStack(this.item));
    }

    public static LootSelectorEntry.Builder<?> lootTableItem(IMaterial drop) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootItem(drop.getItem(), weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootSelectorEntry.Serializer<LootItem> {
        @Override
        public void serializeType(JsonObject json, LootItem entry, JsonSerializationContext context) {
            super.serializeType(json, entry, context);
            MinecraftKey resourceLocation = IRegistry.ITEM.getKey(entry.item);
            if (resourceLocation == null) {
                throw new IllegalArgumentException("Can't serialize unknown item " + entry.item);
            } else {
                json.addProperty("name", resourceLocation.toString());
            }
        }

        @Override
        protected LootItem deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            Item item = ChatDeserializer.getAsItem(jsonObject, "name");
            return new LootItem(item, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
