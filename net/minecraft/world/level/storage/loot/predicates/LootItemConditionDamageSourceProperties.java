package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.advancements.critereon.CriterionConditionDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public class LootItemConditionDamageSourceProperties implements LootItemCondition {
    final CriterionConditionDamageSource predicate;

    LootItemConditionDamageSourceProperties(CriterionConditionDamageSource predicate) {
        this.predicate = predicate;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.ORIGIN, LootContextParameters.DAMAGE_SOURCE);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        DamageSource damageSource = lootContext.getContextParameter(LootContextParameters.DAMAGE_SOURCE);
        Vec3D vec3 = lootContext.getContextParameter(LootContextParameters.ORIGIN);
        return vec3 != null && damageSource != null && this.predicate.matches(lootContext.getWorld(), vec3, damageSource);
    }

    public static LootItemCondition.Builder hasDamageSource(CriterionConditionDamageSource.Builder builder) {
        return () -> {
            return new LootItemConditionDamageSourceProperties(builder.build());
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionDamageSourceProperties> {
        @Override
        public void serialize(JsonObject json, LootItemConditionDamageSourceProperties object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
        }

        @Override
        public LootItemConditionDamageSourceProperties deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            CriterionConditionDamageSource damageSourcePredicate = CriterionConditionDamageSource.fromJson(jsonObject.get("predicate"));
            return new LootItemConditionDamageSourceProperties(damageSourcePredicate);
        }
    }
}
