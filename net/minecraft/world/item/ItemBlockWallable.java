package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class ItemBlockWallable extends ItemBlock {
    public final Block wallBlock;

    public ItemBlockWallable(Block standingBlock, Block wallBlock, Item.Info settings) {
        super(standingBlock, settings);
        this.wallBlock = wallBlock;
    }

    @Nullable
    @Override
    protected IBlockData getPlacementState(BlockActionContext context) {
        IBlockData blockState = this.wallBlock.getPlacedState(context);
        IBlockData blockState2 = null;
        IWorldReader levelReader = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();

        for(EnumDirection direction : context.getNearestLookingDirections()) {
            if (direction != EnumDirection.UP) {
                IBlockData blockState3 = direction == EnumDirection.DOWN ? this.getBlock().getPlacedState(context) : blockState;
                if (blockState3 != null && blockState3.canPlace(levelReader, blockPos)) {
                    blockState2 = blockState3;
                    break;
                }
            }
        }

        return blockState2 != null && levelReader.isUnobstructed(blockState2, blockPos, VoxelShapeCollision.empty()) ? blockState2 : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(this.wallBlock, item);
    }
}
