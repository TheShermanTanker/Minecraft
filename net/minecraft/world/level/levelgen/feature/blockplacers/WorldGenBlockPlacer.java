package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class WorldGenBlockPlacer {
    public static final Codec<WorldGenBlockPlacer> CODEC = IRegistry.BLOCK_PLACER_TYPES.dispatch(WorldGenBlockPlacer::type, WorldGenBlockPlacers::codec);

    public abstract void place(GeneratorAccess world, BlockPosition pos, IBlockData state, Random random);

    protected abstract WorldGenBlockPlacers<?> type();
}
