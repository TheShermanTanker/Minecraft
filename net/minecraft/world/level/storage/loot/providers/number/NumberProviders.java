package net.minecraft.world.level.storage.loot.providers.number;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;

public class NumberProviders {
    public static final LootNumberProviderType CONSTANT = register("constant", new ConstantValue.Serializer());
    public static final LootNumberProviderType UNIFORM = register("uniform", new UniformGenerator.Serializer());
    public static final LootNumberProviderType BINOMIAL = register("binomial", new BinomialDistributionGenerator.Serializer());
    public static final LootNumberProviderType SCORE = register("score", new ScoreboardValue.Serializer());

    private static LootNumberProviderType register(String id, LootSerializer<? extends NumberProvider> jsonSerializer) {
        return IRegistry.register(IRegistry.LOOT_NUMBER_PROVIDER_TYPE, new MinecraftKey(id), new LootNumberProviderType(jsonSerializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_NUMBER_PROVIDER_TYPE, "provider", "type", NumberProvider::getType).withInlineSerializer(CONSTANT, new ConstantValue.InlineSerializer()).withDefaultType(UNIFORM).build();
    }
}
