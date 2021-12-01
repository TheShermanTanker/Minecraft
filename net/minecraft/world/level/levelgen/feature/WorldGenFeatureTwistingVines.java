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
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class WorldGenFeatureTwistingVines extends WorldGenerator<TwistingVinesConfig> {
    public WorldGenFeatureTwistingVines(Codec<TwistingVinesConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<TwistingVinesConfig> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        if (isInvalidPlacementLocation(worldGenLevel, blockPos)) {
            return false;
        } else {
            Random random = context.random();
            TwistingVinesConfig twistingVinesConfig = context.config();
            int i = twistingVinesConfig.spreadWidth();
            int j = twistingVinesConfig.spreadHeight();
            int k = twistingVinesConfig.maxHeight();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int l = 0; l < i * i; ++l) {
                mutableBlockPos.set(blockPos).move(MathHelper.nextInt(random, -i, i), MathHelper.nextInt(random, -j, j), MathHelper.nextInt(random, -i, i));
                if (findFirstAirBlockAboveGround(worldGenLevel, mutableBlockPos) && !isInvalidPlacementLocation(worldGenLevel, mutableBlockPos)) {
                    int m = MathHelper.nextInt(random, 1, k);
                    if (random.nextInt(6) == 0) {
                        m *= 2;
                    }

                    if (random.nextInt(5) == 0) {
                        m = 1;
                    }

                    int n = 17;
                    int o = 25;
                    placeWeepingVinesColumn(worldGenLevel, random, mutableBlockPos, m, 17, 25);
                }
            }

            return true;
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
