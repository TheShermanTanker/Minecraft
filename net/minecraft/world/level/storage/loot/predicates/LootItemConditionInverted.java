package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public class LootItemConditionInverted implements LootItemCondition {
    final LootItemCondition term;

    LootItemConditionInverted(LootItemCondition term) {
        this.term = term;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.INVERTED;
    }

    @Override
    public final boolean test(LootTableInfo lootContext) {
        return !this.term.test(lootContext);
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.term.getReferencedContextParams();
    }

    @Override
    public void validate(LootCollector reporter) {
        LootItemCondition.super.validate(reporter);
        this.term.validate(reporter);
    }

    public static LootItemCondition.Builder invert(LootItemCondition.Builder term) {
        LootItemConditionInverted invertedLootItemCondition = new LootItemConditionInverted(term.build());
        return () -> {
            return invertedLootItemCondition;
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionInverted> {
        @Override
        public void serialize(JsonObject json, LootItemConditionInverted object, JsonSerializationContext context) {
            json.add("term", context.serialize(object.term));
        }

        @Override
        public LootItemConditionInverted deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition lootItemCondition = ChatDeserializer.getAsObject(jsonObject, "term", jsonDeserializationContext, LootItemCondition.class);
            return new LootItemConditionInverted(lootItemCondition);
        }
    }
}
