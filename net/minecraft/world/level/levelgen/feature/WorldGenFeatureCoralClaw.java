package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureCoralClaw extends WorldGenFeatureCoral {
    public WorldGenFeatureCoralClaw(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected boolean placeFeature(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state) {
        if (!this.placeCoralBlock(world, random, pos, state)) {
            return false;
        } else {
            EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
            int i = random.nextInt(2) + 2;
            List<EnumDirection> list = Lists.newArrayList(direction, direction.getClockWise(), direction.getCounterClockWise());
            Collections.shuffle(list, random);

            for(EnumDirection direction2 : list.subList(0, i)) {
                BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
                int j = random.nextInt(2) + 1;
                mutableBlockPos.move(direction2);
                int k;
                EnumDirection direction3;
                if (direction2 == direction) {
                    direction3 = direction;
                    k = random.nextInt(3) + 2;
                } else {
                    mutableBlockPos.move(EnumDirection.UP);
                    EnumDirection[] directions = new EnumDirection[]{direction2, EnumDirection.UP};
                    direction3 = SystemUtils.getRandom(directions, random);
                    k = random.nextInt(3) + 3;
                }

                for(int m = 0; m < j && this.placeCoralBlock(world, random, mutableBlockPos, state); ++m) {
                    mutableBlockPos.move(direction3);
                }

                mutableBlockPos.move(direction3.opposite());
                mutableBlockPos.move(EnumDirection.UP);

                for(int n = 0; n < k; ++n) {
                    mutableBlockPos.move(direction);
                    if (!this.placeCoralBlock(world, random, mutableBlockPos, state)) {
                        break;
                    }

                    if (random.nextFloat() < 0.25F) {
                        mutableBlockPos.move(EnumDirection.UP);
                    }
                }
            }

            return true;
        }
    }
}
