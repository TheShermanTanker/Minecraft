package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.Vec3D;

public class CriterionTriggerNetherTravel extends CriterionTriggerAbstract<CriterionTriggerNetherTravel.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("nether_travel");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerNetherTravel.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("entered"));
        CriterionConditionLocation locationPredicate2 = CriterionConditionLocation.fromJson(jsonObject.get("exited"));
        CriterionConditionDistance distancePredicate = CriterionConditionDistance.fromJson(jsonObject.get("distance"));
        return new CriterionTriggerNetherTravel.CriterionInstanceTrigger(composite, locationPredicate, locationPredicate2, distancePredicate);
    }

    public void trigger(EntityPlayer player, Vec3D enteredPos) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player.getWorldServer(), enteredPos, player.locX(), player.locY(), player.locZ());
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionLocation entered;
        private final CriterionConditionLocation exited;
        private final CriterionConditionDistance distance;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionLocation enteredPos, CriterionConditionLocation exitedPos, CriterionConditionDistance distance) {
            super(CriterionTriggerNetherTravel.ID, player);
            this.entered = enteredPos;
            this.exited = exitedPos;
            this.distance = distance;
        }

        public static CriterionTriggerNetherTravel.CriterionInstanceTrigger travelledThroughNether(CriterionConditionDistance distance) {
            return new CriterionTriggerNetherTravel.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionLocation.ANY, CriterionConditionLocation.ANY, distance);
        }

        public boolean matches(WorldServer world, Vec3D enteredPos, double exitedPosX, double exitedPosY, double exitedPosZ) {
            if (!this.entered.matches(world, enteredPos.x, enteredPos.y, enteredPos.z)) {
                return false;
            } else if (!this.exited.matches(world, exitedPosX, exitedPosY, exitedPosZ)) {
                return false;
            } else {
                return this.distance.matches(enteredPos.x, enteredPos.y, enteredPos.z, exitedPosX, exitedPosY, exitedPosZ);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("entered", this.entered.serializeToJson());
            jsonObject.add("exited", this.exited.serializeToJson());
            jsonObject.add("distance", this.distance.serializeToJson());
            return jsonObject;
        }
    }
}
