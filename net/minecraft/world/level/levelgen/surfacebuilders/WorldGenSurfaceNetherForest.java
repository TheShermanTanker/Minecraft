package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorOctaves;

public class WorldGenSurfaceNetherForest extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    private static final IBlockData AIR = Blocks.CAVE_AIR.getBlockData();
    protected long seed;
    private NoiseGeneratorOctaves decorationNoise;

    public WorldGenSurfaceNetherForest(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        int j = seaLevel;
        int k = x & 15;
        int m = z & 15;
        double d = this.decorationNoise.getValue((double)x * 0.1D, (double)seaLevel, (double)z * 0.1D);
        boolean bl = d > 0.15D + random.nextDouble() * 0.35D;
        double e = this.decorationNoise.getValue((double)x * 0.1D, 109.0D, (double)z * 0.1D);
        boolean bl2 = e > 0.25D + random.nextDouble() * 0.9D;
        int n = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int o = -1;
        IBlockData blockState = surfaceBuilderBaseConfiguration.getUnderMaterial();

        for(int p = 127; p >= i; --p) {
            mutableBlockPos.set(k, p, m);
            IBlockData blockState2 = surfaceBuilderBaseConfiguration.getTopMaterial();
            IBlockData blockState3 = chunk.getType(mutableBlockPos);
            if (blockState3.isAir()) {
                o = -1;
            } else if (blockState3.is(defaultBlock.getBlock())) {
                if (o == -1) {
                    boolean bl3 = false;
                    if (n <= 0) {
                        bl3 = true;
                        blockState = surfaceBuilderBaseConfiguration.getUnderMaterial();
                    }

                    if (bl) {
                        blockState2 = surfaceBuilderBaseConfiguration.getUnderMaterial();
                    } else if (bl2) {
                        blockState2 = surfaceBuilderBaseConfiguration.getUnderwaterMaterial();
                    }

                    if (p < j && bl3) {
                        blockState2 = defaultFluid;
                    }

                    o = n;
                    if (p >= j - 1) {
                        chunk.setType(mutableBlockPos, blockState2, false);
                    } else {
                        chunk.setType(mutableBlockPos, blockState, false);
                    }
                } else if (o > 0) {
                    --o;
                    chunk.setType(mutableBlockPos, blockState, false);
                }
            }
        }

    }

    @Override
    public void initNoise(long seed) {
        if (this.seed != seed || this.decorationNoise == null) {
            this.decorationNoise = new NoiseGeneratorOctaves(new SeededRandom(seed), ImmutableList.of(0));
        }

        this.seed = seed;
    }
}
