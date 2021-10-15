package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorOctaves;

public class WorldGenSurfaceNether extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    private static final IBlockData AIR = Blocks.CAVE_AIR.getBlockData();
    private static final IBlockData GRAVEL = Blocks.GRAVEL.getBlockData();
    private static final IBlockData SOUL_SAND = Blocks.SOUL_SAND.getBlockData();
    protected long seed;
    protected NoiseGeneratorOctaves decorationNoise;

    public WorldGenSurfaceNether(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        int j = seaLevel;
        int k = x & 15;
        int m = z & 15;
        double d = 0.03125D;
        boolean bl = this.decorationNoise.getValue((double)x * 0.03125D, (double)z * 0.03125D, 0.0D) * 75.0D + random.nextDouble() > 0.0D;
        boolean bl2 = this.decorationNoise.getValue((double)x * 0.03125D, 109.0D, (double)z * 0.03125D) * 75.0D + random.nextDouble() > 0.0D;
        int n = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int o = -1;
        IBlockData blockState = surfaceBuilderBaseConfiguration.getTopMaterial();
        IBlockData blockState2 = surfaceBuilderBaseConfiguration.getUnderMaterial();

        for(int p = 127; p >= i; --p) {
            mutableBlockPos.set(k, p, m);
            IBlockData blockState3 = chunk.getType(mutableBlockPos);
            if (blockState3.isAir()) {
                o = -1;
            } else if (blockState3.is(defaultBlock.getBlock())) {
                if (o == -1) {
                    boolean bl3 = false;
                    if (n <= 0) {
                        bl3 = true;
                        blockState2 = surfaceBuilderBaseConfiguration.getUnderMaterial();
                    } else if (p >= j - 4 && p <= j + 1) {
                        blockState = surfaceBuilderBaseConfiguration.getTopMaterial();
                        blockState2 = surfaceBuilderBaseConfiguration.getUnderMaterial();
                        if (bl2) {
                            blockState = GRAVEL;
                            blockState2 = surfaceBuilderBaseConfiguration.getUnderMaterial();
                        }

                        if (bl) {
                            blockState = SOUL_SAND;
                            blockState2 = SOUL_SAND;
                        }
                    }

                    if (p < j && bl3) {
                        blockState = defaultFluid;
                    }

                    o = n;
                    if (p >= j - 1) {
                        chunk.setType(mutableBlockPos, blockState, false);
                    } else {
                        chunk.setType(mutableBlockPos, blockState2, false);
                    }
                } else if (o > 0) {
                    --o;
                    chunk.setType(mutableBlockPos, blockState2, false);
                }
            }
        }

    }

    @Override
    public void initNoise(long seed) {
        if (this.seed != seed || this.decorationNoise == null) {
            this.decorationNoise = new NoiseGeneratorOctaves(new SeededRandom(seed), IntStream.rangeClosed(-3, 0));
        }

        this.seed = seed;
    }
}
