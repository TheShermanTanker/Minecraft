package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.levelgen.feature.configurations.HeightmapConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenDecoratorFrequencyConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorNoiseConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;
import net.minecraft.world.level.levelgen.placement.nether.WorldGenDecoratorCountMultilayer;

public abstract class WorldGenDecorator<DC extends WorldGenFeatureDecoratorConfiguration> {
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> NOPE = register("nope", new WorldGenDecoratorEmpty(WorldGenFeatureEmptyConfiguration2.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorDecpratedConfiguration> DECORATED = register("decorated", new WorldGenDecoratorDecorated(WorldGenDecoratorDecpratedConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorCarveMaskConfiguration> CARVING_MASK = register("carving_mask", new WorldGenDecoratorCarveMask(WorldGenDecoratorCarveMaskConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorFrequencyConfiguration> COUNT_MULTILAYER = register("count_multilayer", new WorldGenDecoratorCountMultilayer(WorldGenDecoratorFrequencyConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> SQUARE = register("square", new WorldGenDecoratorSquare(WorldGenFeatureEmptyConfiguration2.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> DARK_OAK_TREE = register("dark_oak_tree", new WorldGenDecoratorRoofedTree(WorldGenFeatureEmptyConfiguration2.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> ICEBERG = register("iceberg", new WorldGenDecoratorIceburg(WorldGenFeatureEmptyConfiguration2.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorDungeonConfiguration> CHANCE = register("chance", new WorldGenDecoratorChance(WorldGenDecoratorDungeonConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorFrequencyConfiguration> COUNT = register("count", new WorldGenDecoratorCount(WorldGenDecoratorFrequencyConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureDecoratorNoiseConfiguration> COUNT_NOISE = register("count_noise", new WorldGenDecoratorCountNoise(WorldGenFeatureDecoratorNoiseConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorNoiseConfiguration> COUNT_NOISE_BIASED = register("count_noise_biased", new WorldGenDecoratorCountNoiseBiased(WorldGenDecoratorNoiseConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorFrequencyExtraChanceConfiguration> COUNT_EXTRA = register("count_extra", new WorldGenDecoratorCountExtra(WorldGenDecoratorFrequencyExtraChanceConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenDecoratorDungeonConfiguration> LAVA_LAKE = register("lava_lake", new WorldGenDecoratorLakeLava(WorldGenDecoratorDungeonConfiguration.CODEC));
    public static final WorldGenDecorator<HeightmapConfiguration> HEIGHTMAP = register("heightmap", new WorldGenDecoratorHeightmap(HeightmapConfiguration.CODEC));
    public static final WorldGenDecorator<HeightmapConfiguration> HEIGHTMAP_SPREAD_DOUBLE = register("heightmap_spread_double", new WorldGenDecoratorHeightmapSpreadDouble(HeightmapConfiguration.CODEC));
    public static final WorldGenDecorator<WaterDepthThresholdConfiguration> WATER_DEPTH_THRESHOLD = register("water_depth_threshold", new WaterDepthThresholdDecorator(WaterDepthThresholdConfiguration.CODEC));
    public static final WorldGenDecorator<CaveDecoratorConfiguration> CAVE_SURFACE = register("cave_surface", new CaveSurfaceDecorator(CaveDecoratorConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureChanceDecoratorRangeConfiguration> RANGE = register("range", new WorldGenDecoratorRange(WorldGenFeatureChanceDecoratorRangeConfiguration.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> SPREAD_32_ABOVE = register("spread_32_above", new WorldGenDecoratorSpread32Above(WorldGenFeatureEmptyConfiguration2.CODEC));
    public static final WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> END_GATEWAY = register("end_gateway", new WorldGenDecoratorEndGateway(WorldGenFeatureEmptyConfiguration2.CODEC));
    private final Codec<WorldGenDecoratorConfigured<DC>> configuredCodec;

    private static <T extends WorldGenFeatureDecoratorConfiguration, G extends WorldGenDecorator<T>> G register(String registryName, G decorator) {
        return IRegistry.register(IRegistry.DECORATOR, registryName, decorator);
    }

    public WorldGenDecorator(Codec<DC> configCodec) {
        this.configuredCodec = configCodec.fieldOf("config").xmap((decoratorConfiguration) -> {
            return new WorldGenDecoratorConfigured<>(this, decoratorConfiguration);
        }, WorldGenDecoratorConfigured::config).codec();
    }

    public WorldGenDecoratorConfigured<DC> configured(DC config) {
        return new WorldGenDecoratorConfigured<>(this, config);
    }

    public Codec<WorldGenDecoratorConfigured<DC>> configuredCodec() {
        return this.configuredCodec;
    }

    public abstract Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, DC config, BlockPosition pos);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode());
    }
}
