package net.minecraft.world.level.levelgen.placement.nether;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenDecoratorFrequencyConfiguration;
import net.minecraft.world.level.levelgen.placement.WorldGenDecorator;
import net.minecraft.world.level.levelgen.placement.WorldGenDecoratorContext;

public class WorldGenDecoratorCountMultilayer extends WorldGenDecorator<WorldGenDecoratorFrequencyConfiguration> {
    public WorldGenDecoratorCountMultilayer(Codec<WorldGenDecoratorFrequencyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenDecoratorFrequencyConfiguration config, BlockPosition pos) {
        List<BlockPosition> list = Lists.newArrayList();
        int i = 0;

        boolean bl;
        do {
            bl = false;

            for(int j = 0; j < config.count().sample(random); ++j) {
                int k = random.nextInt(16) + pos.getX();
                int l = random.nextInt(16) + pos.getZ();
                int m = context.getHeight(HeightMap.Type.MOTION_BLOCKING, k, l);
                int n = findOnGroundYPosition(context, k, m, l, i);
                if (n != Integer.MAX_VALUE) {
                    list.add(new BlockPosition(k, n, l));
                    bl = true;
                }
            }

            ++i;
        } while(bl);

        return list.stream();
    }

    private static int findOnGroundYPosition(WorldGenDecoratorContext context, int x, int y, int z, int targetY) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, y, z);
        int i = 0;
        IBlockData blockState = context.getBlockState(mutableBlockPos);

        for(int j = y; j >= context.getMinBuildHeight() + 1; --j) {
            mutableBlockPos.setY(j - 1);
            IBlockData blockState2 = context.getBlockState(mutableBlockPos);
            if (!isEmpty(blockState2) && isEmpty(blockState) && !blockState2.is(Blocks.BEDROCK)) {
                if (i == targetY) {
                    return mutableBlockPos.getY() + 1;
                }

                ++i;
            }

            blockState = blockState2;
        }

        return Integer.MAX_VALUE;
    }

    private static boolean isEmpty(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
}
