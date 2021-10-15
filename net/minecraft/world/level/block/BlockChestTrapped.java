package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityChestTrapped;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockChestTrapped extends BlockChest {
    public BlockChestTrapped(BlockBase.Info settings) {
        super(settings, () -> {
            return TileEntityTypes.TRAPPED_CHEST;
        });
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityChestTrapped(pos, state);
    }

    @Override
    protected Statistic<MinecraftKey> getOpenChestStat() {
        return StatisticList.CUSTOM.get(StatisticList.TRIGGER_TRAPPED_CHEST);
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return MathHelper.clamp(TileEntityChest.getOpenCount(world, pos), 0, 15);
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return direction == EnumDirection.UP ? state.getSignal(world, pos, direction) : 0;
    }
}
