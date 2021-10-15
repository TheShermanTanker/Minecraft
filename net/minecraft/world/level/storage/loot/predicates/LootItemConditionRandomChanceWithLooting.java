package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionRandomChanceWithLooting implements LootItemCondition {
    final float percent;
    final float lootingMultiplier;

    LootItemConditionRandomChanceWithLooting(float chance, float lootingMultiplier) {
        this.percent = chance;
        this.lootingMultiplier = lootingMultiplier;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE_WITH_LOOTING;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.KILLER_ENTITY);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        Entity entity = lootContext.getContextParameter(LootContextParameters.KILLER_ENTITY);
        int i = 0;
        if (entity instanceof EntityLiving) {
            i = EnchantmentManager.getMobLooting((EntityLiving)entity);
        }

        return lootContext.getRandom().nextFloat() < this.percent + (float)i * this.lootingMultiplier;
    }

    public static LootItemCondition.Builder randomChanceAndLootingBoost(float chance, float lootingMultiplier) {
        return () -> {
            return new LootItemConditionRandomChanceWithLooting(chance, lootingMultiplier);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionRandomChanceWithLooting> {
        @Override
        public void serialize(JsonObject json, LootItemConditionRandomChanceWithLooting object, JsonSerializationContext context) {
            json.addProperty("chance", object.percent);
            json.addProperty("looting_multiplier", object.lootingMultiplier);
        }

        @Override
        public LootItemConditionRandomChanceWithLooting deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            return new LootItemConditionRandomChanceWithLooting(ChatDeserializer.getAsFloat(jsonObject, "chance"), ChatDeserializer.getAsFloat(jsonObject, "looting_multiplier"));
        }
    }
}
