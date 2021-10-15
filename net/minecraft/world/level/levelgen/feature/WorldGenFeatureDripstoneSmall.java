package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.SmallDripstoneConfiguration;

public class WorldGenFeatureDripstoneSmall extends WorldGenerator<SmallDripstoneConfiguration> {
    public WorldGenFeatureDripstoneSmall(Codec<SmallDripstoneConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<SmallDripstoneConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        SmallDripstoneConfiguration smallDripstoneConfiguration = context.config();
        if (!DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos)) {
            return false;
        } else {
            int i = MathHelper.randomBetweenInclusive(random, 1, smallDripstoneConfiguration.maxPlacements);
            boolean bl = false;

            for(int j = 0; j < i; ++j) {
                BlockPosition blockPos2 = randomOffset(random, blockPos, smallDripstoneConfiguration);
                if (searchAndTryToPlaceDripstone(worldGenLevel, random, blockPos2, smallDripstoneConfiguration)) {
                    bl = true;
                }
            }

            return bl;
        }
    }

    private static boolean searchAndTryToPlaceDripstone(GeneratorAccessSeed world, Random random, BlockPosition pos, SmallDripstoneConfiguration config) {
        EnumDirection direction = EnumDirection.getRandom(random);
        EnumDirection direction2 = random.nextBoolean() ? EnumDirection.UP : EnumDirection.DOWN;
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 0; i < config.emptySpaceSearchRadius; ++i) {
            if (!DripstoneUtils.isEmptyOrWater(world, mutableBlockPos)) {
                return false;
            }

            if (tryToPlaceDripstone(world, random, mutableBlockPos, direction2, config)) {
                return true;
            }

            if (tryToPlaceDripstone(world, random, mutableBlockPos, direction2.opposite(), config)) {
                return true;
            }

            mutableBlockPos.move(direction);
        }

        return false;
    }

    private static boolean tryToPlaceDripstone(GeneratorAccessSeed world, Random random, BlockPosition pos, EnumDirection direction, SmallDripstoneConfiguration config) {
        if (!DripstoneUtils.isEmptyOrWater(world, pos)) {
            return false;
        } else {
            BlockPosition blockPos = pos.relative(direction.opposite());
            IBlockData blockState = world.getType(blockPos);
            if (!DripstoneUtils.isDripstoneBase(blockState)) {
                return false;
            } else {
                createPatchOfDripstoneBlocks(world, random, blockPos);
                int i = random.nextFloat() < config.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater(world, pos.relative(direction)) ? 2 : 1;
                DripstoneUtils.growPointedDripstone(world, pos, direction, i, false);
                return true;
            }
        }
    }

    private static void createPatchOfDripstoneBlocks(GeneratorAccessSeed world, Random random, BlockPosition pos) {
        DripstoneUtils.placeDripstoneBlockIfPossible(world, pos);

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (!(random.nextFloat() < 0.3F)) {
                BlockPosition blockPos = pos.relative(direction);
                DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos);
                if (!random.nextBoolean()) {
                    BlockPosition blockPos2 = blockPos.relative(EnumDirection.getRandom(random));
                    DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos2);
                    if (!random.nextBoolean()) {
                        BlockPosition blockPos3 = blockPos2.relative(EnumDirection.getRandom(random));
                        DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos3);
                    }
                }
            }
        }

    }

    private static BlockPosition randomOffset(Random random, BlockPosition pos, SmallDripstoneConfiguration config) {
        return pos.offset(MathHelper.randomBetweenInclusive(random, -config.maxOffsetFromOrigin, config.maxOffsetFromOrigin), MathHelper.randomBetweenInclusive(random, -config.maxOffsetFromOrigin, config.maxOffsetFromOrigin), MathHelper.randomBetweenInclusive(random, -config.maxOffsetFromOrigin, config.maxOffsetFromOrigin));
    }
}
