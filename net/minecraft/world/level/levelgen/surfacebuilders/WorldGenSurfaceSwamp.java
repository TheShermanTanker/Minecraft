package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceSwamp extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    public WorldGenSurfaceSwamp(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        double d = BiomeBase.BIOME_INFO_NOISE.getValue((double)x * 0.25D, (double)z * 0.25D, false);
        if (d > 0.0D) {
            int j = x & 15;
            int k = z & 15;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int m = height; m >= i; --m) {
                mutableBlockPos.set(j, m, k);
                if (!chunk.getType(mutableBlockPos).isAir()) {
                    if (m == 62 && !chunk.getType(mutableBlockPos).is(defaultFluid.getBlock())) {
                        chunk.setType(mutableBlockPos, defaultFluid, false);
                    }
                    break;
                }
            }
        }

        WorldGenSurface.DEFAULT.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, seaLevel, i, l, surfaceBuilderBaseConfiguration);
    }
}
