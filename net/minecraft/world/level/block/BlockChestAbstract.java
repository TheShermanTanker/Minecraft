package net.minecraft.world.level.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class BlockChestAbstract<E extends TileEntity> extends BlockTileEntity {
    protected final Supplier<TileEntityTypes<? extends E>> blockEntityType;

    protected BlockChestAbstract(BlockBase.Info settings, Supplier<TileEntityTypes<? extends E>> entityTypeSupplier) {
        super(settings);
        this.blockEntityType = entityTypeSupplier;
    }

    public abstract DoubleBlockFinder.Result<? extends TileEntityChest> combine(IBlockData state, World world, BlockPosition pos, boolean ignoreBlocked);
}
