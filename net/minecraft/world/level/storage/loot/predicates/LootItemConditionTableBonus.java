package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionTableBonus implements LootItemCondition {
    final Enchantment enchantment;
    final float[] values;

    LootItemConditionTableBonus(Enchantment enchantment, float[] chances) {
        this.enchantment = enchantment;
        this.values = chances;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.TABLE_BONUS;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.TOOL);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        ItemStack itemStack = lootContext.getContextParameter(LootContextParameters.TOOL);
        int i = itemStack != null ? EnchantmentManager.getEnchantmentLevel(this.enchantment, itemStack) : 0;
        float f = this.values[Math.min(i, this.values.length - 1)];
        return lootContext.getRandom().nextFloat() < f;
    }

    public static LootItemCondition.Builder bonusLevelFlatChance(Enchantment enchantment, float... chances) {
        return () -> {
            return new LootItemConditionTableBonus(enchantment, chances);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionTableBonus> {
        @Override
        public void serialize(JsonObject json, LootItemConditionTableBonus object, JsonSerializationContext context) {
            json.addProperty("enchantment", IRegistry.ENCHANTMENT.getKey(object.enchantment).toString());
            json.add("chances", context.serialize(object.values));
        }

        @Override
        public LootItemConditionTableBonus deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "enchantment"));
            Enchantment enchantment = IRegistry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonParseException("Invalid enchantment id: " + resourceLocation);
            });
            float[] fs = ChatDeserializer.getAsObject(jsonObject, "chances", jsonDeserializationContext, float[].class);
            return new LootItemConditionTableBonus(enchantment, fs);
        }
    }
}
