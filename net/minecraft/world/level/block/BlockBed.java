package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.DismountUtil;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.ICollisionAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBed;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyBedPart;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.ArrayUtils;

public class BlockBed extends BlockFacingHorizontal implements ITileEntity {
    public static final BlockStateEnum<BlockPropertyBedPart> PART = BlockProperties.BED_PART;
    public static final BlockStateBoolean OCCUPIED = BlockProperties.OCCUPIED;
    protected static final int HEIGHT = 9;
    protected static final VoxelShape BASE = Block.box(0.0D, 3.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    private static final int LEG_WIDTH = 3;
    protected static final VoxelShape LEG_NORTH_WEST = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 3.0D, 3.0D);
    protected static final VoxelShape LEG_SOUTH_WEST = Block.box(0.0D, 0.0D, 13.0D, 3.0D, 3.0D, 16.0D);
    protected static final VoxelShape LEG_NORTH_EAST = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 3.0D, 3.0D);
    protected static final VoxelShape LEG_SOUTH_EAST = Block.box(13.0D, 0.0D, 13.0D, 16.0D, 3.0D, 16.0D);
    protected static final VoxelShape NORTH_SHAPE = VoxelShapes.or(BASE, LEG_NORTH_WEST, LEG_NORTH_EAST);
    protected static final VoxelShape SOUTH_SHAPE = VoxelShapes.or(BASE, LEG_SOUTH_WEST, LEG_SOUTH_EAST);
    protected static final VoxelShape WEST_SHAPE = VoxelShapes.or(BASE, LEG_NORTH_WEST, LEG_SOUTH_WEST);
    protected static final VoxelShape EAST_SHAPE = VoxelShapes.or(BASE, LEG_NORTH_EAST, LEG_SOUTH_EAST);
    private final EnumColor color;

    public BlockBed(EnumColor color, BlockBase.Info settings) {
        super(settings);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(PART, BlockPropertyBedPart.FOOT).set(OCCUPIED, Boolean.valueOf(false)));
    }

    @Nullable
    public static EnumDirection getBedOrientation(IBlockAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return blockState.getBlock() instanceof BlockBed ? blockState.get(FACING) : null;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.CONSUME;
        } else {
            if (state.get(PART) != BlockPropertyBedPart.HEAD) {
                pos = pos.relative(state.get(FACING));
                state = world.getType(pos);
                if (!state.is(this)) {
                    return EnumInteractionResult.CONSUME;
                }
            }

            if (!canSetSpawn(world)) {
                world.removeBlock(pos, false);
                BlockPosition blockPos = pos.relative(state.get(FACING).opposite());
                if (world.getType(blockPos).is(this)) {
                    world.removeBlock(blockPos, false);
                }

                world.createExplosion((Entity)null, DamageSource.badRespawnPointExplosion(), (ExplosionDamageCalculator)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 5.0F, true, Explosion.Effect.DESTROY);
                return EnumInteractionResult.SUCCESS;
            } else if (state.get(OCCUPIED)) {
                if (!this.kickVillagerOutOfBed(world, pos)) {
                    player.displayClientMessage(new ChatMessage("block.minecraft.bed.occupied"), true);
                }

                return EnumInteractionResult.SUCCESS;
            } else {
                player.sleep(pos).ifLeft((reason) -> {
                    if (reason != null) {
                        player.displayClientMessage(reason.getMessage(), true);
                    }

                });
                return EnumInteractionResult.SUCCESS;
            }
        }
    }

    public static boolean canSetSpawn(World world) {
        return world.getDimensionManager().isBedWorks();
    }

    private boolean kickVillagerOutOfBed(World world, BlockPosition pos) {
        List<EntityVillager> list = world.getEntitiesOfClass(EntityVillager.class, new AxisAlignedBB(pos), EntityLiving::isSleeping);
        if (list.isEmpty()) {
            return false;
        } else {
            list.get(0).entityWakeup();
            return true;
        }
    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        super.fallOn(world, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityAfterFallOn(IBlockAccess world, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(world, entity);
        } else {
            this.bounceUp(entity);
        }

    }

    private void bounceUp(Entity entity) {
        Vec3D vec3 = entity.getMot();
        if (vec3.y < 0.0D) {
            double d = entity instanceof EntityLiving ? 1.0D : 0.8D;
            entity.setMot(vec3.x, -vec3.y * (double)0.66F * d, vec3.z);
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == getNeighbourDirection(state.get(PART), state.get(FACING))) {
            return neighborState.is(this) && neighborState.get(PART) != state.get(PART) ? state.set(OCCUPIED, neighborState.get(OCCUPIED)) : Blocks.AIR.getBlockData();
        } else {
            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    private static EnumDirection getNeighbourDirection(BlockPropertyBedPart part, EnumDirection direction) {
        return part == BlockPropertyBedPart.FOOT ? direction : direction.opposite();
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide && player.isCreative()) {
            BlockPropertyBedPart bedPart = state.get(PART);
            if (bedPart == BlockPropertyBedPart.FOOT) {
                BlockPosition blockPos = pos.relative(getNeighbourDirection(bedPart, state.get(FACING)));
                IBlockData blockState = world.getType(blockPos);
                if (blockState.is(this) && blockState.get(PART) == BlockPropertyBedPart.HEAD) {
                    world.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 35);
                    world.triggerEffect(player, 2001, blockPos, Block.getCombinedId(blockState));
                }
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getHorizontalDirection();
        BlockPosition blockPos = ctx.getClickPosition();
        BlockPosition blockPos2 = blockPos.relative(direction);
        return ctx.getWorld().getType(blockPos2).canBeReplaced(ctx) ? this.getBlockData().set(FACING, direction) : null;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        EnumDirection direction = getConnectedDirection(state).opposite();
        switch(direction) {
        case NORTH:
            return NORTH_SHAPE;
        case SOUTH:
            return SOUTH_SHAPE;
        case WEST:
            return WEST_SHAPE;
        default:
            return EAST_SHAPE;
        }
    }

    public static EnumDirection getConnectedDirection(IBlockData state) {
        EnumDirection direction = state.get(FACING);
        return state.get(PART) == BlockPropertyBedPart.HEAD ? direction.opposite() : direction;
    }

    public static DoubleBlockFinder.BlockType getBlockType(IBlockData state) {
        BlockPropertyBedPart bedPart = state.get(PART);
        return bedPart == BlockPropertyBedPart.HEAD ? DoubleBlockFinder.BlockType.FIRST : DoubleBlockFinder.BlockType.SECOND;
    }

    private static boolean isBunkBed(IBlockAccess world, BlockPosition pos) {
        return world.getType(pos.below()).getBlock() instanceof BlockBed;
    }

    public static Optional<Vec3D> findStandUpPosition(EntityTypes<?> type, ICollisionAccess world, BlockPosition pos, float f) {
        EnumDirection direction = world.getType(pos).get(FACING);
        EnumDirection direction2 = direction.getClockWise();
        EnumDirection direction3 = direction2.isFacingAngle(f) ? direction2.opposite() : direction2;
        if (isBunkBed(world, pos)) {
            return findBunkBedStandUpPosition(type, world, pos, direction, direction3);
        } else {
            int[][] is = bedStandUpOffsets(direction, direction3);
            Optional<Vec3D> optional = findStandUpPositionAtOffset(type, world, pos, is, true);
            return optional.isPresent() ? optional : findStandUpPositionAtOffset(type, world, pos, is, false);
        }
    }

    private static Optional<Vec3D> findBunkBedStandUpPosition(EntityTypes<?> type, ICollisionAccess world, BlockPosition pos, EnumDirection direction, EnumDirection direction2) {
        int[][] is = bedSurroundStandUpOffsets(direction, direction2);
        Optional<Vec3D> optional = findStandUpPositionAtOffset(type, world, pos, is, true);
        if (optional.isPresent()) {
            return optional;
        } else {
            BlockPosition blockPos = pos.below();
            Optional<Vec3D> optional2 = findStandUpPositionAtOffset(type, world, blockPos, is, true);
            if (optional2.isPresent()) {
                return optional2;
            } else {
                int[][] js = bedAboveStandUpOffsets(direction);
                Optional<Vec3D> optional3 = findStandUpPositionAtOffset(type, world, pos, js, true);
                if (optional3.isPresent()) {
                    return optional3;
                } else {
                    Optional<Vec3D> optional4 = findStandUpPositionAtOffset(type, world, pos, is, false);
                    if (optional4.isPresent()) {
                        return optional4;
                    } else {
                        Optional<Vec3D> optional5 = findStandUpPositionAtOffset(type, world, blockPos, is, false);
                        return optional5.isPresent() ? optional5 : findStandUpPositionAtOffset(type, world, pos, js, false);
                    }
                }
            }
        }
    }

    private static Optional<Vec3D> findStandUpPositionAtOffset(EntityTypes<?> type, ICollisionAccess world, BlockPosition pos, int[][] is, boolean bl) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int[] js : is) {
            mutableBlockPos.set(pos.getX() + js[0], pos.getY(), pos.getZ() + js[1]);
            Vec3D vec3 = DismountUtil.findSafeDismountLocation(type, world, mutableBlockPos, bl);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBed(pos, state, this.color);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        super.postPlace(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            BlockPosition blockPos = pos.relative(state.get(FACING));
            world.setTypeAndData(blockPos, state.set(PART, BlockPropertyBedPart.HEAD), 3);
            world.update(pos, Blocks.AIR);
            state.updateNeighbourShapes(world, pos, 3);
        }

    }

    public EnumColor getColor() {
        return this.color;
    }

    @Override
    public long getSeed(IBlockData state, BlockPosition pos) {
        BlockPosition blockPos = pos.relative(state.get(FACING), state.get(PART) == BlockPropertyBedPart.HEAD ? 0 : 1);
        return MathHelper.getSeed(blockPos.getX(), pos.getY(), blockPos.getZ());
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    private static int[][] bedStandUpOffsets(EnumDirection direction, EnumDirection direction2) {
        return ArrayUtils.addAll((int[][])bedSurroundStandUpOffsets(direction, direction2), (int[][])bedAboveStandUpOffsets(direction));
    }

    private static int[][] bedSurroundStandUpOffsets(EnumDirection direction, EnumDirection direction2) {
        return new int[][]{{direction2.getAdjacentX(), direction2.getAdjacentZ()}, {direction2.getAdjacentX() - direction.getAdjacentX(), direction2.getAdjacentZ() - direction.getAdjacentZ()}, {direction2.getAdjacentX() - direction.getAdjacentX() * 2, direction2.getAdjacentZ() - direction.getAdjacentZ() * 2}, {-direction.getAdjacentX() * 2, -direction.getAdjacentZ() * 2}, {-direction2.getAdjacentX() - direction.getAdjacentX() * 2, -direction2.getAdjacentZ() - direction.getAdjacentZ() * 2}, {-direction2.getAdjacentX() - direction.getAdjacentX(), -direction2.getAdjacentZ() - direction.getAdjacentZ()}, {-direction2.getAdjacentX(), -direction2.getAdjacentZ()}, {-direction2.getAdjacentX() + direction.getAdjacentX(), -direction2.getAdjacentZ() + direction.getAdjacentZ()}, {direction.getAdjacentX(), direction.getAdjacentZ()}, {direction2.getAdjacentX() + direction.getAdjacentX(), direction2.getAdjacentZ() + direction.getAdjacentZ()}};
    }

    private static int[][] bedAboveStandUpOffsets(EnumDirection direction) {
        return new int[][]{{0, 0}, {-direction.getAdjacentX(), -direction.getAdjacentZ()}};
    }
}
