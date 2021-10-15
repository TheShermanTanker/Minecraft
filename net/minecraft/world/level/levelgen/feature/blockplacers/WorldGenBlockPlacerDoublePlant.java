package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockTallPlant;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenBlockPlacerDoublePlant extends WorldGenBlockPlacer {
    public static final Codec<WorldGenBlockPlacerDoublePlant> CODEC;
    public static final WorldGenBlockPlacerDoublePlant INSTANCE = new WorldGenBlockPlacerDoublePlant();

    @Override
    protected WorldGenBlockPlacers<?> type() {
        return WorldGenBlockPlacers.DOUBLE_PLANT_PLACER;
    }

    @Override
    public void place(GeneratorAccess world, BlockPosition pos, IBlockData state, Random random) {
        BlockTallPlant.placeAt(world, state, pos, 2);
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
