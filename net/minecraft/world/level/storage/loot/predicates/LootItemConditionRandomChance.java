package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class LootItemConditionRandomChance implements LootItemCondition {
    final float probability;

    LootItemConditionRandomChance(float chance) {
        this.probability = chance;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE;
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        return lootContext.getRandom().nextFloat() < this.probability;
    }

    public static LootItemCondition.Builder randomChance(float chance) {
        return () -> {
            return new LootItemConditionRandomChance(chance);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionRandomChance> {
        @Override
        public void serialize(JsonObject json, LootItemConditionRandomChance object, JsonSerializationContext context) {
            json.addProperty("chance", object.probability);
        }

        @Override
        public LootItemConditionRandomChance deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            return new LootItemConditionRandomChance(ChatDeserializer.getAsFloat(jsonObject, "chance"));
        }
    }
}
