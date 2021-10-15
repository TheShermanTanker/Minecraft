package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockMobSpawner extends BlockTileEntity {
    protected BlockMobSpawner(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityMobSpawner(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.MOB_SPAWNER, world.isClientSide ? TileEntityMobSpawner::clientTick : TileEntityMobSpawner::serverTick);
    }

    @Override
    public void dropNaturally(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
        super.dropNaturally(state, world, pos, stack);
        int i = 15 + world.random.nextInt(15) + world.random.nextInt(15);
        this.dropExperience(world, pos, i);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
    }
}
