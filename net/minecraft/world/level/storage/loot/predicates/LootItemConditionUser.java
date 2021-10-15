package net.minecraft.world.level.storage.loot.predicates;

public interface LootItemConditionUser<T> {
    T when(LootItemCondition.Builder condition);

    T unwrap();
}
