package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public class WorldGenSurfaceDefaultBlock extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    public WorldGenSurfaceDefaultBlock(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        this.apply(random, chunk, biome, x, z, height, noise, defaultBlock, defaultFluid, surfaceBuilderBaseConfiguration.getTopMaterial(), surfaceBuilderBaseConfiguration.getUnderMaterial(), surfaceBuilderBaseConfiguration.getUnderwaterMaterial(), seaLevel, i);
    }

    protected void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData fluidBlock, IBlockData topBlock, IBlockData underBlock, IBlockData underwaterBlock, int seaLevel, int i) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int j = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        if (j == 0) {
            boolean bl = false;

            for(int k = height; k >= i; --k) {
                mutableBlockPos.set(x, k, z);
                IBlockData blockState = chunk.getType(mutableBlockPos);
                if (blockState.isAir()) {
                    bl = false;
                } else if (blockState.is(defaultBlock.getBlock())) {
                    if (!bl) {
                        IBlockData blockState2;
                        if (k >= seaLevel) {
                            blockState2 = Blocks.AIR.getBlockData();
                        } else if (k == seaLevel - 1) {
                            blockState2 = biome.getAdjustedTemperature(mutableBlockPos) < 0.15F ? Blocks.ICE.getBlockData() : fluidBlock;
                        } else if (k >= seaLevel - (7 + j)) {
                            blockState2 = defaultBlock;
                        } else {
                            blockState2 = underwaterBlock;
                        }

                        chunk.setType(mutableBlockPos, blockState2, false);
                    }

                    bl = true;
                }
            }
        } else {
            IBlockData blockState6 = underBlock;
            int l = -1;

            for(int m = height; m >= i; --m) {
                mutableBlockPos.set(x, m, z);
                IBlockData blockState7 = chunk.getType(mutableBlockPos);
                if (blockState7.isAir()) {
                    l = -1;
                } else if (blockState7.is(defaultBlock.getBlock())) {
                    if (l == -1) {
                        l = j;
                        IBlockData blockState8;
                        if (m >= seaLevel + 2) {
                            blockState8 = topBlock;
                        } else if (m >= seaLevel - 1) {
                            blockState6 = underBlock;
                            blockState8 = topBlock;
                        } else if (m >= seaLevel - 4) {
                            blockState6 = underBlock;
                            blockState8 = underBlock;
                        } else if (m >= seaLevel - (7 + j)) {
                            blockState8 = blockState6;
                        } else {
                            blockState6 = defaultBlock;
                            blockState8 = underwaterBlock;
                        }

                        chunk.setType(mutableBlockPos, blockState8, false);
                    } else if (l > 0) {
                        --l;
                        chunk.setType(mutableBlockPos, blockState6, false);
                        if (l == 0 && blockState6.is(Blocks.SAND) && j > 1) {
                            l = random.nextInt(4) + Math.max(0, m - seaLevel);
                            blockState6 = blockState6.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.getBlockData() : Blocks.SANDSTONE.getBlockData();
                        }
                    }
                }
            }
        }

    }
}
