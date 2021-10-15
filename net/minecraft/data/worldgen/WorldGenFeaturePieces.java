package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;

public class WorldGenFeaturePieces {
    public static final ResourceKey<WorldGenFeatureDefinedStructurePoolTemplate> EMPTY = ResourceKey.create(IRegistry.TEMPLATE_POOL_REGISTRY, new MinecraftKey("empty"));
    private static final WorldGenFeatureDefinedStructurePoolTemplate BUILTIN_EMPTY = register(new WorldGenFeatureDefinedStructurePoolTemplate(EMPTY.location(), EMPTY.location(), ImmutableList.of(), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));

    public static WorldGenFeatureDefinedStructurePoolTemplate register(WorldGenFeatureDefinedStructurePoolTemplate templatePool) {
        return RegistryGeneration.register(RegistryGeneration.TEMPLATE_POOL, templatePool.getName(), templatePool);
    }

    public static WorldGenFeatureDefinedStructurePoolTemplate bootstrap() {
        WorldGenFeatureBastionPieces.bootstrap();
        WorldGenFeaturePillagerOutpostPieces.bootstrap();
        WorldGenFeatureVillages.bootstrap();
        return BUILTIN_EMPTY;
    }

    static {
        bootstrap();
    }
}
