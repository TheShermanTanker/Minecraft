package net.minecraft.world.level.storage.loot.entries;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

@FunctionalInterface
interface LootEntryChildren {
    LootEntryChildren ALWAYS_FALSE = (lootContext, consumer) -> {
        return false;
    };
    LootEntryChildren ALWAYS_TRUE = (lootContext, consumer) -> {
        return true;
    };

    boolean expand(LootTableInfo context, Consumer<LootEntry> choiceConsumer);

    default LootEntryChildren and(LootEntryChildren other) {
        Objects.requireNonNull(other);
        return (context, lootChoiceExpander) -> {
            return this.expand(context, lootChoiceExpander) && other.expand(context, lootChoiceExpander);
        };
    }

    default LootEntryChildren or(LootEntryChildren other) {
        Objects.requireNonNull(other);
        return (context, lootChoiceExpander) -> {
            return this.expand(context, lootChoiceExpander) || other.expand(context, lootChoiceExpander);
        };
    }
}
