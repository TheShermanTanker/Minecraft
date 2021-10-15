package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDeadBush extends BlockPlant {
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    protected BlockDeadBush(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(Blocks.SAND) || floor.is(Blocks.RED_SAND) || floor.is(Blocks.TERRACOTTA) || floor.is(Blocks.WHITE_TERRACOTTA) || floor.is(Blocks.ORANGE_TERRACOTTA) || floor.is(Blocks.MAGENTA_TERRACOTTA) || floor.is(Blocks.LIGHT_BLUE_TERRACOTTA) || floor.is(Blocks.YELLOW_TERRACOTTA) || floor.is(Blocks.LIME_TERRACOTTA) || floor.is(Blocks.PINK_TERRACOTTA) || floor.is(Blocks.GRAY_TERRACOTTA) || floor.is(Blocks.LIGHT_GRAY_TERRACOTTA) || floor.is(Blocks.CYAN_TERRACOTTA) || floor.is(Blocks.PURPLE_TERRACOTTA) || floor.is(Blocks.BLUE_TERRACOTTA) || floor.is(Blocks.BROWN_TERRACOTTA) || floor.is(Blocks.GREEN_TERRACOTTA) || floor.is(Blocks.RED_TERRACOTTA) || floor.is(Blocks.BLACK_TERRACOTTA) || floor.is(TagsBlock.DIRT);
    }
}
