package net.minecraft.world.level.storage.loot.functions;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootItemUser;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public interface LootItemFunction extends LootItemUser, BiFunction<ItemStack, LootTableInfo, ItemStack> {
    LootItemFunctionType getType();

    static Consumer<ItemStack> decorate(BiFunction<ItemStack, LootTableInfo, ItemStack> itemApplier, Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        return (stack) -> {
            lootConsumer.accept(itemApplier.apply(stack, context));
        };
    }

    public interface Builder {
        LootItemFunction build();
    }
}
