package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;

public enum BlockAccessAir implements IBlockAccess {
    INSTANCE;

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return null;
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        return Blocks.AIR.getBlockData();
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        return FluidTypes.EMPTY.defaultFluidState();
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
