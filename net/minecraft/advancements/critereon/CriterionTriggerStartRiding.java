package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class CriterionTriggerStartRiding extends CriterionTriggerAbstract<CriterionTriggerStartRiding.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("started_riding");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerStartRiding.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        return new CriterionTriggerStartRiding.CriterionInstanceTrigger(composite);
    }

    public void trigger(EntityPlayer player) {
        this.trigger(player, (conditions) -> {
            return true;
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player) {
            super(CriterionTriggerStartRiding.ID, player);
        }

        public static CriterionTriggerStartRiding.CriterionInstanceTrigger playerStartsRiding(CriterionConditionEntity.Builder player) {
            return new CriterionTriggerStartRiding.CriterionInstanceTrigger(CriterionConditionEntity.Composite.wrap(player.build()));
        }
    }
}
