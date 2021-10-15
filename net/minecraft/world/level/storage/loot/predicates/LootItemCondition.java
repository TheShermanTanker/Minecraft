package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootItemUser;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public interface LootItemCondition extends LootItemUser, Predicate<LootTableInfo> {
    LootItemConditionType getType();

    @FunctionalInterface
    public interface Builder {
        LootItemCondition build();

        default LootItemCondition.Builder invert() {
            return LootItemConditionInverted.invert(this);
        }

        default LootItemConditionAlternative.Builder or(LootItemCondition.Builder condition) {
            return LootItemConditionAlternative.alternative(this, condition);
        }
    }
}
