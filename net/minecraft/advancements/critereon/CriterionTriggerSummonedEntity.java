package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerSummonedEntity extends CriterionTriggerAbstract<CriterionTriggerSummonedEntity.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("summoned_entity");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerSummonedEntity.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new CriterionTriggerSummonedEntity.CriterionInstanceTrigger(composite, composite2);
    }

    public void trigger(EntityPlayer player, Entity entity) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite entity;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite entity) {
            super(CriterionTriggerSummonedEntity.ID, player);
            this.entity = entity;
        }

        public static CriterionTriggerSummonedEntity.CriterionInstanceTrigger summonedEntity(CriterionConditionEntity.Builder summonedEntityPredicateBuilder) {
            return new CriterionTriggerSummonedEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(summonedEntityPredicateBuilder.build()));
        }

        public boolean matches(LootTableInfo summonedEntityContext) {
            return this.entity.matches(summonedEntityContext);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
