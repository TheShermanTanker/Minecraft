package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyWood;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;

public class BlockFloorSign extends BlockSign {
    public static final BlockStateInteger ROTATION = BlockProperties.ROTATION_16;

    public BlockFloorSign(BlockBase.Info settings, BlockPropertyWood type) {
        super(settings, type);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(ROTATION, Integer.valueOf(0)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.below()).getMaterial().isBuildable();
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return this.getBlockData().set(ROTATION, Integer.valueOf(MathHelper.floor((double)((180.0F + ctx.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15)).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN && !this.canPlace(state, world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(ROTATION, Integer.valueOf(rotation.rotate(state.get(ROTATION), 16)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.set(ROTATION, Integer.valueOf(mirror.mirror(state.get(ROTATION), 16)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(ROTATION, WATERLOGGED);
    }
}
