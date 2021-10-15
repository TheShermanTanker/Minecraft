package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockNetherrack extends Block implements IBlockFragilePlantElement {
    public BlockNetherrack(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        if (!world.getType(pos.above()).propagatesSkylightDown(world, pos)) {
            return false;
        } else {
            for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (world.getType(blockPos).is(TagsBlock.NYLIUM)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        boolean bl = false;
        boolean bl2 = false;

        for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            IBlockData blockState = world.getType(blockPos);
            if (blockState.is(Blocks.WARPED_NYLIUM)) {
                bl2 = true;
            }

            if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
                bl = true;
            }

            if (bl2 && bl) {
                break;
            }
        }

        if (bl2 && bl) {
            world.setTypeAndData(pos, random.nextBoolean() ? Blocks.WARPED_NYLIUM.getBlockData() : Blocks.CRIMSON_NYLIUM.getBlockData(), 3);
        } else if (bl2) {
            world.setTypeAndData(pos, Blocks.WARPED_NYLIUM.getBlockData(), 3);
        } else if (bl) {
            world.setTypeAndData(pos, Blocks.CRIMSON_NYLIUM.getBlockData(), 3);
        }

    }
}
