package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerKilled extends CriterionTriggerAbstract<CriterionTriggerKilled.CriterionInstanceTrigger> {
    final MinecraftKey id;

    public CriterionTriggerKilled(MinecraftKey id) {
        this.id = id;
    }

    @Override
    public MinecraftKey getId() {
        return this.id;
    }

    @Override
    public CriterionTriggerKilled.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        return new CriterionTriggerKilled.CriterionInstanceTrigger(this.id, composite, CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext), CriterionConditionDamageSource.fromJson(jsonObject.get("killing_blow")));
    }

    public void trigger(EntityPlayer player, Entity entity, DamageSource killingDamage) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext, killingDamage);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite entityPredicate;
        private final CriterionConditionDamageSource killingBlow;

        public CriterionInstanceTrigger(MinecraftKey id, CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite entity, CriterionConditionDamageSource killingBlow) {
            super(id, player);
            this.entityPredicate = entity;
            this.killingBlow = killingBlow;
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity killedEntityPredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicate), CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity.Builder killedEntityPredicateBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicateBuilder.build()), CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity() {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity killedEntityPredicate, CriterionConditionDamageSource damageSourcePredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicate), damageSourcePredicate);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity.Builder killedEntityPredicateBuilder, CriterionConditionDamageSource damageSourcePredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicateBuilder.build()), damageSourcePredicate);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity killedEntityPredicate, CriterionConditionDamageSource.Builder damageSourcePredicateBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicate), damageSourcePredicateBuilder.build());
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger playerKilledEntity(CriterionConditionEntity.Builder killedEntityPredicateBuilder, CriterionConditionDamageSource.Builder killingBlowBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.PLAYER_KILLED_ENTITY.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killedEntityPredicateBuilder.build()), killingBlowBuilder.build());
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity killerEntityPredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicate), CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity.Builder killerEntityPredicateBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicateBuilder.build()), CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer() {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionDamageSource.ANY);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity killerEntityPredicate, CriterionConditionDamageSource damageSourcePredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicate), damageSourcePredicate);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity.Builder killerEntityPredicateBuilder, CriterionConditionDamageSource damageSourcePredicate) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicateBuilder.build()), damageSourcePredicate);
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity killerEntityPredicate, CriterionConditionDamageSource.Builder damageSourcePredicateBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicate), damageSourcePredicateBuilder.build());
        }

        public static CriterionTriggerKilled.CriterionInstanceTrigger entityKilledPlayer(CriterionConditionEntity.Builder killerEntityPredicateBuilder, CriterionConditionDamageSource.Builder damageSourcePredicateBuilder) {
            return new CriterionTriggerKilled.CriterionInstanceTrigger(CriterionTriggers.ENTITY_KILLED_PLAYER.id, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(killerEntityPredicateBuilder.build()), damageSourcePredicateBuilder.build());
        }

        public boolean matches(EntityPlayer player, LootTableInfo killedEntityContext, DamageSource killingBlow) {
            return !this.killingBlow.matches(player, killingBlow) ? false : this.entityPredicate.matches(killedEntityContext);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("entity", this.entityPredicate.toJson(predicateSerializer));
            jsonObject.add("killing_blow", this.killingBlow.serializeToJson());
            return jsonObject;
        }
    }
}
