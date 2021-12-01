package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.Vec3D;

public class DistanceTrigger extends CriterionTriggerAbstract<DistanceTrigger.TriggerInstance> {
    final MinecraftKey id;

    public DistanceTrigger(MinecraftKey id) {
        this.id = id;
    }

    @Override
    public MinecraftKey getId() {
        return this.id;
    }

    @Override
    public DistanceTrigger.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("start_position"));
        CriterionConditionDistance distancePredicate = CriterionConditionDistance.fromJson(jsonObject.get("distance"));
        return new DistanceTrigger.TriggerInstance(this.id, composite, locationPredicate, distancePredicate);
    }

    public void trigger(EntityPlayer player, Vec3D startPos) {
        Vec3D vec3 = player.getPositionVector();
        this.trigger(player, (conditions) -> {
            return conditions.matches(player.getWorldServer(), startPos, vec3);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionLocation startPosition;
        private final CriterionConditionDistance distance;

        public TriggerInstance(MinecraftKey id, CriterionConditionEntity.Composite entity, CriterionConditionLocation startPos, CriterionConditionDistance distance) {
            super(id, entity);
            this.startPosition = startPos;
            this.distance = distance;
        }

        public static DistanceTrigger.TriggerInstance fallFromHeight(CriterionConditionEntity.Builder entity, CriterionConditionDistance distance, CriterionConditionLocation startPos) {
            return new DistanceTrigger.TriggerInstance(CriterionTriggers.FALL_FROM_HEIGHT.id, CriterionConditionEntity.Composite.wrap(entity.build()), startPos, distance);
        }

        public static DistanceTrigger.TriggerInstance rideEntityInLava(CriterionConditionEntity.Builder entity, CriterionConditionDistance distance) {
            return new DistanceTrigger.TriggerInstance(CriterionTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.id, CriterionConditionEntity.Composite.wrap(entity.build()), CriterionConditionLocation.ANY, distance);
        }

        public static DistanceTrigger.TriggerInstance travelledThroughNether(CriterionConditionDistance distance) {
            return new DistanceTrigger.TriggerInstance(CriterionTriggers.NETHER_TRAVEL.id, CriterionConditionEntity.Composite.ANY, CriterionConditionLocation.ANY, distance);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("start_position", this.startPosition.serializeToJson());
            jsonObject.add("distance", this.distance.serializeToJson());
            return jsonObject;
        }

        public boolean matches(WorldServer world, Vec3D startPos, Vec3D endPos) {
            if (!this.startPosition.matches(world, startPos.x, startPos.y, startPos.z)) {
                return false;
            } else {
                return this.distance.matches(startPos.x, startPos.y, startPos.z, endPos.x, endPos.y, endPos.z);
            }
        }
    }
}
