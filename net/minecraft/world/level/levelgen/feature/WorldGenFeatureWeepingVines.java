package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockGrowingTop;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureWeepingVines extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();

    public WorldGenFeatureWeepingVines(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        if (!worldGenLevel.isEmpty(blockPos)) {
            return false;
        } else {
            IBlockData blockState = worldGenLevel.getType(blockPos.above());
            if (!blockState.is(Blocks.NETHERRACK) && !blockState.is(Blocks.NETHER_WART_BLOCK)) {
                return false;
            } else {
                this.placeRoofNetherWart(worldGenLevel, random, blockPos);
                this.placeRoofWeepingVines(worldGenLevel, random, blockPos);
                return true;
            }
        }
    }

    private void placeRoofNetherWart(GeneratorAccess world, Random random, BlockPosition pos) {
        world.setTypeAndData(pos, Blocks.NETHER_WART_BLOCK.getBlockData(), 2);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < 200; ++i) {
            mutableBlockPos.setWithOffset(pos, random.nextInt(6) - random.nextInt(6), random.nextInt(2) - random.nextInt(5), random.nextInt(6) - random.nextInt(6));
            if (world.isEmpty(mutableBlockPos)) {
                int j = 0;

                for(EnumDirection direction : DIRECTIONS) {
                    IBlockData blockState = world.getType(mutableBlockPos2.setWithOffset(mutableBlockPos, direction));
                    if (blockState.is(Blocks.NETHERRACK) || blockState.is(Blocks.NETHER_WART_BLOCK)) {
                        ++j;
                    }

                    if (j > 1) {
                        break;
                    }
                }

                if (j == 1) {
                    world.setTypeAndData(mutableBlockPos, Blocks.NETHER_WART_BLOCK.getBlockData(), 2);
                }
            }
        }

    }

    private void placeRoofWeepingVines(GeneratorAccess world, Random random, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < 100; ++i) {
            mutableBlockPos.setWithOffset(pos, random.nextInt(8) - random.nextInt(8), random.nextInt(2) - random.nextInt(7), random.nextInt(8) - random.nextInt(8));
            if (world.isEmpty(mutableBlockPos)) {
                IBlockData blockState = world.getType(mutableBlockPos.above());
                if (blockState.is(Blocks.NETHERRACK) || blockState.is(Blocks.NETHER_WART_BLOCK)) {
                    int j = MathHelper.nextInt(random, 1, 8);
                    if (random.nextInt(6) == 0) {
                        j *= 2;
                    }

                    if (random.nextInt(5) == 0) {
                        j = 1;
                    }

                    int k = 17;
                    int l = 25;
                    placeWeepingVinesColumn(world, random, mutableBlockPos, j, 17, 25);
                }
            }
        }

    }

    public static void placeWeepingVinesColumn(GeneratorAccess world, Random random, BlockPosition.MutableBlockPosition pos, int length, int minAge, int maxAge) {
        for(int i = 0; i <= length; ++i) {
            if (world.isEmpty(pos)) {
                if (i == length || !world.isEmpty(pos.below())) {
                    world.setTypeAndData(pos, Blocks.WEEPING_VINES.getBlockData().set(BlockGrowingTop.AGE, Integer.valueOf(MathHelper.nextInt(random, minAge, maxAge))), 2);
                    break;
                }

                world.setTypeAndData(pos, Blocks.WEEPING_VINES_PLANT.getBlockData(), 2);
            }

            pos.move(EnumDirection.DOWN);
        }

    }
}
