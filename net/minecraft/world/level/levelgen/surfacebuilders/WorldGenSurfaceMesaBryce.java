package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceMesaBryce extends WorldGenSurfaceMesa {
    private static final IBlockData WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getBlockData();
    private static final IBlockData ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getBlockData();
    private static final IBlockData TERRACOTTA = Blocks.TERRACOTTA.getBlockData();

    public WorldGenSurfaceMesaBryce(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        double d = 0.0D;
        double e = Math.min(Math.abs(noise), this.pillarNoise.getValue((double)x * 0.25D, (double)z * 0.25D, false) * 15.0D);
        if (e > 0.0D) {
            double f = 0.001953125D;
            double g = Math.abs(this.pillarRoofNoise.getValue((double)x * 0.001953125D, (double)z * 0.001953125D, false));
            d = e * e * 2.5D;
            double h = Math.ceil(g * 50.0D) + 14.0D;
            if (d > h) {
                d = h;
            }

            d = d + 64.0D;
        }

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
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int o = Math.max(height, (int)d + 1); o >= i; --o) {
            mutableBlockPos.set(j, o, k);
            if (chunk.getType(mutableBlockPos).isAir() && o < (int)d) {
                chunk.setType(mutableBlockPos, defaultBlock, false);
            }

            IBlockData blockState5 = chunk.getType(mutableBlockPos);
            if (blockState5.isAir()) {
                n = -1;
            } else if (blockState5.is(defaultBlock.getBlock())) {
                if (n == -1) {
                    bl2 = false;
                    if (m <= 0) {
                        blockState = Blocks.AIR.getBlockData();
                        blockState4 = defaultBlock;
                    } else if (o >= seaLevel - 4 && o <= seaLevel + 1) {
                        blockState = WHITE_TERRACOTTA;
                        blockState4 = blockState2;
                    }

                    if (o < seaLevel && (blockState == null || blockState.isAir())) {
                        blockState = defaultFluid;
                    }

                    n = m + Math.max(0, o - seaLevel);
                    if (o >= seaLevel - 1) {
                        if (o <= seaLevel + 3 + m) {
                            chunk.setType(mutableBlockPos, blockState3, false);
                            bl2 = true;
                        } else {
                            IBlockData blockState7;
                            if (o >= 64 && o <= 127) {
                                if (bl) {
                                    blockState7 = TERRACOTTA;
                                } else {
                                    blockState7 = this.getBand(x, o, z);
                                }
                            } else {
                                blockState7 = ORANGE_TERRACOTTA;
                            }

                            chunk.setType(mutableBlockPos, blockState7, false);
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
                        chunk.setType(mutableBlockPos, this.getBand(x, o, z), false);
                    }
                }
            }
        }

    }
}
