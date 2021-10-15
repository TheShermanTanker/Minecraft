package net.minecraft.world.level.storage.loot.entries;

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
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionUser;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.apache.commons.lang3.ArrayUtils;

public abstract class LootEntryAbstract implements LootEntryChildren {
    protected final LootItemCondition[] conditions;
    private final Predicate<LootTableInfo> compositeCondition;

    protected LootEntryAbstract(LootItemCondition[] conditions) {
        this.conditions = conditions;
        this.compositeCondition = LootItemConditions.andConditions(conditions);
    }

    public void validate(LootCollector reporter) {
        for(int i = 0; i < this.conditions.length; ++i) {
            this.conditions[i].validate(reporter.forChild(".condition[" + i + "]"));
        }

    }

    protected final boolean canRun(LootTableInfo context) {
        return this.compositeCondition.test(context);
    }

    public abstract LootEntryType getType();

    public abstract static class Builder<T extends LootEntryAbstract.Builder<T>> implements LootItemConditionUser<T> {
        private final List<LootItemCondition> conditions = Lists.newArrayList();

        protected abstract T getThis();

        @Override
        public T when(LootItemCondition.Builder builder) {
            this.conditions.add(builder.build());
            return this.getThis();
        }

        @Override
        public final T unwrap() {
            return this.getThis();
        }

        protected LootItemCondition[] getConditions() {
            return this.conditions.toArray(new LootItemCondition[0]);
        }

        public LootEntryAlternatives.Builder otherwise(LootEntryAbstract.Builder<?> builder) {
            return new LootEntryAlternatives.Builder(this, builder);
        }

        public LootEntryGroup.Builder append(LootEntryAbstract.Builder<?> entry) {
            return new LootEntryGroup.Builder(this, entry);
        }

        public LootEntrySequence.Builder then(LootEntryAbstract.Builder<?> entry) {
            return new LootEntrySequence.Builder(this, entry);
        }

        public abstract LootEntryAbstract build();
    }

    public abstract static class Serializer<T extends LootEntryAbstract> implements LootSerializer<T> {
        @Override
        public final void serialize(JsonObject jsonObject, T lootPoolEntryContainer, JsonSerializationContext jsonSerializationContext) {
            if (!ArrayUtils.isEmpty((Object[])lootPoolEntryContainer.conditions)) {
                jsonObject.add("conditions", jsonSerializationContext.serialize(lootPoolEntryContainer.conditions));
            }

            this.serializeType(jsonObject, lootPoolEntryContainer, jsonSerializationContext);
        }

        @Override
        public final T deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition[] lootItemConditions = ChatDeserializer.getAsObject(jsonObject, "conditions", new LootItemCondition[0], jsonDeserializationContext, LootItemCondition[].class);
            return this.deserializeType(jsonObject, jsonDeserializationContext, lootItemConditions);
        }

        public abstract void serializeType(JsonObject json, T entry, JsonSerializationContext context);

        public abstract T deserializeType(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions);
    }
}
