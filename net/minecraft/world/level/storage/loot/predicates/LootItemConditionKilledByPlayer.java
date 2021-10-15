package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionKilledByPlayer implements LootItemCondition {
    static final LootItemConditionKilledByPlayer INSTANCE = new LootItemConditionKilledByPlayer();

    private LootItemConditionKilledByPlayer() {
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.KILLED_BY_PLAYER;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.LAST_DAMAGE_PLAYER);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        return lootContext.hasContextParameter(LootContextParameters.LAST_DAMAGE_PLAYER);
    }

    public static LootItemCondition.Builder killedByPlayer() {
        return () -> {
            return INSTANCE;
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionKilledByPlayer> {
        @Override
        public void serialize(JsonObject json, LootItemConditionKilledByPlayer object, JsonSerializationContext context) {
        }

        @Override
        public LootItemConditionKilledByPlayer deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            return LootItemConditionKilledByPlayer.INSTANCE;
        }
    }
}
