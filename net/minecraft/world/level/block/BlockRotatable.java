package net.minecraft.world.level.block;

import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;

public class BlockRotatable extends Block {
    public static final BlockStateEnum<EnumDirection.EnumAxis> AXIS = BlockProperties.AXIS;

    public BlockRotatable(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(AXIS, EnumDirection.EnumAxis.Y));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return rotatePillar(state, rotation);
    }

    public static IBlockData rotatePillar(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
        case CLOCKWISE_90:
            switch((EnumDirection.EnumAxis)state.get(AXIS)) {
            case X:
                return state.set(AXIS, EnumDirection.EnumAxis.Z);
            case Z:
                return state.set(AXIS, EnumDirection.EnumAxis.X);
            default:
                return state;
            }
        default:
            return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AXIS);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(AXIS, ctx.getClickedFace().getAxis());
    }
}
