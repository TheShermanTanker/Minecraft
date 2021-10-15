package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootEnchantLevel extends LootItemFunctionConditional {
    final NumberProvider levels;
    final boolean treasure;

    LootEnchantLevel(LootItemCondition[] conditions, NumberProvider numberProvider, boolean bl) {
        super(conditions);
        this.levels = numberProvider;
        this.treasure = bl;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.ENCHANT_WITH_LEVELS;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.levels.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Random random = context.getRandom();
        return EnchantmentManager.enchantItem(random, stack, this.levels.getInt(context), this.treasure);
    }

    public static LootEnchantLevel.Builder enchantWithLevels(NumberProvider range) {
        return new LootEnchantLevel.Builder(range);
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootEnchantLevel.Builder> {
        private final NumberProvider levels;
        private boolean treasure;

        public Builder(NumberProvider range) {
            this.levels = range;
        }

        @Override
        protected LootEnchantLevel.Builder getThis() {
            return this;
        }

        public LootEnchantLevel.Builder allowTreasure() {
            this.treasure = true;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootEnchantLevel(this.getConditions(), this.levels, this.treasure);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootEnchantLevel> {
        @Override
        public void serialize(JsonObject json, LootEnchantLevel object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("levels", context.serialize(object.levels));
            json.addProperty("treasure", object.treasure);
        }

        @Override
        public LootEnchantLevel deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "levels", jsonDeserializationContext, NumberProvider.class);
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "treasure", false);
            return new LootEnchantLevel(lootItemConditions, numberProvider, bl);
        }
    }
}
