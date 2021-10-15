package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockGrowingTop;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureTwistingVines extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureTwistingVines(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        return place(context.level(), context.random(), context.origin(), 8, 4, 8);
    }

    public static boolean place(GeneratorAccess world, Random random, BlockPosition pos, int horizontalSpread, int verticalSpread, int length) {
        if (isInvalidPlacementLocation(world, pos)) {
            return false;
        } else {
            placeTwistingVines(world, random, pos, horizontalSpread, verticalSpread, length);
            return true;
        }
    }

    private static void placeTwistingVines(GeneratorAccess world, Random random, BlockPosition pos, int horizontalSpread, int verticalSpread, int length) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < horizontalSpread * horizontalSpread; ++i) {
            mutableBlockPos.set(pos).move(MathHelper.nextInt(random, -horizontalSpread, horizontalSpread), MathHelper.nextInt(random, -verticalSpread, verticalSpread), MathHelper.nextInt(random, -horizontalSpread, horizontalSpread));
            if (findFirstAirBlockAboveGround(world, mutableBlockPos) && !isInvalidPlacementLocation(world, mutableBlockPos)) {
                int j = MathHelper.nextInt(random, 1, length);
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

    private static boolean findFirstAirBlockAboveGround(GeneratorAccess world, BlockPosition.MutableBlockPosition pos) {
        do {
            pos.move(0, -1, 0);
            if (world.isOutsideWorld(pos)) {
                return false;
            }
        } while(world.getType(pos).isAir());

        pos.move(0, 1, 0);
        return true;
    }

    public static void placeWeepingVinesColumn(GeneratorAccess world, Random random, BlockPosition.MutableBlockPosition pos, int maxLength, int minAge, int maxAge) {
        for(int i = 1; i <= maxLength; ++i) {
            if (world.isEmpty(pos)) {
                if (i == maxLength || !world.isEmpty(pos.above())) {
                    world.setTypeAndData(pos, Blocks.TWISTING_VINES.getBlockData().set(BlockGrowingTop.AGE, Integer.valueOf(MathHelper.nextInt(random, minAge, maxAge))), 2);
                    break;
                }

                world.setTypeAndData(pos, Blocks.TWISTING_VINES_PLANT.getBlockData(), 2);
            }

            pos.move(EnumDirection.UP);
        }

    }

    private static boolean isInvalidPlacementLocation(GeneratorAccess world, BlockPosition pos) {
        if (!world.isEmpty(pos)) {
            return true;
        } else {
            IBlockData blockState = world.getType(pos.below());
            return !blockState.is(Blocks.NETHERRACK) && !blockState.is(Blocks.WARPED_NYLIUM) && !blockState.is(Blocks.WARPED_WART_BLOCK);
        }
    }
}
