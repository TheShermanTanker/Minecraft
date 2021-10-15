package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceMesaForest extends WorldGenSurfaceMesa {
    private static final IBlockData WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getBlockData();
    private static final IBlockData ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getBlockData();
    private static final IBlockData TERRACOTTA = Blocks.TERRACOTTA.getBlockData();

    public WorldGenSurfaceMesaForest(Codec<WorldGenSurfaceConfigurationBase> codec) {
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
                            if (p > 86 + m * 2) {
                                if (bl) {
                                    chunk.setType(mutableBlockPos, Blocks.COARSE_DIRT.getBlockData(), false);
                                } else {
                                    chunk.setType(mutableBlockPos, Blocks.GRASS_BLOCK.getBlockData(), false);
                                }
                            } else if (p > seaLevel + 3 + m) {
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
                            if (blockState4 == WHITE_TERRACOTTA) {
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
}
