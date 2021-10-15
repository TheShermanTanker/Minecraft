package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class PathfinderWater extends PathfinderAbstract {
    private final boolean allowBreaching;

    public PathfinderWater(boolean canJumpOutOfWater) {
        this.allowBreaching = canJumpOutOfWater;
    }

    @Override
    public PathPoint getStart() {
        return super.getNode(MathHelper.floor(this.mob.getBoundingBox().minX), MathHelper.floor(this.mob.getBoundingBox().minY + 0.5D), MathHelper.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public PathDestination getGoal(double x, double y, double z) {
        return new PathDestination(super.getNode(MathHelper.floor(x - (double)(this.mob.getWidth() / 2.0F)), MathHelper.floor(y + 0.5D), MathHelper.floor(z - (double)(this.mob.getWidth() / 2.0F))));
    }

    @Override
    public int getNeighbors(PathPoint[] successors, PathPoint node) {
        int i = 0;

        for(EnumDirection direction : EnumDirection.values()) {
            PathPoint node2 = this.getWaterNode(node.x + direction.getAdjacentX(), node.y + direction.getAdjacentY(), node.z + direction.getAdjacentZ());
            if (node2 != null && !node2.closed) {
                successors[i++] = node2;
            }
        }

        return i;
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z, EntityInsentient mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
        return this.getBlockPathType(world, x, y, z);
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
        BlockPosition blockPos = new BlockPosition(x, y, z);
        Fluid fluidState = world.getFluid(blockPos);
        IBlockData blockState = world.getType(blockPos);
        if (fluidState.isEmpty() && blockState.isPathfindable(world, blockPos.below(), PathMode.WATER) && blockState.isAir()) {
            return PathType.BREACH;
        } else {
            return fluidState.is(TagsFluid.WATER) && blockState.isPathfindable(world, blockPos, PathMode.WATER) ? PathType.WATER : PathType.BLOCKED;
        }
    }

    @Nullable
    private PathPoint getWaterNode(int x, int y, int z) {
        PathType blockPathTypes = this.isFree(x, y, z);
        return (!this.allowBreaching || blockPathTypes != PathType.BREACH) && blockPathTypes != PathType.WATER ? null : this.getNode(x, y, z);
    }

    @Nullable
    @Override
    protected PathPoint getNode(int x, int y, int z) {
        PathPoint node = null;
        PathType blockPathTypes = this.getBlockPathType(this.mob.level, x, y, z);
        float f = this.mob.getPathfindingMalus(blockPathTypes);
        if (f >= 0.0F) {
            node = super.getNode(x, y, z);
            node.type = blockPathTypes;
            node.costMalus = Math.max(node.costMalus, f);
            if (this.level.getFluid(new BlockPosition(x, y, z)).isEmpty()) {
                node.costMalus += 8.0F;
            }
        }

        return blockPathTypes == PathType.OPEN ? node : node;
    }

    private PathType isFree(int x, int y, int z) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = x; i < x + this.entityWidth; ++i) {
            for(int j = y; j < y + this.entityHeight; ++j) {
                for(int k = z; k < z + this.entityDepth; ++k) {
                    Fluid fluidState = this.level.getFluid(mutableBlockPos.set(i, j, k));
                    IBlockData blockState = this.level.getType(mutableBlockPos.set(i, j, k));
                    if (fluidState.isEmpty() && blockState.isPathfindable(this.level, mutableBlockPos.below(), PathMode.WATER) && blockState.isAir()) {
                        return PathType.BREACH;
                    }

                    if (!fluidState.is(TagsFluid.WATER)) {
                        return PathType.BLOCKED;
                    }
                }
            }
        }

        IBlockData blockState2 = this.level.getType(mutableBlockPos);
        return blockState2.isPathfindable(this.level, mutableBlockPos, PathMode.WATER) ? PathType.WATER : PathType.BLOCKED;
    }
}
