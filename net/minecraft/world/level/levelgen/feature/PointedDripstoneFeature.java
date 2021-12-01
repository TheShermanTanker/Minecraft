package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends WorldGenerator<PointedDripstoneConfiguration> {
    public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<PointedDripstoneConfiguration> context) {
        GeneratorAccess levelAccessor = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        PointedDripstoneConfiguration pointedDripstoneConfiguration = context.config();
        Optional<EnumDirection> optional = getTipDirection(levelAccessor, blockPos, random);
        if (optional.isEmpty()) {
            return false;
        } else {
            BlockPosition blockPos2 = blockPos.relative(optional.get().opposite());
            createPatchOfDripstoneBlocks(levelAccessor, random, blockPos2, pointedDripstoneConfiguration);
            int i = random.nextFloat() < pointedDripstoneConfiguration.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater(levelAccessor.getType(blockPos.relative(optional.get()))) ? 2 : 1;
            DripstoneUtils.growPointedDripstone(levelAccessor, blockPos, optional.get(), i, false);
            return true;
        }
    }

    private static Optional<EnumDirection> getTipDirection(GeneratorAccess world, BlockPosition pos, Random random) {
        boolean bl = DripstoneUtils.isDripstoneBase(world.getType(pos.above()));
        boolean bl2 = DripstoneUtils.isDripstoneBase(world.getType(pos.below()));
        if (bl && bl2) {
            return Optional.of(random.nextBoolean() ? EnumDirection.DOWN : EnumDirection.UP);
        } else if (bl) {
            return Optional.of(EnumDirection.DOWN);
        } else {
            return bl2 ? Optional.of(EnumDirection.UP) : Optional.empty();
        }
    }

    private static void createPatchOfDripstoneBlocks(GeneratorAccess world, Random random, BlockPosition pos, PointedDripstoneConfiguration config) {
        DripstoneUtils.placeDripstoneBlockIfPossible(world, pos);

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (!(random.nextFloat() > config.chanceOfDirectionalSpread)) {
                BlockPosition blockPos = pos.relative(direction);
                DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos);
                if (!(random.nextFloat() > config.chanceOfSpreadRadius2)) {
                    BlockPosition blockPos2 = blockPos.relative(EnumDirection.getRandom(random));
                    DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos2);
                    if (!(random.nextFloat() > config.chanceOfSpreadRadius3)) {
                        BlockPosition blockPos3 = blockPos2.relative(EnumDirection.getRandom(random));
                        DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos3);
                    }
                }
            }
        }

    }
}
