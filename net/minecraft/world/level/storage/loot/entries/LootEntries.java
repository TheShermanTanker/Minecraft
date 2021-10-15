package net.minecraft.world.level.storage.loot.entries;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;

public class LootEntries {
    public static final LootEntryType EMPTY = register("empty", new LootSelectorEmpty.Serializer());
    public static final LootEntryType ITEM = register("item", new LootItem.Serializer());
    public static final LootEntryType REFERENCE = register("loot_table", new LootSelectorLootTable.Serializer());
    public static final LootEntryType DYNAMIC = register("dynamic", new LootSelectorDynamic.Serializer());
    public static final LootEntryType TAG = register("tag", new LootSelectorTag.Serializer());
    public static final LootEntryType ALTERNATIVES = register("alternatives", LootEntryChildrenAbstract.createSerializer(LootEntryAlternatives::new));
    public static final LootEntryType SEQUENCE = register("sequence", LootEntryChildrenAbstract.createSerializer(LootEntrySequence::new));
    public static final LootEntryType GROUP = register("group", LootEntryChildrenAbstract.createSerializer(LootEntryGroup::new));

    private static LootEntryType register(String id, LootSerializer<? extends LootEntryAbstract> jsonSerializer) {
        return IRegistry.register(IRegistry.LOOT_POOL_ENTRY_TYPE, new MinecraftKey(id), new LootEntryType(jsonSerializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_POOL_ENTRY_TYPE, "entry", "type", LootEntryAbstract::getType).build();
    }
}
