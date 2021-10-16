package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoorHinge;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDoor extends Block {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateEnum<BlockPropertyDoorHinge> HINGE = BlockProperties.DOOR_HINGE;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateEnum<BlockPropertyDoubleBlockHalf> HALF = BlockProperties.DOUBLE_BLOCK_HALF;
    protected static final float AABB_DOOR_THICKNESS = 3.0F;
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    protected BlockDoor(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(OPEN, Boolean.valueOf(false)).set(HINGE, BlockPropertyDoorHinge.LEFT).set(POWERED, Boolean.valueOf(false)).set(HALF, BlockPropertyDoubleBlockHalf.LOWER));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        EnumDirection direction = state.get(FACING);
        boolean bl = !state.get(OPEN);
        boolean bl2 = state.get(HINGE) == BlockPropertyDoorHinge.RIGHT;
        switch(direction) {
        case EAST:
        default:
            return bl ? EAST_AABB : (bl2 ? NORTH_AABB : SOUTH_AABB);
        case SOUTH:
            return bl ? SOUTH_AABB : (bl2 ? EAST_AABB : WEST_AABB);
        case WEST:
            return bl ? WEST_AABB : (bl2 ? SOUTH_AABB : NORTH_AABB);
        case NORTH:
            return bl ? NORTH_AABB : (bl2 ? WEST_AABB : EAST_AABB);
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        BlockPropertyDoubleBlockHalf doubleBlockHalf = state.get(HALF);
        if (direction.getAxis() == EnumDirection.EnumAxis.Y && doubleBlockHalf == BlockPropertyDoubleBlockHalf.LOWER == (direction == EnumDirection.UP)) {
            return neighborState.is(this) && neighborState.get(HALF) != doubleBlockHalf ? state.set(FACING, neighborState.get(FACING)).set(OPEN, neighborState.get(OPEN)).set(HINGE, neighborState.get(HINGE)).set(POWERED, neighborState.get(POWERED)) : Blocks.AIR.getBlockData();
        } else {
            return doubleBlockHalf == BlockPropertyDoubleBlockHalf.LOWER && direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide && player.isCreative()) {
            BlockTallPlant.preventCreativeDropFromBottomPart(world, pos, state, player);
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return state.get(OPEN);
        case WATER:
            return false;
        case AIR:
            return state.get(OPEN);
        default:
            return false;
        }
    }

    private int getCloseSound() {
        return this.material == Material.METAL ? 1011 : 1012;
    }

    private int getOpenSound() {
        return this.material == Material.METAL ? 1005 : 1006;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPosition blockPos = ctx.getClickPosition();
        World level = ctx.getWorld();
        if (blockPos.getY() < level.getMaxBuildHeight() - 1 && level.getType(blockPos.above()).canBeReplaced(ctx)) {
            boolean bl = level.isBlockIndirectlyPowered(blockPos) || level.isBlockIndirectlyPowered(blockPos.above());
            return this.getBlockData().set(FACING, ctx.getHorizontalDirection()).set(HINGE, this.getHinge(ctx)).set(POWERED, Boolean.valueOf(bl)).set(OPEN, Boolean.valueOf(bl)).set(HALF, BlockPropertyDoubleBlockHalf.LOWER);
        } else {
            return null;
        }
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        world.setTypeAndData(pos.above(), state.set(HALF, BlockPropertyDoubleBlockHalf.UPPER), 3);
    }

    private BlockPropertyDoorHinge getHinge(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection direction = ctx.getHorizontalDirection();
        BlockPosition blockPos2 = blockPos.above();
        EnumDirection direction2 = direction.getCounterClockWise();
        BlockPosition blockPos3 = blockPos.relative(direction2);
        IBlockData blockState = blockGetter.getType(blockPos3);
        BlockPosition blockPos4 = blockPos2.relative(direction2);
        IBlockData blockState2 = blockGetter.getType(blockPos4);
        EnumDirection direction3 = direction.getClockWise();
        BlockPosition blockPos5 = blockPos.relative(direction3);
        IBlockData blockState3 = blockGetter.getType(blockPos5);
        BlockPosition blockPos6 = blockPos2.relative(direction3);
        IBlockData blockState4 = blockGetter.getType(blockPos6);
        int i = (blockState.isCollisionShapeFullBlock(blockGetter, blockPos3) ? -1 : 0) + (blockState2.isCollisionShapeFullBlock(blockGetter, blockPos4) ? -1 : 0) + (blockState3.isCollisionShapeFullBlock(blockGetter, blockPos5) ? 1 : 0) + (blockState4.isCollisionShapeFullBlock(blockGetter, blockPos6) ? 1 : 0);
        boolean bl = blockState.is(this) && blockState.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER;
        boolean bl2 = blockState3.is(this) && blockState3.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER;
        if ((!bl || bl2) && i <= 0) {
            if ((!bl2 || bl) && i >= 0) {
                int j = direction.getAdjacentX();
                int k = direction.getAdjacentZ();
                Vec3D vec3 = ctx.getPos();
                double d = vec3.x - (double)blockPos.getX();
                double e = vec3.z - (double)blockPos.getZ();
                return (j >= 0 || !(e < 0.5D)) && (j <= 0 || !(e > 0.5D)) && (k >= 0 || !(d > 0.5D)) && (k <= 0 || !(d < 0.5D)) ? BlockPropertyDoorHinge.LEFT : BlockPropertyDoorHinge.RIGHT;
            } else {
                return BlockPropertyDoorHinge.LEFT;
            }
        } else {
            return BlockPropertyDoorHinge.RIGHT;
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (this.material == Material.METAL) {
            return EnumInteractionResult.PASS;
        } else {
            state = state.cycle(OPEN);
            world.setTypeAndData(pos, state, 10);
            world.triggerEffect(player, state.get(OPEN) ? this.getOpenSound() : this.getCloseSound(), pos, 0);
            world.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    public boolean isOpen(IBlockData state) {
        return state.get(OPEN);
    }

    public void setDoor(@Nullable Entity entity, World world, IBlockData state, BlockPosition pos, boolean open) {
        if (state.is(this) && state.get(OPEN) != open) {
            world.setTypeAndData(pos, state.set(OPEN, Boolean.valueOf(open)), 10);
            this.playSound(world, pos, open);
            world.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        boolean bl = world.isBlockIndirectlyPowered(pos) || world.isBlockIndirectlyPowered(pos.relative(state.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER ? EnumDirection.UP : EnumDirection.DOWN));
        if (!this.getBlockData().is(block) && bl != state.get(POWERED)) {
            if (bl != state.get(OPEN)) {
                this.playSound(world, pos, bl);
                world.gameEvent(bl ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }

            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)).set(OPEN, Boolean.valueOf(bl)), 2);
        }

    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        return state.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER ? blockState.isFaceSturdy(world, blockPos, EnumDirection.UP) : blockState.is(this);
    }

    private void playSound(World world, BlockPosition pos, boolean open) {
        world.triggerEffect((EntityHuman)null, open ? this.getOpenSound() : this.getCloseSound(), pos, 0);
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return mirror == EnumBlockMirror.NONE ? state : state.rotate(mirror.getRotation(state.get(FACING))).cycle(HINGE);
    }

    @Override
    public long getSeed(IBlockData state, BlockPosition pos) {
        return MathHelper.getSeed(pos.getX(), pos.below(state.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HALF, FACING, OPEN, HINGE, POWERED);
    }

    public static boolean isWoodenDoor(World world, BlockPosition pos) {
        return isWoodenDoor(world.getType(pos));
    }

    public static boolean isWoodenDoor(IBlockData state) {
        return state.getBlock() instanceof BlockDoor && (state.getMaterial() == Material.WOOD || state.getMaterial() == Material.NETHER_WOOD);
    }
}
