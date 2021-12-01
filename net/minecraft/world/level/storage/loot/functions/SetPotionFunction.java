package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction extends LootItemFunctionConditional {
    final PotionRegistry potion;

    SetPotionFunction(LootItemCondition[] conditions, PotionRegistry potion) {
        super(conditions);
        this.potion = potion;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_POTION;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        PotionUtil.setPotion(stack, this.potion);
        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> setPotion(PotionRegistry potion) {
        return simpleBuilder((conditions) -> {
            return new SetPotionFunction(conditions, potion);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<SetPotionFunction> {
        @Override
        public void serialize(JsonObject json, SetPotionFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("id", IRegistry.POTION.getKey(object.potion).toString());
        }

        @Override
        public SetPotionFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            String string = ChatDeserializer.getAsString(jsonObject, "id");
            PotionRegistry potion = IRegistry.POTION.getOptional(MinecraftKey.tryParse(string)).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown potion '" + string + "'");
            });
            return new SetPotionFunction(lootItemConditions, potion);
        }
    }
}
