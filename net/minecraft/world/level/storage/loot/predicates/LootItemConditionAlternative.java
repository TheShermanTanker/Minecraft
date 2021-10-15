package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class LootItemConditionAlternative implements LootItemCondition {
    final LootItemCondition[] terms;
    private final Predicate<LootTableInfo> composedPredicate;

    LootItemConditionAlternative(LootItemCondition[] terms) {
        this.terms = terms;
        this.composedPredicate = LootItemConditions.orConditions(terms);
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ALTERNATIVE;
    }

    @Override
    public final boolean test(LootTableInfo lootContext) {
        return this.composedPredicate.test(lootContext);
    }

    @Override
    public void validate(LootCollector reporter) {
        LootItemCondition.super.validate(reporter);

        for(int i = 0; i < this.terms.length; ++i) {
            this.terms[i].validate(reporter.forChild(".term[" + i + "]"));
        }

    }

    public static LootItemConditionAlternative.Builder alternative(LootItemCondition.Builder... terms) {
        return new LootItemConditionAlternative.Builder(terms);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final List<LootItemCondition> terms = Lists.newArrayList();

        public Builder(LootItemCondition.Builder... terms) {
            for(LootItemCondition.Builder builder : terms) {
                this.terms.add(builder.build());
            }

        }

        @Override
        public LootItemConditionAlternative.Builder or(LootItemCondition.Builder condition) {
            this.terms.add(condition.build());
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new LootItemConditionAlternative(this.terms.toArray(new LootItemCondition[0]));
        }
    }

    public static class Serializer implements LootSerializer<LootItemConditionAlternative> {
        @Override
        public void serialize(JsonObject json, LootItemConditionAlternative object, JsonSerializationContext context) {
            json.add("terms", context.serialize(object.terms));
        }

        @Override
        public LootItemConditionAlternative deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition[] lootItemConditions = ChatDeserializer.getAsObject(jsonObject, "terms", jsonDeserializationContext, LootItemCondition[].class);
            return new LootItemConditionAlternative(lootItemConditions);
        }
    }
}
