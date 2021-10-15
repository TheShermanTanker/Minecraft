package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureCoralTree extends WorldGenFeatureCoral {
    public WorldGenFeatureCoralTree(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected boolean placeFeature(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        int i = random.nextInt(3) + 1;

        for(int j = 0; j < i; ++j) {
            if (!this.placeCoralBlock(world, random, mutableBlockPos, state)) {
                return true;
            }

            mutableBlockPos.move(EnumDirection.UP);
        }

        BlockPosition blockPos = mutableBlockPos.immutableCopy();
        int k = random.nextInt(3) + 2;
        List<EnumDirection> list = Lists.newArrayList(EnumDirection.EnumDirectionLimit.HORIZONTAL);
        Collections.shuffle(list, random);

        for(EnumDirection direction : list.subList(0, k)) {
            mutableBlockPos.set(blockPos);
            mutableBlockPos.move(direction);
            int l = random.nextInt(5) + 2;
            int m = 0;

            for(int n = 0; n < l && this.placeCoralBlock(world, random, mutableBlockPos, state); ++n) {
                ++m;
                mutableBlockPos.move(EnumDirection.UP);
                if (n == 0 || m >= 2 && random.nextFloat() < 0.25F) {
                    mutableBlockPos.move(direction);
                    m = 0;
                }
            }
        }

        return true;
    }
}
