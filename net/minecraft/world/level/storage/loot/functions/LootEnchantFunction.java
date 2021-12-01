package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootEnchantFunction extends LootItemFunctionConditional {
    public static final int NO_LIMIT = 0;
    final NumberProvider value;
    final int limit;

    LootEnchantFunction(LootItemCondition[] conditions, NumberProvider countRange, int limit) {
        super(conditions);
        this.value = countRange;
        this.limit = limit;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.LOOTING_ENCHANT;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return Sets.union(ImmutableSet.of(LootContextParameters.KILLER_ENTITY), this.value.getReferencedContextParams());
    }

    boolean hasLimit() {
        return this.limit > 0;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Entity entity = context.getContextParameter(LootContextParameters.KILLER_ENTITY);
        if (entity instanceof EntityLiving) {
            int i = EnchantmentManager.getMobLooting((EntityLiving)entity);
            if (i == 0) {
                return stack;
            }

            float f = (float)i * this.value.getFloat(context);
            stack.add(Math.round(f));
            if (this.hasLimit() && stack.getCount() > this.limit) {
                stack.setCount(this.limit);
            }
        }

        return stack;
    }

    public static LootEnchantFunction.Builder lootingMultiplier(NumberProvider countRange) {
        return new LootEnchantFunction.Builder(countRange);
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootEnchantFunction.Builder> {
        private final NumberProvider count;
        private int limit = 0;

        public Builder(NumberProvider countRange) {
            this.count = countRange;
        }

        @Override
        protected LootEnchantFunction.Builder getThis() {
            return this;
        }

        public LootEnchantFunction.Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootEnchantFunction(this.getConditions(), this.count, this.limit);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootEnchantFunction> {
        @Override
        public void serialize(JsonObject json, LootEnchantFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("count", context.serialize(object.value));
            if (object.hasLimit()) {
                json.add("limit", context.serialize(object.limit));
            }

        }

        @Override
        public LootEnchantFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            int i = ChatDeserializer.getAsInt(jsonObject, "limit", 0);
            return new LootEnchantFunction(lootItemConditions, ChatDeserializer.getAsObject(jsonObject, "count", jsonDeserializationContext, NumberProvider.class), i);
        }
    }
}
