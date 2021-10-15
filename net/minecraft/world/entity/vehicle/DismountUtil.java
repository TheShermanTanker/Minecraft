package net.minecraft.world.entity.vehicle;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.ICollisionAccess;
import net.minecraft.world.level.block.BlockTrapdoor;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class DismountUtil {
    public static int[][] offsetsForDirection(EnumDirection movementDirection) {
        EnumDirection direction = movementDirection.getClockWise();
        EnumDirection direction2 = direction.opposite();
        EnumDirection direction3 = movementDirection.opposite();
        return new int[][]{{direction.getAdjacentX(), direction.getAdjacentZ()}, {direction2.getAdjacentX(), direction2.getAdjacentZ()}, {direction3.getAdjacentX() + direction.getAdjacentX(), direction3.getAdjacentZ() + direction.getAdjacentZ()}, {direction3.getAdjacentX() + direction2.getAdjacentX(), direction3.getAdjacentZ() + direction2.getAdjacentZ()}, {movementDirection.getAdjacentX() + direction.getAdjacentX(), movementDirection.getAdjacentZ() + direction.getAdjacentZ()}, {movementDirection.getAdjacentX() + direction2.getAdjacentX(), movementDirection.getAdjacentZ() + direction2.getAdjacentZ()}, {direction3.getAdjacentX(), direction3.getAdjacentZ()}, {movementDirection.getAdjacentX(), movementDirection.getAdjacentZ()}};
    }

    public static boolean isBlockFloorValid(double height) {
        return !Double.isInfinite(height) && height < 1.0D;
    }

    public static boolean canDismountTo(ICollisionAccess world, EntityLiving entity, AxisAlignedBB targetBox) {
        return world.getBlockCollisions(entity, targetBox).allMatch(VoxelShape::isEmpty);
    }

    public static boolean canDismountTo(ICollisionAccess world, Vec3D offset, EntityLiving entity, EntityPose pose) {
        return canDismountTo(world, entity, entity.getLocalBoundsForPose(pose).move(offset));
    }

    public static VoxelShape nonClimbableShape(IBlockAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return !blockState.is(TagsBlock.CLIMBABLE) && (!(blockState.getBlock() instanceof BlockTrapdoor) || !blockState.get(BlockTrapdoor.OPEN)) ? blockState.getCollisionShape(world, pos) : VoxelShapes.empty();
    }

    public static double findCeilingFrom(BlockPosition pos, int maxDistance, Function<BlockPosition, VoxelShape> collisionShapeGetter) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        int i = 0;

        while(i < maxDistance) {
            VoxelShape voxelShape = collisionShapeGetter.apply(mutableBlockPos);
            if (!voxelShape.isEmpty()) {
                return (double)(pos.getY() + i) + voxelShape.min(EnumDirection.EnumAxis.Y);
            }

            ++i;
            mutableBlockPos.move(EnumDirection.UP);
        }

        return Double.POSITIVE_INFINITY;
    }

    @Nullable
    public static Vec3D findSafeDismountLocation(EntityTypes<?> entityType, ICollisionAccess world, BlockPosition pos, boolean ignoreInvalidPos) {
        if (ignoreInvalidPos && entityType.isBlockDangerous(world.getType(pos))) {
            return null;
        } else {
            double d = world.getBlockFloorHeight(nonClimbableShape(world, pos), () -> {
                return nonClimbableShape(world, pos.below());
            });
            if (!isBlockFloorValid(d)) {
                return null;
            } else if (ignoreInvalidPos && d <= 0.0D && entityType.isBlockDangerous(world.getType(pos.below()))) {
                return null;
            } else {
                Vec3D vec3 = Vec3D.upFromBottomCenterOf(pos, d);
                return world.getBlockCollisions((Entity)null, entityType.getDimensions().makeBoundingBox(vec3)).allMatch(VoxelShape::isEmpty) ? vec3 : null;
            }
        }
    }
}
