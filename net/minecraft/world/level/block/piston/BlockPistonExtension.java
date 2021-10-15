package net.minecraft.world.level.block.piston;

import java.util.Arrays;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockDirectional;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyPistonType;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockPistonExtension extends BlockDirectional {
    public static final BlockStateEnum<BlockPropertyPistonType> TYPE = BlockProperties.PISTON_TYPE;
    public static final BlockStateBoolean SHORT = BlockProperties.SHORT;
    public static final float PLATFORM = 4.0F;
    protected static final VoxelShape EAST_AABB = Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    protected static final float AABB_OFFSET = 2.0F;
    protected static final float EDGE_MIN = 6.0F;
    protected static final float EDGE_MAX = 10.0F;
    protected static final VoxelShape UP_ARM_AABB = Block.box(6.0D, -4.0D, 6.0D, 10.0D, 12.0D, 10.0D);
    protected static final VoxelShape DOWN_ARM_AABB = Block.box(6.0D, 4.0D, 6.0D, 10.0D, 20.0D, 10.0D);
    protected static final VoxelShape SOUTH_ARM_AABB = Block.box(6.0D, 6.0D, -4.0D, 10.0D, 10.0D, 12.0D);
    protected static final VoxelShape NORTH_ARM_AABB = Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 20.0D);
    protected static final VoxelShape EAST_ARM_AABB = Block.box(-4.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D);
    protected static final VoxelShape WEST_ARM_AABB = Block.box(4.0D, 6.0D, 6.0D, 20.0D, 10.0D, 10.0D);
    protected static final VoxelShape SHORT_UP_ARM_AABB = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 12.0D, 10.0D);
    protected static final VoxelShape SHORT_DOWN_ARM_AABB = Block.box(6.0D, 4.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    protected static final VoxelShape SHORT_SOUTH_ARM_AABB = Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 12.0D);
    protected static final VoxelShape SHORT_NORTH_ARM_AABB = Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 16.0D);
    protected static final VoxelShape SHORT_EAST_ARM_AABB = Block.box(0.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D);
    protected static final VoxelShape SHORT_WEST_ARM_AABB = Block.box(4.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
    private static final VoxelShape[] SHAPES_SHORT = makeShapes(true);
    private static final VoxelShape[] SHAPES_LONG = makeShapes(false);

    private static VoxelShape[] makeShapes(boolean shortHead) {
        return Arrays.stream(EnumDirection.values()).map((direction) -> {
            return calculateShape(direction, shortHead);
        }).toArray((i) -> {
            return new VoxelShape[i];
        });
    }

    private static VoxelShape calculateShape(EnumDirection direction, boolean shortHead) {
        switch(direction) {
        case DOWN:
        default:
            return VoxelShapes.or(DOWN_AABB, shortHead ? SHORT_DOWN_ARM_AABB : DOWN_ARM_AABB);
        case UP:
            return VoxelShapes.or(UP_AABB, shortHead ? SHORT_UP_ARM_AABB : UP_ARM_AABB);
        case NORTH:
            return VoxelShapes.or(NORTH_AABB, shortHead ? SHORT_NORTH_ARM_AABB : NORTH_ARM_AABB);
        case SOUTH:
            return VoxelShapes.or(SOUTH_AABB, shortHead ? SHORT_SOUTH_ARM_AABB : SOUTH_ARM_AABB);
        case WEST:
            return VoxelShapes.or(WEST_AABB, shortHead ? SHORT_WEST_ARM_AABB : WEST_ARM_AABB);
        case EAST:
            return VoxelShapes.or(EAST_AABB, shortHead ? SHORT_EAST_ARM_AABB : EAST_ARM_AABB);
        }
    }

    public BlockPistonExtension(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(TYPE, BlockPropertyPistonType.DEFAULT).set(SHORT, Boolean.valueOf(false)));
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return (state.get(SHORT) ? SHAPES_SHORT : SHAPES_LONG)[state.get(FACING).ordinal()];
    }

    private boolean isFittingBase(IBlockData headState, IBlockData pistonState) {
        Block block = headState.get(TYPE) == BlockPropertyPistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
        return pistonState.is(block) && pistonState.get(BlockPiston.EXTENDED) && pistonState.get(FACING) == headState.get(FACING);
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide && player.getAbilities().instabuild) {
            BlockPosition blockPos = pos.relative(state.get(FACING).opposite());
            if (this.isFittingBase(state, world.getType(blockPos))) {
                world.destroyBlock(blockPos, false);
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            super.remove(state, world, pos, newState, moved);
            BlockPosition blockPos = pos.relative(state.get(FACING).opposite());
            if (this.isFittingBase(state, world.getType(blockPos))) {
                world.destroyBlock(blockPos, true);
            }

        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction.opposite() == state.get(FACING) && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.relative(state.get(FACING).opposite()));
        return this.isFittingBase(state, blockState) || blockState.is(Blocks.MOVING_PISTON) && blockState.get(FACING) == state.get(FACING);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (state.canPlace(world, pos)) {
            BlockPosition blockPos = pos.relative(state.get(FACING).opposite());
            world.getType(blockPos).doPhysics(world, blockPos, block, fromPos, false);
        }

    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(state.get(TYPE) == BlockPropertyPistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, TYPE, SHORT);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
