package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceTaigaMega extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    public WorldGenSurfaceTaigaMega(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        if (noise > 1.75D) {
            WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, WorldGenSurface.CONFIG_COARSE_DIRT);
        } else if (noise > -0.95D) {
            WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, WorldGenSurface.CONFIG_PODZOL);
        } else {
            WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, WorldGenSurface.CONFIG_GRASS);
        }

    }
}
