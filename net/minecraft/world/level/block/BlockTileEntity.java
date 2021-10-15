package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class BlockTileEntity extends Block implements ITileEntity {
    protected BlockTileEntity(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.INVISIBLE;
    }

    @Override
    public boolean triggerEvent(IBlockData state, World world, BlockPosition pos, int type, int data) {
        super.triggerEvent(state, world, pos, type, data);
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity == null ? false : blockEntity.setProperty(type, data);
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof ITileInventory ? (ITileInventory)blockEntity : null;
    }

    @Nullable
    protected static <E extends TileEntity, A extends TileEntity> BlockEntityTicker<A> createTickerHelper(TileEntityTypes<A> givenType, TileEntityTypes<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? ticker : null;
    }
}
