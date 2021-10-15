package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class ValueCheckCondition implements LootItemCondition {
    final NumberProvider provider;
    final IntRange range;

    ValueCheckCondition(NumberProvider value, IntRange range) {
        this.provider = value;
        this.range = range;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.VALUE_CHECK;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return Sets.union(this.provider.getReferencedContextParams(), this.range.getReferencedContextParams());
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        return this.range.test(lootContext, this.provider.getInt(lootContext));
    }

    public static LootItemCondition.Builder hasValue(NumberProvider value, IntRange range) {
        return () -> {
            return new ValueCheckCondition(value, range);
        };
    }

    public static class Serializer implements LootSerializer<ValueCheckCondition> {
        @Override
        public void serialize(JsonObject json, ValueCheckCondition object, JsonSerializationContext context) {
            json.add("value", context.serialize(object.provider));
            json.add("range", context.serialize(object.range));
        }

        @Override
        public ValueCheckCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "value", jsonDeserializationContext, NumberProvider.class);
            IntRange intRange = ChatDeserializer.getAsObject(jsonObject, "range", jsonDeserializationContext, IntRange.class);
            return new ValueCheckCondition(numberProvider, intRange);
        }
    }
}
