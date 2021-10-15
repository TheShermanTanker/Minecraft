package net.minecraft.world;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;

public interface IInventoryHolder {
    IWorldInventory getContainer(IBlockData state, GeneratorAccess world, BlockPosition pos);
}
