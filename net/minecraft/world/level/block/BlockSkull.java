package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockSkull extends BlockSkullAbstract {
    public static final int MAX = 15;
    private static final int ROTATIONS = 16;
    public static final BlockStateInteger ROTATION = BlockProperties.ROTATION_16;
    protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    protected BlockSkull(BlockSkull.IBlockSkullType type, BlockBase.Info settings) {
        super(type, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(ROTATION, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(ROTATION, Integer.valueOf(MathHelper.floor((double)(ctx.getRotation() * 16.0F / 360.0F) + 0.5D) & 15));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(ROTATION, Integer.valueOf(rotation.rotate(state.get(ROTATION), 16)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.set(ROTATION, Integer.valueOf(mirror.mirror(state.get(ROTATION), 16)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(ROTATION);
    }

    public interface IBlockSkullType {
    }

    public static enum Type implements BlockSkull.IBlockSkullType {
        SKELETON,
        WITHER_SKELETON,
        PLAYER,
        ZOMBIE,
        CREEPER,
        DRAGON;
    }
}
