package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.phys.Vec3D;

public class CriterionTriggerLevitation extends CriterionTriggerAbstract<CriterionTriggerLevitation.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("levitation");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerLevitation.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionDistance distancePredicate = CriterionConditionDistance.fromJson(jsonObject.get("distance"));
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("duration"));
        return new CriterionTriggerLevitation.TriggerInstance(composite, distancePredicate, ints);
    }

    public void trigger(EntityPlayer player, Vec3D startPos, int duration) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, startPos, duration);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionDistance distance;
        private final CriterionConditionValue.IntegerRange duration;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionDistance distance, CriterionConditionValue.IntegerRange duration) {
            super(CriterionTriggerLevitation.ID, player);
            this.distance = distance;
            this.duration = duration;
        }

        public static CriterionTriggerLevitation.TriggerInstance levitated(CriterionConditionDistance distance) {
            return new CriterionTriggerLevitation.TriggerInstance(CriterionConditionEntity.Composite.ANY, distance, CriterionConditionValue.IntegerRange.ANY);
        }

        public boolean matches(EntityPlayer player, Vec3D startPos, int duration) {
            if (!this.distance.matches(startPos.x, startPos.y, startPos.z, player.locX(), player.locY(), player.locZ())) {
                return false;
            } else {
                return this.duration.matches(duration);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("distance", this.distance.serializeToJson());
            jsonObject.add("duration", this.duration.serializeToJson());
            return jsonObject;
        }
    }
}
