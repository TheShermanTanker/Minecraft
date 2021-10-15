package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemConditionReference implements LootItemCondition {
    private static final Logger LOGGER = LogManager.getLogger();
    final MinecraftKey name;

    LootItemConditionReference(MinecraftKey id) {
        this.name = id;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.REFERENCE;
    }

    @Override
    public void validate(LootCollector reporter) {
        if (reporter.hasVisitedCondition(this.name)) {
            reporter.reportProblem("Condition " + this.name + " is recursively called");
        } else {
            LootItemCondition.super.validate(reporter);
            LootItemCondition lootItemCondition = reporter.resolveCondition(this.name);
            if (lootItemCondition == null) {
                reporter.reportProblem("Unknown condition table called " + this.name);
            } else {
                lootItemCondition.validate(reporter.enterTable(".{" + this.name + "}", this.name));
            }

        }
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        LootItemCondition lootItemCondition = lootContext.getCondition(this.name);
        if (lootContext.addVisitedCondition(lootItemCondition)) {
            boolean var3;
            try {
                var3 = lootItemCondition.test(lootContext);
            } finally {
                lootContext.removeVisitedCondition(lootItemCondition);
            }

            return var3;
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
        }
    }

    public static LootItemCondition.Builder conditionReference(MinecraftKey id) {
        return () -> {
            return new LootItemConditionReference(id);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionReference> {
        @Override
        public void serialize(JsonObject json, LootItemConditionReference object, JsonSerializationContext context) {
            json.addProperty("name", object.name.toString());
        }

        @Override
        public LootItemConditionReference deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "name"));
            return new LootItemConditionReference(resourceLocation);
        }
    }
}
