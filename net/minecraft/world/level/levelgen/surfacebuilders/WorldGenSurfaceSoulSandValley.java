package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenSurfaceSoulSandValley extends WorldGenSurfaceNetherAbstract {
    private static final IBlockData SOUL_SAND = Blocks.SOUL_SAND.getBlockData();
    private static final IBlockData SOUL_SOIL = Blocks.SOUL_SOIL.getBlockData();
    private static final IBlockData GRAVEL = Blocks.GRAVEL.getBlockData();
    private static final ImmutableList<IBlockData> BLOCK_STATES = ImmutableList.of(SOUL_SAND, SOUL_SOIL);

    public WorldGenSurfaceSoulSandValley(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    protected ImmutableList<IBlockData> getFloorBlockStates() {
        return BLOCK_STATES;
    }

    @Override
    protected ImmutableList<IBlockData> getCeilingBlockStates() {
        return BLOCK_STATES;
    }

    @Override
    protected IBlockData getPatchBlockState() {
        return GRAVEL;
    }
}
