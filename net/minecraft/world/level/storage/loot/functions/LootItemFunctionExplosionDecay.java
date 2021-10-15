package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.Random;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionExplosionDecay extends LootItemFunctionConditional {
    LootItemFunctionExplosionDecay(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.EXPLOSION_DECAY;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Float float_ = context.getContextParameter(LootContextParameters.EXPLOSION_RADIUS);
        if (float_ != null) {
            Random random = context.getRandom();
            float f = 1.0F / float_;
            int i = stack.getCount();
            int j = 0;

            for(int k = 0; k < i; ++k) {
                if (random.nextFloat() <= f) {
                    ++j;
                }
            }

            stack.setCount(j);
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> explosionDecay() {
        return simpleBuilder(LootItemFunctionExplosionDecay::new);
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionExplosionDecay> {
        @Override
        public LootItemFunctionExplosionDecay deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            return new LootItemFunctionExplosionDecay(lootItemConditions);
        }
    }
}
