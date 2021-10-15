package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class CriterionTriggerConstructBeacon extends CriterionTriggerAbstract<CriterionTriggerConstructBeacon.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("construct_beacon");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerConstructBeacon.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("level"));
        return new CriterionTriggerConstructBeacon.TriggerInstance(composite, ints);
    }

    public void trigger(EntityPlayer player, int level) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(level);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionValue.IntegerRange level;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionValue.IntegerRange level) {
            super(CriterionTriggerConstructBeacon.ID, player);
            this.level = level;
        }

        public static CriterionTriggerConstructBeacon.TriggerInstance constructedBeacon() {
            return new CriterionTriggerConstructBeacon.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionValue.IntegerRange.ANY);
        }

        public static CriterionTriggerConstructBeacon.TriggerInstance constructedBeacon(CriterionConditionValue.IntegerRange level) {
            return new CriterionTriggerConstructBeacon.TriggerInstance(CriterionConditionEntity.Composite.ANY, level);
        }

        public boolean matches(int level) {
            return this.level.matches(level);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("level", this.level.serializeToJson());
            return jsonObject;
        }
    }
}
