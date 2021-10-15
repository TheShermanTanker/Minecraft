package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3;

public class WorldGenSurfaceMesa extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    protected static final int MAX_CLAY_DEPTH = 15;
    private static final IBlockData WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getBlockData();
    private static final IBlockData ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getBlockData();
    private static final IBlockData TERRACOTTA = Blocks.TERRACOTTA.getBlockData();
    private static final IBlockData YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.getBlockData();
    private static final IBlockData BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.getBlockData();
    private static final IBlockData RED_TERRACOTTA = Blocks.RED_TERRACOTTA.getBlockData();
    private static final IBlockData LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.getBlockData();
    protected IBlockData[] clayBands;
    protected long seed;
    protected NoiseGenerator3 pillarNoise;
    protected NoiseGenerator3 pillarRoofNoise;
    protected NoiseGenerator3 clayBandsOffsetNoise;

    public WorldGenSurfaceMesa(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        int j = x & 15;
        int k = z & 15;
        IBlockData blockState = WHITE_TERRACOTTA;
        WorldGenSurfaceConfiguration surfaceBuilderConfiguration = biome.getGenerationSettings().getSurfaceBuilderConfig();
        IBlockData blockState2 = surfaceBuilderConfiguration.getUnderMaterial();
        IBlockData blockState3 = surfaceBuilderConfiguration.getTopMaterial();
        IBlockData blockState4 = blockState2;
        int m = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        boolean bl = Math.cos(noise / 3.0D * Math.PI) > 0.0D;
        int n = -1;
        boolean bl2 = false;
        int o = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int p = height; p >= i; --p) {
            if (o < 15) {
                mutableBlockPos.set(j, p, k);
                IBlockData blockState5 = chunk.getType(mutableBlockPos);
                if (blockState5.isAir()) {
                    n = -1;
                } else if (blockState5.is(defaultBlock.getBlock())) {
                    if (n == -1) {
                        bl2 = false;
                        if (m <= 0) {
                            blockState = Blocks.AIR.getBlockData();
                            blockState4 = defaultBlock;
                        } else if (p >= seaLevel - 4 && p <= seaLevel + 1) {
                            blockState = WHITE_TERRACOTTA;
                            blockState4 = blockState2;
                        }

                        if (p < seaLevel && (blockState == null || blockState.isAir())) {
                            blockState = defaultFluid;
                        }

                        n = m + Math.max(0, p - seaLevel);
                        if (p >= seaLevel - 1) {
                            if (p > seaLevel + 3 + m) {
                                IBlockData blockState7;
                                if (p >= 64 && p <= 127) {
                                    if (bl) {
                                        blockState7 = TERRACOTTA;
                                    } else {
                                        blockState7 = this.getBand(x, p, z);
                                    }
                                } else {
                                    blockState7 = ORANGE_TERRACOTTA;
                                }

                                chunk.setType(mutableBlockPos, blockState7, false);
                            } else {
                                chunk.setType(mutableBlockPos, blockState3, false);
                                bl2 = true;
                            }
                        } else {
                            chunk.setType(mutableBlockPos, blockState4, false);
                            if (blockState4.is(Blocks.WHITE_TERRACOTTA) || blockState4.is(Blocks.ORANGE_TERRACOTTA) || blockState4.is(Blocks.MAGENTA_TERRACOTTA) || blockState4.is(Blocks.LIGHT_BLUE_TERRACOTTA) || blockState4.is(Blocks.YELLOW_TERRACOTTA) || blockState4.is(Blocks.LIME_TERRACOTTA) || blockState4.is(Blocks.PINK_TERRACOTTA) || blockState4.is(Blocks.GRAY_TERRACOTTA) || blockState4.is(Blocks.LIGHT_GRAY_TERRACOTTA) || blockState4.is(Blocks.CYAN_TERRACOTTA) || blockState4.is(Blocks.PURPLE_TERRACOTTA) || blockState4.is(Blocks.BLUE_TERRACOTTA) || blockState4.is(Blocks.BROWN_TERRACOTTA) || blockState4.is(Blocks.GREEN_TERRACOTTA) || blockState4.is(Blocks.RED_TERRACOTTA) || blockState4.is(Blocks.BLACK_TERRACOTTA)) {
                                chunk.setType(mutableBlockPos, ORANGE_TERRACOTTA, false);
                            }
                        }
                    } else if (n > 0) {
                        --n;
                        if (bl2) {
                            chunk.setType(mutableBlockPos, ORANGE_TERRACOTTA, false);
                        } else {
                            chunk.setType(mutableBlockPos, this.getBand(x, p, z), false);
                        }
                    }

                    ++o;
                }
            }
        }

    }

    @Override
    public void initNoise(long seed) {
        if (this.seed != seed || this.clayBands == null) {
            this.generateBands(seed);
        }

        if (this.seed != seed || this.pillarNoise == null || this.pillarRoofNoise == null) {
            SeededRandom worldgenRandom = new SeededRandom(seed);
            this.pillarNoise = new NoiseGenerator3(worldgenRandom, IntStream.rangeClosed(-3, 0));
            this.pillarRoofNoise = new NoiseGenerator3(worldgenRandom, ImmutableList.of(0));
        }

        this.seed = seed;
    }

    protected void generateBands(long seed) {
        this.clayBands = new IBlockData[64];
        Arrays.fill(this.clayBands, TERRACOTTA);
        SeededRandom worldgenRandom = new SeededRandom(seed);
        this.clayBandsOffsetNoise = new NoiseGenerator3(worldgenRandom, ImmutableList.of(0));

        for(int i = 0; i < 64; ++i) {
            i += worldgenRandom.nextInt(5) + 1;
            if (i < 64) {
                this.clayBands[i] = ORANGE_TERRACOTTA;
            }
        }

        int j = worldgenRandom.nextInt(4) + 2;

        for(int k = 0; k < j; ++k) {
            int l = worldgenRandom.nextInt(3) + 1;
            int m = worldgenRandom.nextInt(64);

            for(int n = 0; m + n < 64 && n < l; ++n) {
                this.clayBands[m + n] = YELLOW_TERRACOTTA;
            }
        }

        int o = worldgenRandom.nextInt(4) + 2;

        for(int p = 0; p < o; ++p) {
            int q = worldgenRandom.nextInt(3) + 2;
            int r = worldgenRandom.nextInt(64);

            for(int s = 0; r + s < 64 && s < q; ++s) {
                this.clayBands[r + s] = BROWN_TERRACOTTA;
            }
        }

        int t = worldgenRandom.nextInt(4) + 2;

        for(int u = 0; u < t; ++u) {
            int v = worldgenRandom.nextInt(3) + 1;
            int w = worldgenRandom.nextInt(64);

            for(int x = 0; w + x < 64 && x < v; ++x) {
                this.clayBands[w + x] = RED_TERRACOTTA;
            }
        }

        int y = worldgenRandom.nextInt(3) + 3;
        int z = 0;

        for(int aa = 0; aa < y; ++aa) {
            int ab = 1;
            z += worldgenRandom.nextInt(16) + 4;

            for(int ac = 0; z + ac < 64 && ac < 1; ++ac) {
                this.clayBands[z + ac] = WHITE_TERRACOTTA;
                if (z + ac > 1 && worldgenRandom.nextBoolean()) {
                    this.clayBands[z + ac - 1] = LIGHT_GRAY_TERRACOTTA;
                }

                if (z + ac < 63 && worldgenRandom.nextBoolean()) {
                    this.clayBands[z + ac + 1] = LIGHT_GRAY_TERRACOTTA;
                }
            }
        }

    }

    protected IBlockData getBand(int x, int y, int z) {
        int i = (int)Math.round(this.clayBandsOffsetNoise.getValue((double)x / 512.0D, (double)z / 512.0D, false) * 2.0D);
        return this.clayBands[(y + i + 64) % 64];
    }
}
