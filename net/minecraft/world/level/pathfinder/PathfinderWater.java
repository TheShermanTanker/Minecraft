package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class PathfinderWater extends PathfinderAbstract {
    private final boolean allowBreaching;
    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();

    public PathfinderWater(boolean canJumpOutOfWater) {
        this.allowBreaching = canJumpOutOfWater;
    }

    @Override
    public void prepare(ChunkCache cachedWorld, EntityInsentient entity) {
        super.prepare(cachedWorld, entity);
        this.pathTypesByPosCache.clear();
    }

    @Override
    public void done() {
        super.done();
        this.pathTypesByPosCache.clear();
    }

    @Override
    public PathPoint getStart() {
        return super.getNode(MathHelper.floor(this.mob.getBoundingBox().minX), MathHelper.floor(this.mob.getBoundingBox().minY + 0.5D), MathHelper.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public PathDestination getGoal(double x, double y, double z) {
        return new PathDestination(super.getNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
    }

    @Override
    public int getNeighbors(PathPoint[] successors, PathPoint node) {
        int i = 0;
        Map<EnumDirection, PathPoint> map = Maps.newEnumMap(EnumDirection.class);

        for(EnumDirection direction : EnumDirection.values()) {
            PathPoint node2 = this.getNode(node.x + direction.getAdjacentX(), node.y + direction.getAdjacentY(), node.z + direction.getAdjacentZ());
            map.put(direction, node2);
            if (this.isNodeValid(node2)) {
                successors[i++] = node2;
            }
        }

        for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            EnumDirection direction3 = direction2.getClockWise();
            PathPoint node3 = this.getNode(node.x + direction2.getAdjacentX() + direction3.getAdjacentX(), node.y, node.z + direction2.getAdjacentZ() + direction3.getAdjacentZ());
            if (this.isDiagonalNodeValid(node3, map.get(direction2), map.get(direction3))) {
                successors[i++] = node3;
            }
        }

        return i;
    }

    protected boolean isNodeValid(@Nullable PathPoint node) {
        return node != null && !node.closed;
    }

    protected boolean isDiagonalNodeValid(@Nullable PathPoint node, @Nullable PathPoint node2, @Nullable PathPoint node3) {
        return this.isNodeValid(node) && node2 != null && node2.costMalus >= 0.0F && node3 != null && node3.costMalus >= 0.0F;
    }

    @Nullable
    @Override
    protected PathPoint getNode(int x, int y, int z) {
        PathPoint node = null;
        PathType blockPathTypes = this.getCachedBlockType(x, y, z);
        if (this.allowBreaching && blockPathTypes == PathType.BREACH || blockPathTypes == PathType.WATER) {
            float f = this.mob.getPathfindingMalus(blockPathTypes);
            if (f >= 0.0F) {
                node = super.getNode(x, y, z);
                node.type = blockPathTypes;
                node.costMalus = Math.max(node.costMalus, f);
                if (this.level.getFluid(new BlockPosition(x, y, z)).isEmpty()) {
                    node.costMalus += 8.0F;
                }
            }
        }

        return node;
    }

    protected PathType getCachedBlockType(int i, int j, int k) {
        return this.pathTypesByPosCache.computeIfAbsent(BlockPosition.asLong(i, j, k), (l) -> {
            return this.getBlockPathType(this.level, i, j, k);
        });
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
        return this.getBlockPathType(world, x, y, z, this.mob, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z, EntityInsentient mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = x; i < x + sizeX; ++i) {
            for(int j = y; j < y + sizeY; ++j) {
                for(int k = z; k < z + sizeZ; ++k) {
                    Fluid fluidState = world.getFluid(mutableBlockPos.set(i, j, k));
                    IBlockData blockState = world.getType(mutableBlockPos.set(i, j, k));
                    if (fluidState.isEmpty() && blockState.isPathfindable(world, mutableBlockPos.below(), PathMode.WATER) && blockState.isAir()) {
                        return PathType.BREACH;
                    }

                    if (!fluidState.is(TagsFluid.WATER)) {
                        return PathType.BLOCKED;
                    }
                }
            }
        }

        IBlockData blockState2 = world.getType(mutableBlockPos);
        return blockState2.isPathfindable(world, mutableBlockPos, PathMode.WATER) ? PathType.WATER : PathType.BLOCKED;
    }
}
