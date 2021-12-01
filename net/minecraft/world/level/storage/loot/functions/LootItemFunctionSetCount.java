package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootItemFunctionSetCount extends LootItemFunctionConditional {
    final NumberProvider value;
    final boolean add;

    LootItemFunctionSetCount(LootItemCondition[] conditions, NumberProvider countRange, boolean add) {
        super(conditions);
        this.value = countRange;
        this.add = add;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_COUNT;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        int i = this.add ? stack.getCount() : 0;
        stack.setCount(MathHelper.clamp(i + this.value.getInt(context), 0, stack.getMaxStackSize()));
        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> setCount(NumberProvider countRange) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetCount(conditions, countRange, false);
        });
    }

    public static LootItemFunctionConditional.Builder<?> setCount(NumberProvider countRange, boolean add) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetCount(conditions, countRange, add);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetCount> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetCount object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("count", context.serialize(object.value));
            json.addProperty("add", object.add);
        }

        @Override
        public LootItemFunctionSetCount deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "count", jsonDeserializationContext, NumberProvider.class);
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "add", false);
            return new LootItemFunctionSetCount(lootItemConditions, numberProvider, bl);
        }
    }
}
