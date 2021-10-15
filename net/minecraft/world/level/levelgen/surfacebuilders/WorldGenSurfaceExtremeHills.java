package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceExtremeHills extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    public WorldGenSurfaceExtremeHills(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        if (noise > 1.0D) {
            WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, WorldGenSurface.CONFIG_STONE);
        } else {
            WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, WorldGenSurface.CONFIG_GRASS);
        }

    }
}
