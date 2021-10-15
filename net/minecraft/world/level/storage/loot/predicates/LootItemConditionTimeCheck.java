package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public class LootItemConditionTimeCheck implements LootItemCondition {
    @Nullable
    final Long period;
    final IntRange value;

    LootItemConditionTimeCheck(@Nullable Long period, IntRange value) {
        this.period = period;
        this.value = value;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.TIME_CHECK;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        WorldServer serverLevel = lootContext.getWorld();
        long l = serverLevel.getDayTime();
        if (this.period != null) {
            l %= this.period;
        }

        return this.value.test(lootContext, (int)l);
    }

    public static LootItemConditionTimeCheck.Builder time(IntRange value) {
        return new LootItemConditionTimeCheck.Builder(value);
    }

    public static class Builder implements LootItemCondition.Builder {
        @Nullable
        private Long period;
        private final IntRange value;

        public Builder(IntRange value) {
            this.value = value;
        }

        public LootItemConditionTimeCheck.Builder setPeriod(long period) {
            this.period = period;
            return this;
        }

        @Override
        public LootItemConditionTimeCheck build() {
            return new LootItemConditionTimeCheck(this.period, this.value);
        }
    }

    public static class Serializer implements LootSerializer<LootItemConditionTimeCheck> {
        @Override
        public void serialize(JsonObject json, LootItemConditionTimeCheck object, JsonSerializationContext context) {
            json.addProperty("period", object.period);
            json.add("value", context.serialize(object.value));
        }

        @Override
        public LootItemConditionTimeCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Long long_ = jsonObject.has("period") ? ChatDeserializer.getAsLong(jsonObject, "period") : null;
            IntRange intRange = ChatDeserializer.getAsObject(jsonObject, "value", jsonDeserializationContext, IntRange.class);
            return new LootItemConditionTimeCheck(long_, intRange);
        }
    }
}
