package net.minecraft.world.level.levelgen.surfacebuilders;

import net.minecraft.world.level.block.state.IBlockData;

public interface WorldGenSurfaceConfiguration {
    IBlockData getTopMaterial();

    IBlockData getUnderMaterial();

    IBlockData getUnderwaterMaterial();
}
