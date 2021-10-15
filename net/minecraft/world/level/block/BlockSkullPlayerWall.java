package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class BlockSkullPlayerWall extends BlockSkullWall {
    protected BlockSkullPlayerWall(BlockBase.Info settings) {
        super(BlockSkull.Type.PLAYER, settings);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        Blocks.PLAYER_HEAD.postPlace(world, pos, state, placer, itemStack);
    }

    @Override
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        return Blocks.PLAYER_HEAD.getDrops(state, builder);
    }
}
