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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemFunctionSetDamage extends LootItemFunctionConditional {
    private static final Logger LOGGER = LogManager.getLogger();
    final NumberProvider damage;
    final boolean add;

    LootItemFunctionSetDamage(LootItemCondition[] conditons, NumberProvider durabilityRange, boolean add) {
        super(conditons);
        this.damage = durabilityRange;
        this.add = add;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_DAMAGE;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.damage.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.isDamageableItem()) {
            int i = stack.getMaxDamage();
            float f = this.add ? 1.0F - (float)stack.getDamage() / (float)i : 0.0F;
            float g = 1.0F - MathHelper.clamp(this.damage.getFloat(context) + f, 0.0F, 1.0F);
            stack.setDamage(MathHelper.floor(g * (float)i));
        } else {
            LOGGER.warn("Couldn't set damage of loot item {}", (Object)stack);
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> setDamage(NumberProvider durabilityRange) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetDamage(conditions, durabilityRange, false);
        });
    }

    public static LootItemFunctionConditional.Builder<?> setDamage(NumberProvider durabilityRange, boolean add) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetDamage(conditions, durabilityRange, add);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetDamage> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetDamage object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("damage", context.serialize(object.damage));
            json.addProperty("add", object.add);
        }

        @Override
        public LootItemFunctionSetDamage deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "damage", jsonDeserializationContext, NumberProvider.class);
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "add", false);
            return new LootItemFunctionSetDamage(lootItemConditions, numberProvider, bl);
        }
    }
}
