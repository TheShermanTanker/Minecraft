package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenBlockPlacerSimple extends WorldGenBlockPlacer {
    public static final Codec<WorldGenBlockPlacerSimple> CODEC;
    public static final WorldGenBlockPlacerSimple INSTANCE = new WorldGenBlockPlacerSimple();

    @Override
    protected WorldGenBlockPlacers<?> type() {
        return WorldGenBlockPlacers.SIMPLE_BLOCK_PLACER;
    }

    @Override
    public void place(GeneratorAccess world, BlockPosition pos, IBlockData state, Random random) {
        world.setTypeAndData(pos, state, 2);
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
