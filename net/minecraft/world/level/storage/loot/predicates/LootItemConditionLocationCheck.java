package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.advancements.critereon.CriterionConditionLocation;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public class LootItemConditionLocationCheck implements LootItemCondition {
    final CriterionConditionLocation predicate;
    final BlockPosition offset;

    LootItemConditionLocationCheck(CriterionConditionLocation predicate, BlockPosition offset) {
        this.predicate = predicate;
        this.offset = offset;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.LOCATION_CHECK;
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        Vec3D vec3 = lootContext.getContextParameter(LootContextParameters.ORIGIN);
        return vec3 != null && this.predicate.matches(lootContext.getWorld(), vec3.getX() + (double)this.offset.getX(), vec3.getY() + (double)this.offset.getY(), vec3.getZ() + (double)this.offset.getZ());
    }

    public static LootItemCondition.Builder checkLocation(CriterionConditionLocation.Builder predicateBuilder) {
        return () -> {
            return new LootItemConditionLocationCheck(predicateBuilder.build(), BlockPosition.ZERO);
        };
    }

    public static LootItemCondition.Builder checkLocation(CriterionConditionLocation.Builder predicateBuilder, BlockPosition pos) {
        return () -> {
            return new LootItemConditionLocationCheck(predicateBuilder.build(), pos);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionLocationCheck> {
        @Override
        public void serialize(JsonObject json, LootItemConditionLocationCheck object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
            if (object.offset.getX() != 0) {
                json.addProperty("offsetX", object.offset.getX());
            }

            if (object.offset.getY() != 0) {
                json.addProperty("offsetY", object.offset.getY());
            }

            if (object.offset.getZ() != 0) {
                json.addProperty("offsetZ", object.offset.getZ());
            }

        }

        @Override
        public LootItemConditionLocationCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("predicate"));
            int i = ChatDeserializer.getAsInt(jsonObject, "offsetX", 0);
            int j = ChatDeserializer.getAsInt(jsonObject, "offsetY", 0);
            int k = ChatDeserializer.getAsInt(jsonObject, "offsetZ", 0);
            return new LootItemConditionLocationCheck(locationPredicate, new BlockPosition(i, j, k));
        }
    }
}
