package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class BlockWeb extends Block {
    public BlockWeb(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        entity.makeStuckInBlock(state, new Vec3D(0.25D, (double)0.05F, 0.25D));
    }
}
