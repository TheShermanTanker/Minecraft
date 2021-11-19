package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerPlayerHurtEntity extends CriterionTriggerAbstract<CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("player_hurt_entity");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionDamage damagePredicate = CriterionConditionDamage.fromJson(jsonObject.get("damage"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(composite, damagePredicate, composite2);
    }

    public void trigger(EntityPlayer player, Entity entity, DamageSource damage, float dealt, float taken, boolean blocked) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext, damage, dealt, taken, blocked);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionDamage damage;
        private final CriterionConditionEntity.Composite entity;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionDamage damage, CriterionConditionEntity.Composite entity) {
            super(CriterionTriggerPlayerHurtEntity.ID, player);
            this.damage = damage;
            this.entity = entity;
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity() {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionDamage.ANY, CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity(CriterionConditionDamage damagePredicate) {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, damagePredicate, CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity(CriterionConditionDamage.Builder damagePredicateBuilder) {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, damagePredicateBuilder.build(), CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity(CriterionConditionEntity hurtEntityPredicate) {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionDamage.ANY, CriterionConditionEntity.Composite.wrap(hurtEntityPredicate));
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity(CriterionConditionDamage damagePredicate, CriterionConditionEntity hurtEntityPredicate) {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, damagePredicate, CriterionConditionEntity.Composite.wrap(hurtEntityPredicate));
        }

        public static CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger playerHurtEntity(CriterionConditionDamage.Builder damagePredicateBuilder, CriterionConditionEntity hurtEntityPredicate) {
            return new CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, damagePredicateBuilder.build(), CriterionConditionEntity.Composite.wrap(hurtEntityPredicate));
        }

        public boolean matches(EntityPlayer player, LootTableInfo entityContext, DamageSource source, float dealt, float taken, boolean blocked) {
            if (!this.damage.matches(player, source, dealt, taken, blocked)) {
                return false;
            } else {
                return this.entity.matches(entityContext);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("damage", this.damage.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
