package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerTamedAnimal extends CriterionTriggerAbstract<CriterionTriggerTamedAnimal.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("tame_animal");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerTamedAnimal.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new CriterionTriggerTamedAnimal.TriggerInstance(composite, composite2);
    }

    public void trigger(EntityPlayer player, EntityAnimal entity) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite entity;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite entity) {
            super(CriterionTriggerTamedAnimal.ID, player);
            this.entity = entity;
        }

        public static CriterionTriggerTamedAnimal.TriggerInstance tamedAnimal() {
            return new CriterionTriggerTamedAnimal.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerTamedAnimal.TriggerInstance tamedAnimal(CriterionConditionEntity entity) {
            return new CriterionTriggerTamedAnimal.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(entity));
        }

        public boolean matches(LootTableInfo tamedEntityContext) {
            return this.entity.matches(tamedEntityContext);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
