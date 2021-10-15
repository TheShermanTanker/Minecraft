package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public class LootItemConditionEntityProperty implements LootItemCondition {
    final CriterionConditionEntity predicate;
    final LootTableInfo.EntityTarget entityTarget;

    LootItemConditionEntityProperty(CriterionConditionEntity predicate, LootTableInfo.EntityTarget entity) {
        this.predicate = predicate;
        this.entityTarget = entity;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_PROPERTIES;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.ORIGIN, this.entityTarget.getParam());
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        Entity entity = lootContext.getContextParameter(this.entityTarget.getParam());
        Vec3D vec3 = lootContext.getContextParameter(LootContextParameters.ORIGIN);
        return this.predicate.matches(lootContext.getWorld(), vec3, entity);
    }

    public static LootItemCondition.Builder entityPresent(LootTableInfo.EntityTarget entity) {
        return hasProperties(entity, CriterionConditionEntity.Builder.entity());
    }

    public static LootItemCondition.Builder hasProperties(LootTableInfo.EntityTarget entity, CriterionConditionEntity.Builder predicateBuilder) {
        return () -> {
            return new LootItemConditionEntityProperty(predicateBuilder.build(), entity);
        };
    }

    public static LootItemCondition.Builder hasProperties(LootTableInfo.EntityTarget entity, CriterionConditionEntity predicate) {
        return () -> {
            return new LootItemConditionEntityProperty(predicate, entity);
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionEntityProperty> {
        @Override
        public void serialize(JsonObject json, LootItemConditionEntityProperty object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
            json.add("entity", context.serialize(object.entityTarget));
        }

        @Override
        public LootItemConditionEntityProperty deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            CriterionConditionEntity entityPredicate = CriterionConditionEntity.fromJson(jsonObject.get("predicate"));
            return new LootItemConditionEntityProperty(entityPredicate, ChatDeserializer.getAsObject(jsonObject, "entity", jsonDeserializationContext, LootTableInfo.EntityTarget.class));
        }
    }
}
