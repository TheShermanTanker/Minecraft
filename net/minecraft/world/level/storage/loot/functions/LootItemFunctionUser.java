package net.minecraft.world.level.storage.loot.functions;

public interface LootItemFunctionUser<T> {
    T apply(LootItemFunction.Builder function);

    T unwrap();
}
