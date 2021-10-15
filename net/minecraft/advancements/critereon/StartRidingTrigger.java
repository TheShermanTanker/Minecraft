package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class StartRidingTrigger extends CriterionTriggerAbstract<StartRidingTrigger.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("started_riding");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public StartRidingTrigger.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        return new StartRidingTrigger.TriggerInstance(composite);
    }

    public void trigger(EntityPlayer player) {
        this.trigger(player, (conditions) -> {
            return true;
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        public TriggerInstance(CriterionConditionEntity.Composite player) {
            super(StartRidingTrigger.ID, player);
        }

        public static StartRidingTrigger.TriggerInstance playerStartsRiding(CriterionConditionEntity.Builder player) {
            return new StartRidingTrigger.TriggerInstance(CriterionConditionEntity.Composite.wrap(player.build()));
        }
    }
}
