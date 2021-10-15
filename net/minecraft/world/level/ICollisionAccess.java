package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
        return this.noCollision((Entity)null, box, (e) -> {
            return true;
        });
    }

    default boolean getCubes(Entity entity) {
        return this.noCollision(entity, entity.getBoundingBox(), (e) -> {
            return true;
        });
    }

    default boolean getCubes(Entity entity, AxisAlignedBB box) {
        return this.noCollision(entity, box, (e) -> {
            return true;
        });
    }

    default boolean noCollision(@Nullable Entity entity, AxisAlignedBB box, Predicate<Entity> filter) {
        return this.getCollisions(entity, box, filter).allMatch(VoxelShape::isEmpty);
    }

    Stream<VoxelShape> getEntityCollisions(@Nullable Entity entity, AxisAlignedBB box, Predicate<Entity> predicate);

    default Stream<VoxelShape> getCollisions(@Nullable Entity entity, AxisAlignedBB box, Predicate<Entity> predicate) {
        return Stream.concat(this.getBlockCollisions(entity, box), this.getEntityCollisions(entity, box, predicate));
    }

    default Stream<VoxelShape> getBlockCollisions(@Nullable Entity entity, AxisAlignedBB box) {
        return StreamSupport.stream(new VoxelShapeSpliterator(this, entity, box), false);
    }

    default boolean hasBlockCollision(@Nullable Entity entity, AxisAlignedBB box, BiPredicate<IBlockData, BlockPosition> predicate) {
        return !this.getBlockCollisions(entity, box, predicate).allMatch(VoxelShape::isEmpty);
    }

    default Stream<VoxelShape> getBlockCollisions(@Nullable Entity entity, AxisAlignedBB box, BiPredicate<IBlockData, BlockPosition> predicate) {
        return StreamSupport.stream(new VoxelShapeSpliterator(this, entity, box, predicate), false);
    }

    default Optional<Vec3D> findFreePosition(@Nullable Entity entity, VoxelShape shape, Vec3D target, double x, double y, double z) {
        if (shape.isEmpty()) {
            return Optional.empty();
        } else {
            AxisAlignedBB aABB = shape.getBoundingBox().grow(x, y, z);
            VoxelShape voxelShape = this.getBlockCollisions(entity, aABB).flatMap((collision) -> {
                return collision.toList().stream();
            }).map((box) -> {
                return box.grow(x / 2.0D, y / 2.0D, z / 2.0D);
            }).map(VoxelShapes::create).reduce(VoxelShapes.empty(), VoxelShapes::or);
            VoxelShape voxelShape2 = VoxelShapes.join(shape, voxelShape, OperatorBoolean.ONLY_FIRST);
            return voxelShape2.closestPointTo(target);
        }
    }
}
