package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.Material;

public class WorldGenFeatureHugeFungi extends WorldGenerator<WorldGenFeatureHugeFungiConfiguration> {
    private static final float HUGE_PROBABILITY = 0.06F;

    public WorldGenFeatureHugeFungi(Codec<WorldGenFeatureHugeFungiConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureHugeFungiConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        WorldGenFeatureHugeFungiConfiguration hugeFungusConfiguration = context.config();
        Block block = hugeFungusConfiguration.validBaseState.getBlock();
        BlockPosition blockPos2 = null;
        IBlockData blockState = worldGenLevel.getType(blockPos.below());
        if (blockState.is(block)) {
            blockPos2 = blockPos;
        }

        if (blockPos2 == null) {
            return false;
        } else {
            int i = MathHelper.nextInt(random, 4, 13);
            if (random.nextInt(12) == 0) {
                i *= 2;
            }

            if (!hugeFungusConfiguration.planted) {
                int j = chunkGenerator.getGenerationDepth();
                if (blockPos2.getY() + i + 1 >= j) {
                    return false;
                }
            }

            boolean bl = !hugeFungusConfiguration.planted && random.nextFloat() < 0.06F;
            worldGenLevel.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 4);
            this.placeStem(worldGenLevel, random, hugeFungusConfiguration, blockPos2, i, bl);
            this.placeHat(worldGenLevel, random, hugeFungusConfiguration, blockPos2, i, bl);
            return true;
        }
    }

    private static boolean isReplaceable(GeneratorAccess world, BlockPosition pos, boolean replacePlants) {
        return world.isStateAtPosition(pos, (state) -> {
            Material material = state.getMaterial();
            return state.getMaterial().isReplaceable() || replacePlants && material == Material.PLANT;
        });
    }

    private void placeStem(GeneratorAccess world, Random random, WorldGenFeatureHugeFungiConfiguration config, BlockPosition pos, int stemHeight, boolean thickStem) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        IBlockData blockState = config.stemState;
        int i = thickStem ? 1 : 0;

        for(int j = -i; j <= i; ++j) {
            for(int k = -i; k <= i; ++k) {
                boolean bl = thickStem && MathHelper.abs(j) == i && MathHelper.abs(k) == i;

                for(int l = 0; l < stemHeight; ++l) {
                    mutableBlockPos.setWithOffset(pos, j, l, k);
                    if (isReplaceable(world, mutableBlockPos, true)) {
                        if (config.planted) {
                            if (!world.getType(mutableBlockPos.below()).isAir()) {
                                world.destroyBlock(mutableBlockPos, true);
                            }

                            world.setTypeAndData(mutableBlockPos, blockState, 3);
                        } else if (bl) {
                            if (random.nextFloat() < 0.1F) {
                                this.setBlock(world, mutableBlockPos, blockState);
                            }
                        } else {
                            this.setBlock(world, mutableBlockPos, blockState);
                        }
                    }
                }
            }
        }

    }

    private void placeHat(GeneratorAccess world, Random random, WorldGenFeatureHugeFungiConfiguration config, BlockPosition pos, int hatHeight, boolean thickStem) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        boolean bl = config.hatState.is(Blocks.NETHER_WART_BLOCK);
        int i = Math.min(random.nextInt(1 + hatHeight / 3) + 5, hatHeight);
        int j = hatHeight - i;

        for(int k = j; k <= hatHeight; ++k) {
            int l = k < hatHeight - random.nextInt(3) ? 2 : 1;
            if (i > 8 && k < j + 4) {
                l = 3;
            }

            if (thickStem) {
                ++l;
            }

            for(int m = -l; m <= l; ++m) {
                for(int n = -l; n <= l; ++n) {
                    boolean bl2 = m == -l || m == l;
                    boolean bl3 = n == -l || n == l;
                    boolean bl4 = !bl2 && !bl3 && k != hatHeight;
                    boolean bl5 = bl2 && bl3;
                    boolean bl6 = k < j + 3;
                    mutableBlockPos.setWithOffset(pos, m, k, n);
                    if (isReplaceable(world, mutableBlockPos, false)) {
                        if (config.planted && !world.getType(mutableBlockPos.below()).isAir()) {
                            world.destroyBlock(mutableBlockPos, true);
                        }

                        if (bl6) {
                            if (!bl4) {
                                this.placeHatDropBlock(world, random, mutableBlockPos, config.hatState, bl);
                            }
                        } else if (bl4) {
                            this.placeHatBlock(world, random, config, mutableBlockPos, 0.1F, 0.2F, bl ? 0.1F : 0.0F);
                        } else if (bl5) {
                            this.placeHatBlock(world, random, config, mutableBlockPos, 0.01F, 0.7F, bl ? 0.083F : 0.0F);
                        } else {
                            this.placeHatBlock(world, random, config, mutableBlockPos, 5.0E-4F, 0.98F, bl ? 0.07F : 0.0F);
                        }
                    }
                }
            }
        }

    }

    private void placeHatBlock(GeneratorAccess world, Random random, WorldGenFeatureHugeFungiConfiguration config, BlockPosition.MutableBlockPosition pos, float decorationChance, float generationChance, float vineChance) {
        if (random.nextFloat() < decorationChance) {
            this.setBlock(world, pos, config.decorState);
        } else if (random.nextFloat() < generationChance) {
            this.setBlock(world, pos, config.hatState);
            if (random.nextFloat() < vineChance) {
                tryPlaceWeepingVines(pos, world, random);
            }
        }

    }

    private void placeHatDropBlock(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state, boolean bl) {
        if (world.getType(pos.below()).is(state.getBlock())) {
            this.setBlock(world, pos, state);
        } else if ((double)random.nextFloat() < 0.15D) {
            this.setBlock(world, pos, state);
            if (bl && random.nextInt(11) == 0) {
                tryPlaceWeepingVines(pos, world, random);
            }
        }

    }

    private static void tryPlaceWeepingVines(BlockPosition pos, GeneratorAccess world, Random random) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable().move(EnumDirection.DOWN);
        if (world.isEmpty(mutableBlockPos)) {
            int i = MathHelper.nextInt(random, 1, 5);
            if (random.nextInt(7) == 0) {
                i *= 2;
            }

            int j = 23;
            int k = 25;
            WorldGenFeatureWeepingVines.placeWeepingVinesColumn(world, random, mutableBlockPos, i, 23, 25);
        }
    }
}
