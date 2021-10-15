package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockWitherSkullWall extends BlockSkullWall {
    protected BlockWitherSkullWall(BlockBase.Info settings) {
        super(BlockSkull.Type.WITHER_SKELETON, settings);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        Blocks.WITHER_SKELETON_SKULL.postPlace(world, pos, state, placer, itemStack);
    }
}
