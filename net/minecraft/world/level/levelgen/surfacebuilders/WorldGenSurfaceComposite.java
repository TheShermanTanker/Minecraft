package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceComposite<SC extends WorldGenSurfaceConfiguration> {
    public static final Codec<WorldGenSurfaceComposite<?>> DIRECT_CODEC = IRegistry.SURFACE_BUILDER.dispatch((configuredSurfaceBuilder) -> {
        return configuredSurfaceBuilder.surfaceBuilder;
    }, WorldGenSurface::configuredCodec);
    public static final Codec<Supplier<WorldGenSurfaceComposite<?>>> CODEC = RegistryFileCodec.create(IRegistry.CONFIGURED_SURFACE_BUILDER_REGISTRY, DIRECT_CODEC);
    public final WorldGenSurface<SC> surfaceBuilder;
    public final SC config;

    public WorldGenSurfaceComposite(WorldGenSurface<SC> surfaceBuilder, SC config) {
        this.surfaceBuilder = surfaceBuilder;
        this.config = config;
    }

    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l) {
        this.surfaceBuilder.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, this.config);
    }

    public void initNoise(long seed) {
        this.surfaceBuilder.initNoise(seed);
    }

    public SC config() {
        return this.config;
    }
}
