package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class CriterionTriggerUsedEnderEye extends CriterionTriggerAbstract<CriterionTriggerUsedEnderEye.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("used_ender_eye");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerUsedEnderEye.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("distance"));
        return new CriterionTriggerUsedEnderEye.CriterionInstanceTrigger(composite, doubles);
    }

    public void trigger(EntityPlayer player, BlockPosition strongholdPos) {
        double d = player.locX() - (double)strongholdPos.getX();
        double e = player.locZ() - (double)strongholdPos.getZ();
        double f = d * d + e * e;
        this.trigger(player, (conditions) -> {
            return conditions.matches(f);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionValue.DoubleRange level;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionValue.DoubleRange distance) {
            super(CriterionTriggerUsedEnderEye.ID, player);
            this.level = distance;
        }

        public boolean matches(double distance) {
            return this.level.matchesSqr(distance);
        }
    }
}
