package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class CriterionTriggerTick extends CriterionTriggerAbstract<CriterionTriggerTick.TriggerInstance> {
    public static final MinecraftKey ID = new MinecraftKey("tick");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerTick.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        return new CriterionTriggerTick.TriggerInstance(composite);
    }

    public void trigger(EntityPlayer player) {
        this.trigger(player, (conditions) -> {
            return true;
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        public TriggerInstance(CriterionConditionEntity.Composite player) {
            super(CriterionTriggerTick.ID, player);
        }
    }
}
