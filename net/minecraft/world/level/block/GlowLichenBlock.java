package net.minecraft.world.level.block;

import java.util.Random;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;

public class GlowLichenBlock extends BlockMultiface implements IBlockFragilePlantElement, IBlockWaterlogged {
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;

    public GlowLichenBlock(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)));
    }

    public static ToIntFunction<IBlockData> emission(int luminance) {
        return (state) -> {
            return BlockMultiface.hasAnyFace(state) ? luminance : 0;
        };
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return !context.getItemStack().is(Items.GLOW_LICHEN) || super.canBeReplaced(state, context);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return Stream.of(DIRECTIONS).anyMatch((direction) -> {
            return this.canSpread(state, world, pos, direction.opposite());
        });
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.spreadFromRandomFaceTowardRandomDirection(state, world, pos, random);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.getFluid().isEmpty();
    }
}
