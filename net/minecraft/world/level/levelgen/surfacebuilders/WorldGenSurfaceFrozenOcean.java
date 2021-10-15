package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3;
import net.minecraft.world.level.material.Material;

public class WorldGenSurfaceFrozenOcean extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    protected static final IBlockData PACKED_ICE = Blocks.PACKED_ICE.getBlockData();
    protected static final IBlockData SNOW_BLOCK = Blocks.SNOW_BLOCK.getBlockData();
    private static final IBlockData AIR = Blocks.AIR.getBlockData();
    private static final IBlockData GRAVEL = Blocks.GRAVEL.getBlockData();
    private static final IBlockData ICE = Blocks.ICE.getBlockData();
    private NoiseGenerator3 icebergNoise;
    private NoiseGenerator3 icebergRoofNoise;
    private long seed;

    public WorldGenSurfaceFrozenOcean(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        double d = 0.0D;
        double e = 0.0D;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        float f = biome.getAdjustedTemperature(mutableBlockPos.set(x, 63, z));
        double g = Math.min(Math.abs(noise), this.icebergNoise.getValue((double)x * 0.1D, (double)z * 0.1D, false) * 15.0D);
        if (g > 1.8D) {
            double h = 0.09765625D;
            double j = Math.abs(this.icebergRoofNoise.getValue((double)x * 0.09765625D, (double)z * 0.09765625D, false));
            d = g * g * 1.2D;
            double k = Math.ceil(j * 40.0D) + 14.0D;
            if (d > k) {
                d = k;
            }

            if (f > 0.1F) {
                d -= 2.0D;
            }

            if (d > 2.0D) {
                e = (double)seaLevel - d - 7.0D;
                d = d + (double)seaLevel;
            } else {
                d = 0.0D;
            }
        }

        int m = x & 15;
        int n = z & 15;
        WorldGenSurfaceConfiguration surfaceBuilderConfiguration = biome.getGenerationSettings().getSurfaceBuilderConfig();
        IBlockData blockState = surfaceBuilderConfiguration.getUnderMaterial();
        IBlockData blockState2 = surfaceBuilderConfiguration.getTopMaterial();
        IBlockData blockState3 = blockState;
        IBlockData blockState4 = blockState2;
        int o = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        int p = -1;
        int q = 0;
        int r = 2 + random.nextInt(4);
        int s = seaLevel + 18 + random.nextInt(10);

        for(int t = Math.max(height, (int)d + 1); t >= i; --t) {
            mutableBlockPos.set(m, t, n);
            if (chunk.getType(mutableBlockPos).isAir() && t < (int)d && random.nextDouble() > 0.01D) {
                chunk.setType(mutableBlockPos, PACKED_ICE, false);
            } else if (chunk.getType(mutableBlockPos).getMaterial() == Material.WATER && t > (int)e && t < seaLevel && e != 0.0D && random.nextDouble() > 0.15D) {
                chunk.setType(mutableBlockPos, PACKED_ICE, false);
            }

            IBlockData blockState5 = chunk.getType(mutableBlockPos);
            if (blockState5.isAir()) {
                p = -1;
            } else if (!blockState5.is(defaultBlock.getBlock())) {
                if (blockState5.is(Blocks.PACKED_ICE) && q <= r && t > s) {
                    chunk.setType(mutableBlockPos, SNOW_BLOCK, false);
                    ++q;
                }
            } else if (p == -1) {
                if (o <= 0) {
                    blockState4 = AIR;
                    blockState3 = defaultBlock;
                } else if (t >= seaLevel - 4 && t <= seaLevel + 1) {
                    blockState4 = blockState2;
                    blockState3 = blockState;
                }

                if (t < seaLevel && (blockState4 == null || blockState4.isAir())) {
                    if (biome.getAdjustedTemperature(mutableBlockPos.set(x, t, z)) < 0.15F) {
                        blockState4 = ICE;
                    } else {
                        blockState4 = defaultFluid;
                    }
                }

                p = o;
                if (t >= seaLevel - 1) {
                    chunk.setType(mutableBlockPos, blockState4, false);
                } else if (t < seaLevel - 7 - o) {
                    blockState4 = AIR;
                    blockState3 = defaultBlock;
                    chunk.setType(mutableBlockPos, GRAVEL, false);
                } else {
                    chunk.setType(mutableBlockPos, blockState3, false);
                }
            } else if (p > 0) {
                --p;
                chunk.setType(mutableBlockPos, blockState3, false);
                if (p == 0 && blockState3.is(Blocks.SAND) && o > 1) {
                    p = random.nextInt(4) + Math.max(0, t - 63);
                    blockState3 = blockState3.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.getBlockData() : Blocks.SANDSTONE.getBlockData();
                }
            }
        }

    }

    @Override
    public void initNoise(long seed) {
        if (this.seed != seed || this.icebergNoise == null || this.icebergRoofNoise == null) {
            SeededRandom worldgenRandom = new SeededRandom(seed);
            this.icebergNoise = new NoiseGenerator3(worldgenRandom, IntStream.rangeClosed(-3, 0));
            this.icebergRoofNoise = new NoiseGenerator3(worldgenRandom, ImmutableList.of(0));
        }

        this.seed = seed;
    }
}
