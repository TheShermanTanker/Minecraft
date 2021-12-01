package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class StructureFeature<FC extends WorldGenFeatureConfiguration, F extends StructureGenerator<FC>> {
    public static final Codec<StructureFeature<?, ?>> DIRECT_CODEC = IRegistry.STRUCTURE_FEATURE.byNameCodec().dispatch((configuredStructureFeature) -> {
        return configuredStructureFeature.feature;
    }, StructureGenerator::configuredStructureCodec);
    public static final Codec<Supplier<StructureFeature<?, ?>>> CODEC = RegistryFileCodec.create(IRegistry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<Supplier<StructureFeature<?, ?>>>> LIST_CODEC = RegistryFileCodec.homogeneousList(IRegistry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
    public final F feature;
    public final FC config;

    public StructureFeature(F feature, FC config) {
        this.feature = feature;
        this.config = config;
    }

    public StructureStart<?> generate(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, DefinedStructureManager structureManager, long worldSeed, ChunkCoordIntPair chunkPos, int structureReferences, StructureSettingsFeature structureConfig, IWorldHeightAccess world, Predicate<BiomeBase> biomeLimit) {
        return this.feature.generate(registryManager, chunkGenerator, biomeSource, structureManager, worldSeed, chunkPos, structureReferences, structureConfig, this.config, world, biomeLimit);
    }
}
