package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Predicate;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;

public class LootItemConditions {
    public static final LootItemConditionType INVERTED = register("inverted", new LootItemConditionInverted.Serializer());
    public static final LootItemConditionType ALTERNATIVE = register("alternative", new LootItemConditionAlternative.Serializer());
    public static final LootItemConditionType RANDOM_CHANCE = register("random_chance", new LootItemConditionRandomChance.Serializer());
    public static final LootItemConditionType RANDOM_CHANCE_WITH_LOOTING = register("random_chance_with_looting", new LootItemConditionRandomChanceWithLooting.Serializer());
    public static final LootItemConditionType ENTITY_PROPERTIES = register("entity_properties", new LootItemConditionEntityProperty.Serializer());
    public static final LootItemConditionType KILLED_BY_PLAYER = register("killed_by_player", new LootItemConditionKilledByPlayer.Serializer());
    public static final LootItemConditionType ENTITY_SCORES = register("entity_scores", new LootItemConditionEntityScore.Serializer());
    public static final LootItemConditionType BLOCK_STATE_PROPERTY = register("block_state_property", new LootItemConditionBlockStateProperty.Serializer());
    public static final LootItemConditionType MATCH_TOOL = register("match_tool", new LootItemConditionMatchTool.Serializer());
    public static final LootItemConditionType TABLE_BONUS = register("table_bonus", new LootItemConditionTableBonus.Serializer());
    public static final LootItemConditionType SURVIVES_EXPLOSION = register("survives_explosion", new LootItemConditionSurvivesExplosion.Serializer());
    public static final LootItemConditionType DAMAGE_SOURCE_PROPERTIES = register("damage_source_properties", new LootItemConditionDamageSourceProperties.Serializer());
    public static final LootItemConditionType LOCATION_CHECK = register("location_check", new LootItemConditionLocationCheck.Serializer());
    public static final LootItemConditionType WEATHER_CHECK = register("weather_check", new LootItemConditionWeatherCheck.Serializer());
    public static final LootItemConditionType REFERENCE = register("reference", new LootItemConditionReference.Serializer());
    public static final LootItemConditionType TIME_CHECK = register("time_check", new LootItemConditionTimeCheck.Serializer());
    public static final LootItemConditionType VALUE_CHECK = register("value_check", new ValueCheckCondition.Serializer());

    private static LootItemConditionType register(String id, LootSerializer<? extends LootItemCondition> serializer) {
        return IRegistry.register(IRegistry.LOOT_CONDITION_TYPE, new MinecraftKey(id), new LootItemConditionType(serializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_CONDITION_TYPE, "condition", "condition", LootItemCondition::getType).build();
    }

    public static <T> Predicate<T> andConditions(Predicate<T>[] predicates) {
        switch(predicates.length) {
        case 0:
            return (predicatesx) -> {
                return true;
            };
        case 1:
            return predicates[0];
        case 2:
            return predicates[0].and(predicates[1]);
        default:
            return (operand) -> {
                for(Predicate<T> predicate : predicates) {
                    if (!predicate.test(operand)) {
                        return false;
                    }
                }

                return true;
            };
        }
    }

    public static <T> Predicate<T> orConditions(Predicate<T>[] predicates) {
        switch(predicates.length) {
        case 0:
            return (predicatesx) -> {
                return false;
            };
        case 1:
            return predicates[0];
        case 2:
            return predicates[0].or(predicates[1]);
        default:
            return (operand) -> {
                for(Predicate<T> predicate : predicates) {
                    if (predicate.test(operand)) {
                        return true;
                    }
                }

                return false;
            };
        }
    }
}
