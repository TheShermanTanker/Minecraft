package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.phys.Vec3D;

public class CriterionTriggerTargetHit extends CriterionTriggerAbstract<CriterionTriggerTargetHit.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("target_hit");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerTargetHit.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("signal_strength"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "projectile", deserializationContext);
        return new CriterionTriggerTargetHit.CriterionInstanceTrigger(composite, ints, composite2);
    }

    public void trigger(EntityPlayer player, Entity projectile, Vec3D hitPos, int signalStrength) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, projectile);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, hitPos, signalStrength);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionValue.IntegerRange signalStrength;
        private final CriterionConditionEntity.Composite projectile;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionValue.IntegerRange signalStrength, CriterionConditionEntity.Composite projectile) {
            super(CriterionTriggerTargetHit.ID, player);
            this.signalStrength = signalStrength;
            this.projectile = projectile;
        }

        public static CriterionTriggerTargetHit.CriterionInstanceTrigger targetHit(CriterionConditionValue.IntegerRange signalStrength, CriterionConditionEntity.Composite projectile) {
            return new CriterionTriggerTargetHit.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, signalStrength, projectile);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("signal_strength", this.signalStrength.serializeToJson());
            jsonObject.add("projectile", this.projectile.toJson(predicateSerializer));
            return jsonObject;
        }

        public boolean matches(LootTableInfo projectileContext, Vec3D hitPos, int signalStrength) {
            if (!this.signalStrength.matches(signalStrength)) {
                return false;
            } else {
                return this.projectile.matches(projectileContext);
            }
        }
    }
}
