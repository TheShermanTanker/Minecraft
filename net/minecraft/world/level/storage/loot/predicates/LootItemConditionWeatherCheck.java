package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class LootItemConditionWeatherCheck implements LootItemCondition {
    @Nullable
    final Boolean isRaining;
    @Nullable
    final Boolean isThundering;

    LootItemConditionWeatherCheck(@Nullable Boolean raining, @Nullable Boolean thundering) {
        this.isRaining = raining;
        this.isThundering = thundering;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.WEATHER_CHECK;
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        WorldServer serverLevel = lootContext.getWorld();
        if (this.isRaining != null && this.isRaining != serverLevel.isRaining()) {
            return false;
        } else {
            return this.isThundering == null || this.isThundering == serverLevel.isThundering();
        }
    }

    public static LootItemConditionWeatherCheck.Builder weather() {
        return new LootItemConditionWeatherCheck.Builder();
    }

    public static class Builder implements LootItemCondition.Builder {
        @Nullable
        private Boolean isRaining;
        @Nullable
        private Boolean isThundering;

        public LootItemConditionWeatherCheck.Builder setRaining(@Nullable Boolean raining) {
            this.isRaining = raining;
            return this;
        }

        public LootItemConditionWeatherCheck.Builder setThundering(@Nullable Boolean thundering) {
            this.isThundering = thundering;
            return this;
        }

        @Override
        public LootItemConditionWeatherCheck build() {
            return new LootItemConditionWeatherCheck(this.isRaining, this.isThundering);
        }
    }

    public static class Serializer implements LootSerializer<LootItemConditionWeatherCheck> {
        @Override
        public void serialize(JsonObject json, LootItemConditionWeatherCheck object, JsonSerializationContext context) {
            json.addProperty("raining", object.isRaining);
            json.addProperty("thundering", object.isThundering);
        }

        @Override
        public LootItemConditionWeatherCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Boolean boolean_ = jsonObject.has("raining") ? ChatDeserializer.getAsBoolean(jsonObject, "raining") : null;
            Boolean boolean2 = jsonObject.has("thundering") ? ChatDeserializer.getAsBoolean(jsonObject, "thundering") : null;
            return new LootItemConditionWeatherCheck(boolean_, boolean2);
        }
    }
}
