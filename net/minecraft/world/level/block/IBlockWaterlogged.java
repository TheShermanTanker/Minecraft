package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public interface IBlockWaterlogged extends IFluidSource, IFluidContainer {
    @Override
    default boolean canPlace(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) {
        return !state.get(BlockProperties.WATERLOGGED) && fluid == FluidTypes.WATER;
    }

    @Override
    default boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        if (!state.get(BlockProperties.WATERLOGGED) && fluidState.getType() == FluidTypes.WATER) {
            if (!world.isClientSide()) {
                world.setTypeAndData(pos, state.set(BlockProperties.WATERLOGGED, Boolean.valueOf(true)), 3);
                world.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(world));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    default ItemStack removeFluid(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        if (state.get(BlockProperties.WATERLOGGED)) {
            world.setTypeAndData(pos, state.set(BlockProperties.WATERLOGGED, Boolean.valueOf(false)), 3);
            if (!state.canPlace(world, pos)) {
                world.destroyBlock(pos, true);
            }

            return new ItemStack(Items.WATER_BUCKET);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    default Optional<SoundEffect> getPickupSound() {
        return FluidTypes.WATER.getPickupSound();
    }
}
