package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenFeatureConfigured<FC extends WorldGenFeatureConfiguration, F extends WorldGenerator<FC>> {
    public static final Codec<WorldGenFeatureConfigured<?, ?>> DIRECT_CODEC = IRegistry.FEATURE.byNameCodec().dispatch((configuredFeature) -> {
        return configuredFeature.feature;
    }, WorldGenerator::configuredCodec);
    public static final Codec<Supplier<WorldGenFeatureConfigured<?, ?>>> CODEC = RegistryFileCodec.create(IRegistry.CONFIGURED_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> LIST_CODEC = RegistryFileCodec.homogeneousList(IRegistry.CONFIGURED_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Logger LOGGER = LogManager.getLogger();
    public final F feature;
    public final FC config;

    public WorldGenFeatureConfigured(F feature, FC config) {
        this.feature = feature;
        this.config = config;
    }

    public F feature() {
        return this.feature;
    }

    public FC config() {
        return this.config;
    }

    public PlacedFeature placed(List<PlacementModifier> modifiers) {
        return new PlacedFeature(() -> {
            return this;
        }, modifiers);
    }

    public PlacedFeature placed(PlacementModifier... modifiers) {
        return this.placed(List.of(modifiers));
    }

    public PlacedFeature filteredByBlockSurvival(Block block) {
        return this.filtered(BlockPredicate.wouldSurvive(block.getBlockData(), BlockPosition.ZERO));
    }

    public PlacedFeature onlyWhenEmpty() {
        return this.filtered(BlockPredicate.matchesBlock(Blocks.AIR, BlockPosition.ZERO));
    }

    public PlacedFeature filtered(BlockPredicate predicate) {
        return this.placed(BlockPredicateFilter.forPredicate(predicate));
    }

    public boolean place(GeneratorAccessSeed world, ChunkGenerator chunkGenerator, Random random, BlockPosition origin) {
        return world.ensureCanWrite(origin) ? this.feature.generate(new FeaturePlaceContext<>(Optional.empty(), world, chunkGenerator, random, origin, this.config)) : false;
    }

    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return Stream.concat(Stream.of(this), this.config.getFeatures());
    }

    @Override
    public String toString() {
        return RegistryGeneration.CONFIGURED_FEATURE.getResourceKey(this).map(Objects::toString).orElseGet(() -> {
            return DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, this).toString();
        });
    }
}
