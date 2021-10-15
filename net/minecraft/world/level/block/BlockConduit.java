package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBeacon;
import net.minecraft.world.level.block.entity.TileEntityConduit;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockConduit extends BlockTileEntity implements IBlockWaterlogged {
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final int SIZE = 3;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);

    public BlockConduit(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(WATERLOGGED, Boolean.valueOf(true)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityConduit(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.CONDUIT, world.isClientSide ? TileEntityConduit::clientTick : TileEntityConduit::serverTick);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBeacon) {
                ((TileEntityBeacon)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8));
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
