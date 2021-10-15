package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.item.ItemWearable;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;

public abstract class BlockSkullAbstract extends BlockTileEntity implements ItemWearable {
    private final BlockSkull.IBlockSkullType type;

    public BlockSkullAbstract(BlockSkull.IBlockSkullType type, BlockBase.Info settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntitySkull(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return !world.isClientSide || !state.is(Blocks.DRAGON_HEAD) && !state.is(Blocks.DRAGON_WALL_HEAD) ? null : createTickerHelper(type, TileEntityTypes.SKULL, TileEntitySkull::dragonHeadAnimation);
    }

    public BlockSkull.IBlockSkullType getType() {
        return this.type;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
