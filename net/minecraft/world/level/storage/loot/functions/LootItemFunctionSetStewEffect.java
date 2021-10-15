package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSuspiciousStew;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootItemFunctionSetStewEffect extends LootItemFunctionConditional {
    final Map<MobEffectList, NumberProvider> effectDurationMap;

    LootItemFunctionSetStewEffect(LootItemCondition[] conditions, Map<MobEffectList, NumberProvider> map) {
        super(conditions);
        this.effectDurationMap = ImmutableMap.copyOf(map);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_STEW_EFFECT;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.effectDurationMap.values().stream().flatMap((numberProvider) -> {
            return numberProvider.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.is(Items.SUSPICIOUS_STEW) && !this.effectDurationMap.isEmpty()) {
            Random random = context.getRandom();
            int i = random.nextInt(this.effectDurationMap.size());
            Entry<MobEffectList, NumberProvider> entry = Iterables.get(this.effectDurationMap.entrySet(), i);
            MobEffectList mobEffect = entry.getKey();
            int j = entry.getValue().getInt(context);
            if (!mobEffect.isInstant()) {
                j *= 20;
            }

            ItemSuspiciousStew.saveMobEffect(stack, mobEffect, j);
            return stack;
        } else {
            return stack;
        }
    }

    public static LootItemFunctionSetStewEffect.Builder stewEffect() {
        return new LootItemFunctionSetStewEffect.Builder();
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionSetStewEffect.Builder> {
        private final Map<MobEffectList, NumberProvider> effectDurationMap = Maps.newHashMap();

        @Override
        protected LootItemFunctionSetStewEffect.Builder getThis() {
            return this;
        }

        public LootItemFunctionSetStewEffect.Builder withEffect(MobEffectList effect, NumberProvider durationRange) {
            this.effectDurationMap.put(effect, durationRange);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionSetStewEffect(this.getConditions(), this.effectDurationMap);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetStewEffect> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetStewEffect object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.effectDurationMap.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(MobEffectList mobEffect : object.effectDurationMap.keySet()) {
                    JsonObject jsonObject = new JsonObject();
                    MinecraftKey resourceLocation = IRegistry.MOB_EFFECT.getKey(mobEffect);
                    if (resourceLocation == null) {
                        throw new IllegalArgumentException("Don't know how to serialize mob effect " + mobEffect);
                    }

                    jsonObject.add("type", new JsonPrimitive(resourceLocation.toString()));
                    jsonObject.add("duration", context.serialize(object.effectDurationMap.get(mobEffect)));
                    jsonArray.add(jsonObject);
                }

                json.add("effects", jsonArray);
            }

        }

        @Override
        public LootItemFunctionSetStewEffect deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            Map<MobEffectList, NumberProvider> map = Maps.newHashMap();
            if (jsonObject.has("effects")) {
                for(JsonElement jsonElement : ChatDeserializer.getAsJsonArray(jsonObject, "effects")) {
                    String string = ChatDeserializer.getAsString(jsonElement.getAsJsonObject(), "type");
                    MobEffectList mobEffect = IRegistry.MOB_EFFECT.getOptional(new MinecraftKey(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown mob effect '" + string + "'");
                    });
                    NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonElement.getAsJsonObject(), "duration", jsonDeserializationContext, NumberProvider.class);
                    map.put(mobEffect, numberProvider);
                }
            }

            return new LootItemFunctionSetStewEffect(lootItemConditions, map);
        }
    }
}
