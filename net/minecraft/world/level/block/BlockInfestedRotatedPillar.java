package net.minecraft.world.level.block;

import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockInfestedRotatedPillar extends BlockMonsterEggs {
    public BlockInfestedRotatedPillar(Block regularBlock, BlockBase.Info settings) {
        super(regularBlock, settings);
        this.registerDefaultState(this.getBlockData().set(BlockRotatable.AXIS, EnumDirection.EnumAxis.Y));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return BlockRotatable.rotatePillar(state, rotation);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(BlockRotatable.AXIS);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(BlockRotatable.AXIS, ctx.getClickedFace().getAxis());
    }
}
