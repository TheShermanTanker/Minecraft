package net.minecraft.world.level.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;

public class DoubleBlockFinder {
    public static <S extends TileEntity> DoubleBlockFinder.Result<S> combineWithNeigbour(TileEntityTypes<S> blockEntityType, Function<IBlockData, DoubleBlockFinder.BlockType> typeMapper, Function<IBlockData, EnumDirection> function, BlockStateDirection directionProperty, IBlockData state, GeneratorAccess world, BlockPosition pos, BiPredicate<GeneratorAccess, BlockPosition> fallbackTester) {
        S blockEntity = blockEntityType.getBlockEntity(world, pos);
        if (blockEntity == null) {
            return DoubleBlockFinder.Combiner::acceptNone;
        } else if (fallbackTester.test(world, pos)) {
            return DoubleBlockFinder.Combiner::acceptNone;
        } else {
            DoubleBlockFinder.BlockType blockType = typeMapper.apply(state);
            boolean bl = blockType == DoubleBlockFinder.BlockType.SINGLE;
            boolean bl2 = blockType == DoubleBlockFinder.BlockType.FIRST;
            if (bl) {
                return new DoubleBlockFinder.Result.Single<>(blockEntity);
            } else {
                BlockPosition blockPos = pos.relative(function.apply(state));
                IBlockData blockState = world.getType(blockPos);
                if (blockState.is(state.getBlock())) {
                    DoubleBlockFinder.BlockType blockType2 = typeMapper.apply(blockState);
                    if (blockType2 != DoubleBlockFinder.BlockType.SINGLE && blockType != blockType2 && blockState.get(directionProperty) == state.get(directionProperty)) {
                        if (fallbackTester.test(world, blockPos)) {
                            return DoubleBlockFinder.Combiner::acceptNone;
                        }

                        S blockEntity2 = blockEntityType.getBlockEntity(world, blockPos);
                        if (blockEntity2 != null) {
                            S blockEntity3 = bl2 ? blockEntity : blockEntity2;
                            S blockEntity4 = bl2 ? blockEntity2 : blockEntity;
                            return new DoubleBlockFinder.Result.Double<>(blockEntity3, blockEntity4);
                        }
                    }
                }

                return new DoubleBlockFinder.Result.Single<>(blockEntity);
            }
        }
    }

    public static enum BlockType {
        SINGLE,
        FIRST,
        SECOND;
    }

    public interface Combiner<S, T> {
        T acceptDouble(S first, S second);

        T acceptSingle(S single);

        T acceptNone();
    }

    public interface Result<S> {
        <T> T apply(DoubleBlockFinder.Combiner<? super S, T> retriever);

        public static final class Double<S> implements DoubleBlockFinder.Result<S> {
            private final S first;
            private final S second;

            public Double(S first, S second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public <T> T apply(DoubleBlockFinder.Combiner<? super S, T> retriever) {
                return retriever.acceptDouble(this.first, this.second);
            }
        }

        public static final class Single<S> implements DoubleBlockFinder.Result<S> {
            private final S single;

            public Single(S single) {
                this.single = single;
            }

            @Override
            public <T> T apply(DoubleBlockFinder.Combiner<? super S, T> retriever) {
                return retriever.acceptSingle(this.single);
            }
        }
    }
}
