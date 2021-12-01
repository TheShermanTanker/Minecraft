package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;

/** @deprecated */
@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
    public static final Codec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256).fieldOf("count").xmap(CountOnEveryLayerPlacement::new, (countOnEveryLayerPlacement) -> {
        return countOnEveryLayerPlacement.count;
    }).codec();
    private final IntProvider count;

    private CountOnEveryLayerPlacement(IntProvider count) {
        this.count = count;
    }

    public static CountOnEveryLayerPlacement of(IntProvider count) {
        return new CountOnEveryLayerPlacement(count);
    }

    public static CountOnEveryLayerPlacement of(int count) {
        return of(IntProviderConstant.of(count));
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        Builder<BlockPosition> builder = Stream.builder();
        int i = 0;

        boolean bl;
        do {
            bl = false;

            for(int j = 0; j < this.count.sample(random); ++j) {
                int k = random.nextInt(16) + pos.getX();
                int l = random.nextInt(16) + pos.getZ();
                int m = context.getHeight(HeightMap.Type.MOTION_BLOCKING, k, l);
                int n = findOnGroundYPosition(context, k, m, l, i);
                if (n != Integer.MAX_VALUE) {
                    builder.add(new BlockPosition(k, n, l));
                    bl = true;
                }
            }

            ++i;
        } while(bl);

        return builder.build();
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.COUNT_ON_EVERY_LAYER;
    }

    private static int findOnGroundYPosition(PlacementContext context, int x, int y, int z, int targetY) {
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
