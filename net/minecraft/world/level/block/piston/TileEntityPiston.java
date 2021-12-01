package net.minecraft.world.level.block.piston;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyPistonType;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class TileEntityPiston extends TileEntity {
    private static final int TICKS_TO_EXTEND = 2;
    private static final double PUSH_OFFSET = 0.01D;
    public static final double TICK_MOVEMENT = 0.51D;
    private IBlockData movedState = Blocks.AIR.getBlockData();
    private EnumDirection direction;
    private boolean extending;
    private boolean isSourcePiston;
    private static final ThreadLocal<EnumDirection> NOCLIP = ThreadLocal.withInitial(() -> {
        return null;
    });
    private float progress;
    private float progressO;
    private long lastTicked;
    private int deathTicks;

    public TileEntityPiston(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.PISTON, pos, state);
    }

    public TileEntityPiston(BlockPosition pos, IBlockData state, IBlockData pushedBlock, EnumDirection facing, boolean extending, boolean source) {
        this(pos, state);
        this.movedState = pushedBlock;
        this.direction = facing;
        this.extending = extending;
        this.isSourcePiston = source;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public boolean isExtending() {
        return this.extending;
    }

    public EnumDirection getDirection() {
        return this.direction;
    }

    public boolean isSourcePiston() {
        return this.isSourcePiston;
    }

    public float getProgress(float tickDelta) {
        if (tickDelta > 1.0F) {
            tickDelta = 1.0F;
        }

        return MathHelper.lerp(tickDelta, this.progressO, this.progress);
    }

    public float getXOff(float tickDelta) {
        return (float)this.direction.getAdjacentX() * this.getExtendedProgress(this.getProgress(tickDelta));
    }

    public float getYOff(float tickDelta) {
        return (float)this.direction.getAdjacentY() * this.getExtendedProgress(this.getProgress(tickDelta));
    }

    public float getZOff(float tickDelta) {
        return (float)this.direction.getAdjacentZ() * this.getExtendedProgress(this.getProgress(tickDelta));
    }

    private float getExtendedProgress(float progress) {
        return this.extending ? progress - 1.0F : 1.0F - progress;
    }

    private IBlockData getCollisionRelatedBlockState() {
        return !this.isExtending() && this.isSourcePiston() && this.movedState.getBlock() instanceof BlockPiston ? Blocks.PISTON_HEAD.getBlockData().set(BlockPistonExtension.SHORT, Boolean.valueOf(this.progress > 0.25F)).set(BlockPistonExtension.TYPE, this.movedState.is(Blocks.STICKY_PISTON) ? BlockPropertyPistonType.STICKY : BlockPropertyPistonType.DEFAULT).set(BlockPistonExtension.FACING, this.movedState.get(BlockPiston.FACING)) : this.movedState;
    }

    private static void moveCollidedEntities(World world, BlockPosition pos, float f, TileEntityPiston blockEntity) {
        EnumDirection direction = blockEntity.getMovementDirection();
        double d = (double)(f - blockEntity.progress);
        VoxelShape voxelShape = blockEntity.getCollisionRelatedBlockState().getCollisionShape(world, pos);
        if (!voxelShape.isEmpty()) {
            AxisAlignedBB aABB = moveByPositionAndProgress(pos, voxelShape.getBoundingBox(), blockEntity);
            List<Entity> list = world.getEntities((Entity)null, PistonUtil.getMovementArea(aABB, direction, d).minmax(aABB));
            if (!list.isEmpty()) {
                List<AxisAlignedBB> list2 = voxelShape.toList();
                boolean bl = blockEntity.movedState.is(Blocks.SLIME_BLOCK);
                Iterator var12 = list.iterator();

                while(true) {
                    Entity entity;
                    while(true) {
                        if (!var12.hasNext()) {
                            return;
                        }

                        entity = (Entity)var12.next();
                        if (entity.getPushReaction() != EnumPistonReaction.IGNORE) {
                            if (!bl) {
                                break;
                            }

                            if (!(entity instanceof EntityPlayer)) {
                                Vec3D vec3 = entity.getMot();
                                double e = vec3.x;
                                double g = vec3.y;
                                double h = vec3.z;
                                switch(direction.getAxis()) {
                                case X:
                                    e = (double)direction.getAdjacentX();
                                    break;
                                case Y:
                                    g = (double)direction.getAdjacentY();
                                    break;
                                case Z:
                                    h = (double)direction.getAdjacentZ();
                                }

                                entity.setMot(e, g, h);
                                break;
                            }
                        }
                    }

                    double i = 0.0D;

                    for(AxisAlignedBB aABB2 : list2) {
                        AxisAlignedBB aABB3 = PistonUtil.getMovementArea(moveByPositionAndProgress(pos, aABB2, blockEntity), direction, d);
                        AxisAlignedBB aABB4 = entity.getBoundingBox();
                        if (aABB3.intersects(aABB4)) {
                            i = Math.max(i, getMovement(aABB3, direction, aABB4));
                            if (i >= d) {
                                break;
                            }
                        }
                    }

                    if (!(i <= 0.0D)) {
                        i = Math.min(i, d) + 0.01D;
                        moveEntityByPiston(direction, entity, i, direction);
                        if (!blockEntity.extending && blockEntity.isSourcePiston) {
                            fixEntityWithinPistonBase(pos, entity, direction, d);
                        }
                    }
                }
            }
        }
    }

    private static void moveEntityByPiston(EnumDirection direction, Entity entity, double d, EnumDirection direction2) {
        NOCLIP.set(direction);
        entity.move(EnumMoveType.PISTON, new Vec3D(d * (double)direction2.getAdjacentX(), d * (double)direction2.getAdjacentY(), d * (double)direction2.getAdjacentZ()));
        NOCLIP.set((EnumDirection)null);
    }

    private static void moveStuckEntities(World world, BlockPosition pos, float f, TileEntityPiston blockEntity) {
        if (blockEntity.isStickyForEntities()) {
            EnumDirection direction = blockEntity.getMovementDirection();
            if (direction.getAxis().isHorizontal()) {
                double d = blockEntity.movedState.getCollisionShape(world, pos).max(EnumDirection.EnumAxis.Y);
                AxisAlignedBB aABB = moveByPositionAndProgress(pos, new AxisAlignedBB(0.0D, d, 0.0D, 1.0D, 1.5000000999999998D, 1.0D), blockEntity);
                double e = (double)(f - blockEntity.progress);

                for(Entity entity : world.getEntities((Entity)null, aABB, (entityx) -> {
                    return matchesStickyCritera(aABB, entityx);
                })) {
                    moveEntityByPiston(direction, entity, e, direction);
                }

            }
        }
    }

    private static boolean matchesStickyCritera(AxisAlignedBB box, Entity entity) {
        return entity.getPushReaction() == EnumPistonReaction.NORMAL && entity.isOnGround() && entity.locX() >= box.minX && entity.locX() <= box.maxX && entity.locZ() >= box.minZ && entity.locZ() <= box.maxZ;
    }

    private boolean isStickyForEntities() {
        return this.movedState.is(Blocks.HONEY_BLOCK);
    }

    public EnumDirection getMovementDirection() {
        return this.extending ? this.direction : this.direction.opposite();
    }

    private static double getMovement(AxisAlignedBB aABB, EnumDirection direction, AxisAlignedBB aABB2) {
        switch(direction) {
        case EAST:
            return aABB.maxX - aABB2.minX;
        case WEST:
            return aABB2.maxX - aABB.minX;
        case UP:
        default:
            return aABB.maxY - aABB2.minY;
        case DOWN:
            return aABB2.maxY - aABB.minY;
        case SOUTH:
            return aABB.maxZ - aABB2.minZ;
        case NORTH:
            return aABB2.maxZ - aABB.minZ;
        }
    }

    private static AxisAlignedBB moveByPositionAndProgress(BlockPosition pos, AxisAlignedBB box, TileEntityPiston blockEntity) {
        double d = (double)blockEntity.getExtendedProgress(blockEntity.progress);
        return box.move((double)pos.getX() + d * (double)blockEntity.direction.getAdjacentX(), (double)pos.getY() + d * (double)blockEntity.direction.getAdjacentY(), (double)pos.getZ() + d * (double)blockEntity.direction.getAdjacentZ());
    }

    private static void fixEntityWithinPistonBase(BlockPosition pos, Entity entity, EnumDirection direction, double amount) {
        AxisAlignedBB aABB = entity.getBoundingBox();
        AxisAlignedBB aABB2 = VoxelShapes.block().getBoundingBox().move(pos);
        if (aABB.intersects(aABB2)) {
            EnumDirection direction2 = direction.opposite();
            double d = getMovement(aABB2, direction2, aABB) + 0.01D;
            double e = getMovement(aABB2, direction2, aABB.intersect(aABB2)) + 0.01D;
            if (Math.abs(d - e) < 0.01D) {
                d = Math.min(d, amount) + 0.01D;
                moveEntityByPiston(direction, entity, d, direction2);
            }
        }

    }

    public IBlockData getMovedState() {
        return this.movedState;
    }

    public void finalTick() {
        if (this.level != null && (this.progressO < 1.0F || this.level.isClientSide)) {
            this.progress = 1.0F;
            this.progressO = this.progress;
            this.level.removeTileEntity(this.worldPosition);
            this.setRemoved();
            if (this.level.getType(this.worldPosition).is(Blocks.MOVING_PISTON)) {
                IBlockData blockState;
                if (this.isSourcePiston) {
                    blockState = Blocks.AIR.getBlockData();
                } else {
                    blockState = Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
                }

                this.level.setTypeAndData(this.worldPosition, blockState, 3);
                this.level.neighborChanged(this.worldPosition, blockState.getBlock(), this.worldPosition);
            }
        }

    }

    public static void tick(World world, BlockPosition pos, IBlockData state, TileEntityPiston blockEntity) {
        blockEntity.lastTicked = world.getTime();
        blockEntity.progressO = blockEntity.progress;
        if (blockEntity.progressO >= 1.0F) {
            if (world.isClientSide && blockEntity.deathTicks < 5) {
                ++blockEntity.deathTicks;
            } else {
                world.removeTileEntity(pos);
                blockEntity.setRemoved();
                if (world.getType(pos).is(Blocks.MOVING_PISTON)) {
                    IBlockData blockState = Block.updateFromNeighbourShapes(blockEntity.movedState, world, pos);
                    if (blockState.isAir()) {
                        world.setTypeAndData(pos, blockEntity.movedState, 84);
                        Block.updateOrDestroy(blockEntity.movedState, blockState, world, pos, 3);
                    } else {
                        if (blockState.hasProperty(BlockProperties.WATERLOGGED) && blockState.get(BlockProperties.WATERLOGGED)) {
                            blockState = blockState.set(BlockProperties.WATERLOGGED, Boolean.valueOf(false));
                        }

                        world.setTypeAndData(pos, blockState, 67);
                        world.neighborChanged(pos, blockState.getBlock(), pos);
                    }
                }

            }
        } else {
            float f = blockEntity.progress + 0.5F;
            moveCollidedEntities(world, pos, f, blockEntity);
            moveStuckEntities(world, pos, f, blockEntity);
            blockEntity.progress = f;
            if (blockEntity.progress >= 1.0F) {
                blockEntity.progress = 1.0F;
            }

        }
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.movedState = GameProfileSerializer.readBlockState(nbt.getCompound("blockState"));
        this.direction = EnumDirection.fromType1(nbt.getInt("facing"));
        this.progress = nbt.getFloat("progress");
        this.progressO = this.progress;
        this.extending = nbt.getBoolean("extending");
        this.isSourcePiston = nbt.getBoolean("source");
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        nbt.set("blockState", GameProfileSerializer.writeBlockState(this.movedState));
        nbt.setInt("facing", this.direction.get3DDataValue());
        nbt.setFloat("progress", this.progressO);
        nbt.setBoolean("extending", this.extending);
        nbt.setBoolean("source", this.isSourcePiston);
    }

    public VoxelShape getCollisionShape(IBlockAccess world, BlockPosition pos) {
        VoxelShape voxelShape;
        if (!this.extending && this.isSourcePiston && this.movedState.getBlock() instanceof BlockPiston) {
            voxelShape = this.movedState.set(BlockPiston.EXTENDED, Boolean.valueOf(true)).getCollisionShape(world, pos);
        } else {
            voxelShape = VoxelShapes.empty();
        }

        EnumDirection direction = NOCLIP.get();
        if ((double)this.progress < 1.0D && direction == this.getMovementDirection()) {
            return voxelShape;
        } else {
            IBlockData blockState;
            if (this.isSourcePiston()) {
                blockState = Blocks.PISTON_HEAD.getBlockData().set(BlockPistonExtension.FACING, this.direction).set(BlockPistonExtension.SHORT, Boolean.valueOf(this.extending != 1.0F - this.progress < 0.25F));
            } else {
                blockState = this.movedState;
            }

            float f = this.getExtendedProgress(this.progress);
            double d = (double)((float)this.direction.getAdjacentX() * f);
            double e = (double)((float)this.direction.getAdjacentY() * f);
            double g = (double)((float)this.direction.getAdjacentZ() * f);
            return VoxelShapes.or(voxelShape, blockState.getCollisionShape(world, pos).move(d, e, g));
        }
    }

    public long getLastTicked() {
        return this.lastTicked;
    }
}
