package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionSurvivesExplosion implements LootItemCondition {
    static final LootItemConditionSurvivesExplosion INSTANCE = new LootItemConditionSurvivesExplosion();

    private LootItemConditionSurvivesExplosion() {
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.SURVIVES_EXPLOSION;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.EXPLOSION_RADIUS);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        Float float_ = lootContext.getContextParameter(LootContextParameters.EXPLOSION_RADIUS);
        if (float_ != null) {
            Random random = lootContext.getRandom();
            float f = 1.0F / float_;
            return random.nextFloat() <= f;
        } else {
            return true;
        }
    }

    public static LootItemCondition.Builder survivesExplosion() {
        return () -> {
            return INSTANCE;
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionSurvivesExplosion> {
        @Override
        public void serialize(JsonObject json, LootItemConditionSurvivesExplosion object, JsonSerializationContext context) {
        }

        @Override
        public LootItemConditionSurvivesExplosion deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            return LootItemConditionSurvivesExplosion.INSTANCE;
        }
    }
}
