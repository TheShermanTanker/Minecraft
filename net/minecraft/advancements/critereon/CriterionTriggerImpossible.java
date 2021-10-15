package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;

public class CriterionTriggerImpossible implements CriterionTrigger<CriterionTriggerImpossible.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("impossible");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public void addPlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<CriterionTriggerImpossible.TriggerInstance> conditions) {
    }

    @Override
    public void removePlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<CriterionTriggerImpossible.TriggerInstance> conditions) {
    }

    @Override
    public void removePlayerListeners(AdvancementDataPlayer tracker) {
    }

    @Override
    public CriterionTriggerImpossible.TriggerInstance createInstance(JsonObject jsonObject, LootDeserializationContext deserializationContext) {
        return new CriterionTriggerImpossible.TriggerInstance();
    }

    public static class TriggerInstance implements CriterionInstance {
        @Override
        public MinecraftKey getCriterion() {
            return CriterionTriggerImpossible.ID;
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            return new JsonObject();
        }
    }
}
