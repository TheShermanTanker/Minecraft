package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

public interface IBlockFragilePlantElement {
    boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient);

    boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state);

    void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state);
}
