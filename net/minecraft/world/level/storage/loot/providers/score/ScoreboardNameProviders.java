package net.minecraft.world.level.storage.loot.providers.score;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;

public class ScoreboardNameProviders {
    public static final LootScoreProviderType FIXED = register("fixed", new FixedScoreboardNameProvider.Serializer());
    public static final LootScoreProviderType CONTEXT = register("context", new ContextScoreboardNameProvider.Serializer());

    private static LootScoreProviderType register(String id, LootSerializer<? extends ScoreboardNameProvider> jsonSerializer) {
        return IRegistry.register(IRegistry.LOOT_SCORE_PROVIDER_TYPE, new MinecraftKey(id), new LootScoreProviderType(jsonSerializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_SCORE_PROVIDER_TYPE, "provider", "type", ScoreboardNameProvider::getType).withInlineSerializer(CONTEXT, new ContextScoreboardNameProvider.InlineSerializer()).build();
    }
}
