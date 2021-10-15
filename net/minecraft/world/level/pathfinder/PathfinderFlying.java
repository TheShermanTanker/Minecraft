package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class PathfinderFlying extends PathfinderNormal {
    private final Long2ObjectMap<PathType> pathTypeByPosCache = new Long2ObjectOpenHashMap<>();

    @Override
    public void prepare(ChunkCache cachedWorld, EntityInsentient entity) {
        super.prepare(cachedWorld, entity);
        this.pathTypeByPosCache.clear();
        this.oldWaterCost = entity.getPathfindingMalus(PathType.WATER);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        this.pathTypeByPosCache.clear();
        super.done();
    }

    @Override
    public PathPoint getStart() {
        int i;
        if (this.canFloat() && this.mob.isInWater()) {
            i = this.mob.getBlockY();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(this.mob.locX(), (double)i, this.mob.locZ());

            for(IBlockData blockState = this.level.getType(mutableBlockPos); blockState.is(Blocks.WATER); blockState = this.level.getType(mutableBlockPos)) {
                ++i;
                mutableBlockPos.set(this.mob.locX(), (double)i, this.mob.locZ());
            }
        } else {
            i = MathHelper.floor(this.mob.locY() + 0.5D);
        }

        BlockPosition blockPos = this.mob.getChunkCoordinates();
        PathType blockPathTypes = this.getCachedBlockPathType(blockPos.getX(), i, blockPos.getZ());
        if (this.mob.getPathfindingMalus(blockPathTypes) < 0.0F) {
            for(BlockPosition blockPos2 : ImmutableSet.of(new BlockPosition(this.mob.getBoundingBox().minX, (double)i, this.mob.getBoundingBox().minZ), new BlockPosition(this.mob.getBoundingBox().minX, (double)i, this.mob.getBoundingBox().maxZ), new BlockPosition(this.mob.getBoundingBox().maxX, (double)i, this.mob.getBoundingBox().minZ), new BlockPosition(this.mob.getBoundingBox().maxX, (double)i, this.mob.getBoundingBox().maxZ))) {
                PathType blockPathTypes2 = this.getCachedBlockPathType(blockPos.getX(), i, blockPos.getZ());
                if (this.mob.getPathfindingMalus(blockPathTypes2) >= 0.0F) {
                    return super.getNode(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
                }
            }
        }

        return super.getNode(blockPos.getX(), i, blockPos.getZ());
    }

    @Override
    public PathDestination getGoal(double x, double y, double z) {
        return new PathDestination(super.getNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
    }

    @Override
    public int getNeighbors(PathPoint[] successors, PathPoint node) {
        int i = 0;
        PathPoint node2 = this.getNode(node.x, node.y, node.z + 1);
        if (this.isOpen(node2)) {
            successors[i++] = node2;
        }

        PathPoint node3 = this.getNode(node.x - 1, node.y, node.z);
        if (this.isOpen(node3)) {
            successors[i++] = node3;
        }

        PathPoint node4 = this.getNode(node.x + 1, node.y, node.z);
        if (this.isOpen(node4)) {
            successors[i++] = node4;
        }

        PathPoint node5 = this.getNode(node.x, node.y, node.z - 1);
        if (this.isOpen(node5)) {
            successors[i++] = node5;
        }

        PathPoint node6 = this.getNode(node.x, node.y + 1, node.z);
        if (this.isOpen(node6)) {
            successors[i++] = node6;
        }

        PathPoint node7 = this.getNode(node.x, node.y - 1, node.z);
        if (this.isOpen(node7)) {
            successors[i++] = node7;
        }

        PathPoint node8 = this.getNode(node.x, node.y + 1, node.z + 1);
        if (this.isOpen(node8) && this.hasMalus(node2) && this.hasMalus(node6)) {
            successors[i++] = node8;
        }

        PathPoint node9 = this.getNode(node.x - 1, node.y + 1, node.z);
        if (this.isOpen(node9) && this.hasMalus(node3) && this.hasMalus(node6)) {
            successors[i++] = node9;
        }

        PathPoint node10 = this.getNode(node.x + 1, node.y + 1, node.z);
        if (this.isOpen(node10) && this.hasMalus(node4) && this.hasMalus(node6)) {
            successors[i++] = node10;
        }

        PathPoint node11 = this.getNode(node.x, node.y + 1, node.z - 1);
        if (this.isOpen(node11) && this.hasMalus(node5) && this.hasMalus(node6)) {
            successors[i++] = node11;
        }

        PathPoint node12 = this.getNode(node.x, node.y - 1, node.z + 1);
        if (this.isOpen(node12) && this.hasMalus(node2) && this.hasMalus(node7)) {
            successors[i++] = node12;
        }

        PathPoint node13 = this.getNode(node.x - 1, node.y - 1, node.z);
        if (this.isOpen(node13) && this.hasMalus(node3) && this.hasMalus(node7)) {
            successors[i++] = node13;
        }

        PathPoint node14 = this.getNode(node.x + 1, node.y - 1, node.z);
        if (this.isOpen(node14) && this.hasMalus(node4) && this.hasMalus(node7)) {
            successors[i++] = node14;
        }

        PathPoint node15 = this.getNode(node.x, node.y - 1, node.z - 1);
        if (this.isOpen(node15) && this.hasMalus(node5) && this.hasMalus(node7)) {
            successors[i++] = node15;
        }

        PathPoint node16 = this.getNode(node.x + 1, node.y, node.z - 1);
        if (this.isOpen(node16) && this.hasMalus(node5) && this.hasMalus(node4)) {
            successors[i++] = node16;
        }

        PathPoint node17 = this.getNode(node.x + 1, node.y, node.z + 1);
        if (this.isOpen(node17) && this.hasMalus(node2) && this.hasMalus(node4)) {
            successors[i++] = node17;
        }

        PathPoint node18 = this.getNode(node.x - 1, node.y, node.z - 1);
        if (this.isOpen(node18) && this.hasMalus(node5) && this.hasMalus(node3)) {
            successors[i++] = node18;
        }

        PathPoint node19 = this.getNode(node.x - 1, node.y, node.z + 1);
        if (this.isOpen(node19) && this.hasMalus(node2) && this.hasMalus(node3)) {
            successors[i++] = node19;
        }

        PathPoint node20 = this.getNode(node.x + 1, node.y + 1, node.z - 1);
        if (this.isOpen(node20) && this.hasMalus(node16) && this.hasMalus(node5) && this.hasMalus(node4) && this.hasMalus(node6) && this.hasMalus(node11) && this.hasMalus(node10)) {
            successors[i++] = node20;
        }

        PathPoint node21 = this.getNode(node.x + 1, node.y + 1, node.z + 1);
        if (this.isOpen(node21) && this.hasMalus(node17) && this.hasMalus(node2) && this.hasMalus(node4) && this.hasMalus(node6) && this.hasMalus(node8) && this.hasMalus(node10)) {
            successors[i++] = node21;
        }

        PathPoint node22 = this.getNode(node.x - 1, node.y + 1, node.z - 1);
        if (this.isOpen(node22) && this.hasMalus(node18) && this.hasMalus(node5) && this.hasMalus(node3) && this.hasMalus(node6) && this.hasMalus(node11) && this.hasMalus(node9)) {
            successors[i++] = node22;
        }

        PathPoint node23 = this.getNode(node.x - 1, node.y + 1, node.z + 1);
        if (this.isOpen(node23) && this.hasMalus(node19) && this.hasMalus(node2) && this.hasMalus(node3) && this.hasMalus(node6) && this.hasMalus(node8) && this.hasMalus(node9)) {
            successors[i++] = node23;
        }

        PathPoint node24 = this.getNode(node.x + 1, node.y - 1, node.z - 1);
        if (this.isOpen(node24) && this.hasMalus(node16) && this.hasMalus(node5) && this.hasMalus(node4) && this.hasMalus(node7) && this.hasMalus(node15) && this.hasMalus(node14)) {
            successors[i++] = node24;
        }

        PathPoint node25 = this.getNode(node.x + 1, node.y - 1, node.z + 1);
        if (this.isOpen(node25) && this.hasMalus(node17) && this.hasMalus(node2) && this.hasMalus(node4) && this.hasMalus(node7) && this.hasMalus(node12) && this.hasMalus(node14)) {
            successors[i++] = node25;
        }

        PathPoint node26 = this.getNode(node.x - 1, node.y - 1, node.z - 1);
        if (this.isOpen(node26) && this.hasMalus(node18) && this.hasMalus(node5) && this.hasMalus(node3) && this.hasMalus(node7) && this.hasMalus(node15) && this.hasMalus(node13)) {
            successors[i++] = node26;
        }

        PathPoint node27 = this.getNode(node.x - 1, node.y - 1, node.z + 1);
        if (this.isOpen(node27) && this.hasMalus(node19) && this.hasMalus(node2) && this.hasMalus(node3) && this.hasMalus(node7) && this.hasMalus(node12) && this.hasMalus(node13)) {
            successors[i++] = node27;
        }

        return i;
    }

    private boolean hasMalus(@Nullable PathPoint node) {
        return node != null && node.costMalus >= 0.0F;
    }

    private boolean isOpen(@Nullable PathPoint node) {
        return node != null && !node.closed;
    }

    @Nullable
    @Override
    protected PathPoint getNode(int x, int y, int z) {
        PathPoint node = null;
        PathType blockPathTypes = this.getCachedBlockPathType(x, y, z);
        float f = this.mob.getPathfindingMalus(blockPathTypes);
        if (f >= 0.0F) {
            node = super.getNode(x, y, z);
            node.type = blockPathTypes;
            node.costMalus = Math.max(node.costMalus, f);
            if (blockPathTypes == PathType.WALKABLE) {
                ++node.costMalus;
            }
        }

        return node;
    }

    private PathType getCachedBlockPathType(int x, int y, int z) {
        return this.pathTypeByPosCache.computeIfAbsent(BlockPosition.asLong(x, y, z), (l) -> {
            return this.getBlockPathType(this.level, x, y, z, this.mob, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
        });
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z, EntityInsentient mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
        EnumSet<PathType> enumSet = EnumSet.noneOf(PathType.class);
        PathType blockPathTypes = PathType.BLOCKED;
        BlockPosition blockPos = mob.getChunkCoordinates();
        blockPathTypes = super.getBlockPathTypes(world, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canEnterOpenDoors, enumSet, blockPathTypes, blockPos);
        if (enumSet.contains(PathType.FENCE)) {
            return PathType.FENCE;
        } else {
            PathType blockPathTypes2 = PathType.BLOCKED;

            for(PathType blockPathTypes3 : enumSet) {
                if (mob.getPathfindingMalus(blockPathTypes3) < 0.0F) {
                    return blockPathTypes3;
                }

                if (mob.getPathfindingMalus(blockPathTypes3) >= mob.getPathfindingMalus(blockPathTypes2)) {
                    blockPathTypes2 = blockPathTypes3;
                }
            }

            return blockPathTypes == PathType.OPEN && mob.getPathfindingMalus(blockPathTypes2) == 0.0F ? PathType.OPEN : blockPathTypes2;
        }
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        PathType blockPathTypes = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y, z));
        if (blockPathTypes == PathType.OPEN && y >= world.getMinBuildHeight() + 1) {
            PathType blockPathTypes2 = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y - 1, z));
            if (blockPathTypes2 != PathType.DAMAGE_FIRE && blockPathTypes2 != PathType.LAVA) {
                if (blockPathTypes2 == PathType.DAMAGE_CACTUS) {
                    blockPathTypes = PathType.DAMAGE_CACTUS;
                } else if (blockPathTypes2 == PathType.DAMAGE_OTHER) {
                    blockPathTypes = PathType.DAMAGE_OTHER;
                } else if (blockPathTypes2 == PathType.COCOA) {
                    blockPathTypes = PathType.COCOA;
                } else if (blockPathTypes2 == PathType.FENCE) {
                    blockPathTypes = PathType.FENCE;
                } else {
                    blockPathTypes = blockPathTypes2 != PathType.WALKABLE && blockPathTypes2 != PathType.OPEN && blockPathTypes2 != PathType.WATER ? PathType.WALKABLE : PathType.OPEN;
                }
            } else {
                blockPathTypes = PathType.DAMAGE_FIRE;
            }
        }

        if (blockPathTypes == PathType.WALKABLE || blockPathTypes == PathType.OPEN) {
            blockPathTypes = checkNeighbourBlocks(world, mutableBlockPos.set(x, y, z), blockPathTypes);
        }

        return blockPathTypes;
    }
}
