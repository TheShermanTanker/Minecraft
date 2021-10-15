package net.minecraft.world.level.storage.loot.providers.nbt;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;

public class NbtProviders {
    public static final LootNbtProviderType STORAGE = register("storage", new StorageNbtProvider.Serializer());
    public static final LootNbtProviderType CONTEXT = register("context", new ContextNbtProvider.Serializer());

    private static LootNbtProviderType register(String id, LootSerializer<? extends NbtProvider> jsonSerializer) {
        return IRegistry.register(IRegistry.LOOT_NBT_PROVIDER_TYPE, new MinecraftKey(id), new LootNbtProviderType(jsonSerializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_NBT_PROVIDER_TYPE, "provider", "type", NbtProvider::getType).withInlineSerializer(CONTEXT, new ContextNbtProvider.InlineSerializer()).build();
    }
}
