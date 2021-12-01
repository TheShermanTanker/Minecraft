package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public interface ICollisionAccess extends IBlockAccess {
    WorldBorder getWorldBorder();

    @Nullable
    IBlockAccess getChunkForCollisions(int chunkX, int chunkZ);

    default boolean isUnobstructed(@Nullable Entity except, VoxelShape shape) {
        return true;
    }

    default boolean isUnobstructed(IBlockData state, BlockPosition pos, VoxelShapeCollision context) {
        VoxelShape voxelShape = state.getCollisionShape(this, pos, context);
        return voxelShape.isEmpty() || this.isUnobstructed((Entity)null, voxelShape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()));
    }

    default boolean isUnobstructed(Entity entity) {
        return this.isUnobstructed(entity, VoxelShapes.create(entity.getBoundingBox()));
    }

    default boolean noCollision(AxisAlignedBB box) {
        return this.getCubes((Entity)null, box);
    }

    default boolean getCubes(Entity entity) {
        return this.getCubes(entity, entity.getBoundingBox());
    }

    default boolean getCubes(@Nullable Entity entity, AxisAlignedBB box) {
        for(VoxelShape voxelShape : this.getBlockCollisions(entity, box)) {
            if (!voxelShape.isEmpty()) {
                return false;
            }
        }

        if (!this.getEntityCollisions(entity, box).isEmpty()) {
            return false;
        } else if (entity == null) {
            return true;
        } else {
            VoxelShape voxelShape2 = this.borderCollision(entity, box);
            return voxelShape2 == null || !VoxelShapes.joinIsNotEmpty(voxelShape2, VoxelShapes.create(box), OperatorBoolean.AND);
        }
    }

    List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AxisAlignedBB box);

    default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AxisAlignedBB box) {
        List<VoxelShape> list = this.getEntityCollisions(entity, box);
        Iterable<VoxelShape> iterable = this.getBlockCollisions(entity, box);
        return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
    }

    default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AxisAlignedBB box) {
        return () -> {
            return new BlockCollisions(this, entity, box);
        };
    }

    @Nullable
    private default VoxelShape borderCollision(Entity entity, AxisAlignedBB box) {
        WorldBorder worldBorder = this.getWorldBorder();
        return worldBorder.isInsideCloseToBorder(entity, box) ? worldBorder.getCollisionShape() : null;
    }

    default boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AxisAlignedBB box) {
        BlockCollisions blockCollisions = new BlockCollisions(this, entity, box, true);

        while(blockCollisions.hasNext()) {
            if (!blockCollisions.next().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    default Optional<Vec3D> findFreePosition(@Nullable Entity entity, VoxelShape shape, Vec3D target, double x, double y, double z) {
        if (shape.isEmpty()) {
            return Optional.empty();
        } else {
            AxisAlignedBB aABB = shape.getBoundingBox().grow(x, y, z);
            VoxelShape voxelShape = StreamSupport.stream(this.getBlockCollisions(entity, aABB).spliterator(), false).filter((voxelShapex) -> {
                return this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(voxelShapex.getBoundingBox());
            }).flatMap((voxelShapex) -> {
                return voxelShapex.toList().stream();
            }).map((aABBx) -> {
                return aABBx.grow(x / 2.0D, y / 2.0D, z / 2.0D);
            }).map(VoxelShapes::create).reduce(VoxelShapes.empty(), VoxelShapes::or);
            VoxelShape voxelShape2 = VoxelShapes.join(shape, voxelShape, OperatorBoolean.ONLY_FIRST);
            return voxelShape2.closestPointTo(target);
        }
    }
}
