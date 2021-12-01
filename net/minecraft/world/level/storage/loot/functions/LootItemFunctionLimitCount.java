package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionLimitCount extends LootItemFunctionConditional {
    final IntRange limiter;

    LootItemFunctionLimitCount(LootItemCondition[] conditions, IntRange limit) {
        super(conditions);
        this.limiter = limit;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.LIMIT_COUNT;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.limiter.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        int i = this.limiter.clamp(context, stack.getCount());
        stack.setCount(i);
        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> limitCount(IntRange limit) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionLimitCount(conditions, limit);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionLimitCount> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionLimitCount object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("limit", context.serialize(object.limiter));
        }

        @Override
        public LootItemFunctionLimitCount deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            IntRange intRange = ChatDeserializer.getAsObject(jsonObject, "limit", jsonDeserializationContext, IntRange.class);
            return new LootItemFunctionLimitCount(lootItemConditions, intRange);
        }
    }
}
