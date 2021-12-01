package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.IBlockAccess;

public class PathfinderAmphibious extends PathfinderNormal {
    private final boolean prefersShallowSwimming;
    private float oldWalkableCost;
    private float oldWaterBorderCost;

    public PathfinderAmphibious(boolean penaliseDeepWater) {
        this.prefersShallowSwimming = penaliseDeepWater;
    }

    @Override
    public void prepare(ChunkCache cachedWorld, EntityInsentient entity) {
        super.prepare(cachedWorld, entity);
        entity.setPathfindingMalus(PathType.WATER, 0.0F);
        this.oldWalkableCost = entity.getPathfindingMalus(PathType.WALKABLE);
        entity.setPathfindingMalus(PathType.WALKABLE, 6.0F);
        this.oldWaterBorderCost = entity.getPathfindingMalus(PathType.WATER_BORDER);
        entity.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
        this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
        super.done();
    }

    @Override
    public PathPoint getStart() {
        return this.getNode(MathHelper.floor(this.mob.getBoundingBox().minX), MathHelper.floor(this.mob.getBoundingBox().minY + 0.5D), MathHelper.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public PathDestination getGoal(double x, double y, double z) {
        return new PathDestination(this.getNode(MathHelper.floor(x), MathHelper.floor(y + 0.5D), MathHelper.floor(z)));
    }

    @Override
    public int getNeighbors(PathPoint[] successors, PathPoint node) {
        int i = super.getNeighbors(successors, node);
        PathType blockPathTypes = this.getCachedBlockType(this.mob, node.x, node.y + 1, node.z);
        PathType blockPathTypes2 = this.getCachedBlockType(this.mob, node.x, node.y, node.z);
        int j;
        if (this.mob.getPathfindingMalus(blockPathTypes) >= 0.0F && blockPathTypes2 != PathType.STICKY_HONEY) {
            j = MathHelper.floor(Math.max(1.0F, this.mob.maxUpStep));
        } else {
            j = 0;
        }

        double d = this.getFloorLevel(new BlockPosition(node.x, node.y, node.z));
        PathPoint node2 = this.findAcceptedNode(node.x, node.y + 1, node.z, Math.max(0, j - 1), d, EnumDirection.UP, blockPathTypes2);
        PathPoint node3 = this.findAcceptedNode(node.x, node.y - 1, node.z, j, d, EnumDirection.DOWN, blockPathTypes2);
        if (this.isNeighborValid(node2, node)) {
            successors[i++] = node2;
        }

        if (this.isNeighborValid(node3, node) && blockPathTypes2 != PathType.TRAPDOOR) {
            successors[i++] = node3;
        }

        for(int l = 0; l < i; ++l) {
            PathPoint node4 = successors[l];
            if (node4.type == PathType.WATER && this.prefersShallowSwimming && node4.y < this.mob.level.getSeaLevel() - 10) {
                ++node4.costMalus;
            }
        }

        return i;
    }

    @Override
    protected double getFloorLevel(BlockPosition pos) {
        return this.mob.isInWater() ? (double)pos.getY() + 0.5D : super.getFloorLevel(pos);
    }

    @Override
    protected boolean isAmphibious() {
        return true;
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        PathType blockPathTypes = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y, z));
        if (blockPathTypes == PathType.WATER) {
            for(EnumDirection direction : EnumDirection.values()) {
                PathType blockPathTypes2 = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y, z).move(direction));
                if (blockPathTypes2 == PathType.BLOCKED) {
                    return PathType.WATER_BORDER;
                }
            }

            return PathType.WATER;
        } else {
            return getBlockPathTypeStatic(world, mutableBlockPos);
        }
    }
}
