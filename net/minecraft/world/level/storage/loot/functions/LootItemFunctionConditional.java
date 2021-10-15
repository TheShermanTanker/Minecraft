package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionUser;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.apache.commons.lang3.ArrayUtils;

public abstract class LootItemFunctionConditional implements LootItemFunction {
    protected final LootItemCondition[] predicates;
    private final Predicate<LootTableInfo> compositePredicates;

    protected LootItemFunctionConditional(LootItemCondition[] conditions) {
        this.predicates = conditions;
        this.compositePredicates = LootItemConditions.andConditions(conditions);
    }

    @Override
    public final ItemStack apply(ItemStack itemStack, LootTableInfo lootContext) {
        return this.compositePredicates.test(lootContext) ? this.run(itemStack, lootContext) : itemStack;
    }

    protected abstract ItemStack run(ItemStack stack, LootTableInfo context);

    @Override
    public void validate(LootCollector reporter) {
        LootItemFunction.super.validate(reporter);

        for(int i = 0; i < this.predicates.length; ++i) {
            this.predicates[i].validate(reporter.forChild(".conditions[" + i + "]"));
        }

    }

    protected static LootItemFunctionConditional.Builder<?> simpleBuilder(Function<LootItemCondition[], LootItemFunction> joiner) {
        return new LootItemFunctionConditional.DummyBuilder(joiner);
    }

    public abstract static class Builder<T extends LootItemFunctionConditional.Builder<T>> implements LootItemFunction.Builder, LootItemConditionUser<T> {
        private final List<LootItemCondition> conditions = Lists.newArrayList();

        @Override
        public T when(LootItemCondition.Builder builder) {
            this.conditions.add(builder.build());
            return this.getThis();
        }

        @Override
        public final T unwrap() {
            return this.getThis();
        }

        protected abstract T getThis();

        protected LootItemCondition[] getConditions() {
            return this.conditions.toArray(new LootItemCondition[0]);
        }
    }

    static final class DummyBuilder extends LootItemFunctionConditional.Builder<LootItemFunctionConditional.DummyBuilder> {
        private final Function<LootItemCondition[], LootItemFunction> constructor;

        public DummyBuilder(Function<LootItemCondition[], LootItemFunction> joiner) {
            this.constructor = joiner;
        }

        @Override
        protected LootItemFunctionConditional.DummyBuilder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return this.constructor.apply(this.getConditions());
        }
    }

    public abstract static class Serializer<T extends LootItemFunctionConditional> implements LootSerializer<T> {
        @Override
        public void serialize(JsonObject json, T object, JsonSerializationContext context) {
            if (!ArrayUtils.isEmpty((Object[])object.predicates)) {
                json.add("conditions", context.serialize(object.predicates));
            }

        }

        @Override
        public final T deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition[] lootItemConditions = ChatDeserializer.getAsObject(jsonObject, "conditions", new LootItemCondition[0], jsonDeserializationContext, LootItemCondition[].class);
            return this.deserialize(jsonObject, jsonDeserializationContext, lootItemConditions);
        }

        public abstract T deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions);
    }
}
