package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.BlockFenceGate;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.BlockMinecartTrackAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PathfinderNormal extends PathfinderAbstract {
    public static final double SPACE_BETWEEN_WALL_POSTS = 0.5D;
    protected float oldWaterCost;
    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();
    private final Object2BooleanMap<AxisAlignedBB> collisionCache = new Object2BooleanOpenHashMap<>();

    @Override
    public void prepare(ChunkCache cachedWorld, EntityInsentient entity) {
        super.prepare(cachedWorld, entity);
        this.oldWaterCost = entity.getPathfindingMalus(PathType.WATER);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        this.pathTypesByPosCache.clear();
        this.collisionCache.clear();
        super.done();
    }

    @Override
    public PathPoint getStart() {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int i = this.mob.getBlockY();
        IBlockData blockState = this.level.getType(mutableBlockPos.set(this.mob.locX(), (double)i, this.mob.locZ()));
        if (!this.mob.canStandOnFluid(blockState.getFluid().getType())) {
            if (this.canFloat() && this.mob.isInWater()) {
                while(true) {
                    if (!blockState.is(Blocks.WATER) && blockState.getFluid() != FluidTypes.WATER.getSource(false)) {
                        --i;
                        break;
                    }

                    ++i;
                    blockState = this.level.getType(mutableBlockPos.set(this.mob.locX(), (double)i, this.mob.locZ()));
                }
            } else if (this.mob.isOnGround()) {
                i = MathHelper.floor(this.mob.locY() + 0.5D);
            } else {
                BlockPosition blockPos;
                for(blockPos = this.mob.getChunkCoordinates(); (this.level.getType(blockPos).isAir() || this.level.getType(blockPos).isPathfindable(this.level, blockPos, PathMode.LAND)) && blockPos.getY() > this.mob.level.getMinBuildHeight(); blockPos = blockPos.below()) {
                }

                i = blockPos.above().getY();
            }
        } else {
            while(this.mob.canStandOnFluid(blockState.getFluid().getType())) {
                ++i;
                blockState = this.level.getType(mutableBlockPos.set(this.mob.locX(), (double)i, this.mob.locZ()));
            }

            --i;
        }

        BlockPosition blockPos2 = this.mob.getChunkCoordinates();
        PathType blockPathTypes = this.getCachedBlockType(this.mob, blockPos2.getX(), i, blockPos2.getZ());
        if (this.mob.getPathfindingMalus(blockPathTypes) < 0.0F) {
            AxisAlignedBB aABB = this.mob.getBoundingBox();
            if (this.hasPositiveMalus(mutableBlockPos.set(aABB.minX, (double)i, aABB.minZ)) || this.hasPositiveMalus(mutableBlockPos.set(aABB.minX, (double)i, aABB.maxZ)) || this.hasPositiveMalus(mutableBlockPos.set(aABB.maxX, (double)i, aABB.minZ)) || this.hasPositiveMalus(mutableBlockPos.set(aABB.maxX, (double)i, aABB.maxZ))) {
                PathPoint node = this.getNode(mutableBlockPos);
                node.type = this.getBlockPathType(this.mob, node.asBlockPos());
                node.costMalus = this.mob.getPathfindingMalus(node.type);
                return node;
            }
        }

        PathPoint node2 = this.getNode(blockPos2.getX(), i, blockPos2.getZ());
        node2.type = this.getBlockPathType(this.mob, node2.asBlockPos());
        node2.costMalus = this.mob.getPathfindingMalus(node2.type);
        return node2;
    }

    private boolean hasPositiveMalus(BlockPosition pos) {
        PathType blockPathTypes = this.getBlockPathType(this.mob, pos);
        return this.mob.getPathfindingMalus(blockPathTypes) >= 0.0F;
    }

    @Override
    public PathDestination getGoal(double x, double y, double z) {
        return new PathDestination(this.getNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
    }

    @Override
    public int getNeighbors(PathPoint[] successors, PathPoint node) {
        int i = 0;
        int j = 0;
        PathType blockPathTypes = this.getCachedBlockType(this.mob, node.x, node.y + 1, node.z);
        PathType blockPathTypes2 = this.getCachedBlockType(this.mob, node.x, node.y, node.z);
        if (this.mob.getPathfindingMalus(blockPathTypes) >= 0.0F && blockPathTypes2 != PathType.STICKY_HONEY) {
            j = MathHelper.floor(Math.max(1.0F, this.mob.maxUpStep));
        }

        double d = this.getFloorLevel(new BlockPosition(node.x, node.y, node.z));
        PathPoint node2 = this.findAcceptedNode(node.x, node.y, node.z + 1, j, d, EnumDirection.SOUTH, blockPathTypes2);
        if (this.isNeighborValid(node2, node)) {
            successors[i++] = node2;
        }

        PathPoint node3 = this.findAcceptedNode(node.x - 1, node.y, node.z, j, d, EnumDirection.WEST, blockPathTypes2);
        if (this.isNeighborValid(node3, node)) {
            successors[i++] = node3;
        }

        PathPoint node4 = this.findAcceptedNode(node.x + 1, node.y, node.z, j, d, EnumDirection.EAST, blockPathTypes2);
        if (this.isNeighborValid(node4, node)) {
            successors[i++] = node4;
        }

        PathPoint node5 = this.findAcceptedNode(node.x, node.y, node.z - 1, j, d, EnumDirection.NORTH, blockPathTypes2);
        if (this.isNeighborValid(node5, node)) {
            successors[i++] = node5;
        }

        PathPoint node6 = this.findAcceptedNode(node.x - 1, node.y, node.z - 1, j, d, EnumDirection.NORTH, blockPathTypes2);
        if (this.isDiagonalValid(node, node3, node5, node6)) {
            successors[i++] = node6;
        }

        PathPoint node7 = this.findAcceptedNode(node.x + 1, node.y, node.z - 1, j, d, EnumDirection.NORTH, blockPathTypes2);
        if (this.isDiagonalValid(node, node4, node5, node7)) {
            successors[i++] = node7;
        }

        PathPoint node8 = this.findAcceptedNode(node.x - 1, node.y, node.z + 1, j, d, EnumDirection.SOUTH, blockPathTypes2);
        if (this.isDiagonalValid(node, node3, node2, node8)) {
            successors[i++] = node8;
        }

        PathPoint node9 = this.findAcceptedNode(node.x + 1, node.y, node.z + 1, j, d, EnumDirection.SOUTH, blockPathTypes2);
        if (this.isDiagonalValid(node, node4, node2, node9)) {
            successors[i++] = node9;
        }

        return i;
    }

    protected boolean isNeighborValid(@Nullable PathPoint node, PathPoint successor1) {
        return node != null && !node.closed && (node.costMalus >= 0.0F || successor1.costMalus < 0.0F);
    }

    protected boolean isDiagonalValid(PathPoint xNode, @Nullable PathPoint zNode, @Nullable PathPoint xDiagNode, @Nullable PathPoint zDiagNode) {
        if (zDiagNode != null && xDiagNode != null && zNode != null) {
            if (zDiagNode.closed) {
                return false;
            } else if (xDiagNode.y <= xNode.y && zNode.y <= xNode.y) {
                if (zNode.type != PathType.WALKABLE_DOOR && xDiagNode.type != PathType.WALKABLE_DOOR && zDiagNode.type != PathType.WALKABLE_DOOR) {
                    boolean bl = xDiagNode.type == PathType.FENCE && zNode.type == PathType.FENCE && (double)this.mob.getWidth() < 0.5D;
                    return zDiagNode.costMalus >= 0.0F && (xDiagNode.y < xNode.y || xDiagNode.costMalus >= 0.0F || bl) && (zNode.y < xNode.y || zNode.costMalus >= 0.0F || bl);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean canReachWithoutCollision(PathPoint node) {
        Vec3D vec3 = new Vec3D((double)node.x - this.mob.locX(), (double)node.y - this.mob.locY(), (double)node.z - this.mob.locZ());
        AxisAlignedBB aABB = this.mob.getBoundingBox();
        int i = MathHelper.ceil(vec3.length() / aABB.getSize());
        vec3 = vec3.scale((double)(1.0F / (float)i));

        for(int j = 1; j <= i; ++j) {
            aABB = aABB.move(vec3);
            if (this.hasCollisions(aABB)) {
                return false;
            }
        }

        return true;
    }

    protected double getFloorLevel(BlockPosition pos) {
        return getFloorLevel(this.level, pos);
    }

    public static double getFloorLevel(IBlockAccess world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        VoxelShape voxelShape = world.getType(blockPos).getCollisionShape(world, blockPos);
        return (double)blockPos.getY() + (voxelShape.isEmpty() ? 0.0D : voxelShape.max(EnumDirection.EnumAxis.Y));
    }

    protected boolean isAmphibious() {
        return false;
    }

    @Nullable
    protected PathPoint findAcceptedNode(int x, int y, int z, int maxYStep, double prevFeetY, EnumDirection direction, PathType nodeType) {
        PathPoint node = null;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        double d = this.getFloorLevel(mutableBlockPos.set(x, y, z));
        if (d - prevFeetY > 1.125D) {
            return null;
        } else {
            PathType blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
            float f = this.mob.getPathfindingMalus(blockPathTypes);
            double e = (double)this.mob.getWidth() / 2.0D;
            if (f >= 0.0F) {
                node = this.getNode(x, y, z);
                node.type = blockPathTypes;
                node.costMalus = Math.max(node.costMalus, f);
            }

            if (nodeType == PathType.FENCE && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            if (blockPathTypes != PathType.WALKABLE && (!this.isAmphibious() || blockPathTypes != PathType.WATER)) {
                if ((node == null || node.costMalus < 0.0F) && maxYStep > 0 && blockPathTypes != PathType.FENCE && blockPathTypes != PathType.UNPASSABLE_RAIL && blockPathTypes != PathType.TRAPDOOR && blockPathTypes != PathType.POWDER_SNOW) {
                    node = this.findAcceptedNode(x, y + 1, z, maxYStep - 1, prevFeetY, direction, nodeType);
                    if (node != null && (node.type == PathType.OPEN || node.type == PathType.WALKABLE) && this.mob.getWidth() < 1.0F) {
                        double g = (double)(x - direction.getAdjacentX()) + 0.5D;
                        double h = (double)(z - direction.getAdjacentZ()) + 0.5D;
                        AxisAlignedBB aABB = new AxisAlignedBB(g - e, getFloorLevel(this.level, mutableBlockPos.set(g, (double)(y + 1), h)) + 0.001D, h - e, g + e, (double)this.mob.getHeight() + getFloorLevel(this.level, mutableBlockPos.set((double)node.x, (double)node.y, (double)node.z)) - 0.002D, h + e);
                        if (this.hasCollisions(aABB)) {
                            node = null;
                        }
                    }
                }

                if (!this.isAmphibious() && blockPathTypes == PathType.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, x, y - 1, z) != PathType.WATER) {
                        return node;
                    }

                    while(y > this.mob.level.getMinBuildHeight()) {
                        --y;
                        blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
                        if (blockPathTypes != PathType.WATER) {
                            return node;
                        }

                        node = this.getNode(x, y, z);
                        node.type = blockPathTypes;
                        node.costMalus = Math.max(node.costMalus, this.mob.getPathfindingMalus(blockPathTypes));
                    }
                }

                if (blockPathTypes == PathType.OPEN) {
                    int i = 0;
                    int j = y;

                    while(blockPathTypes == PathType.OPEN) {
                        --y;
                        if (y < this.mob.level.getMinBuildHeight()) {
                            PathPoint node2 = this.getNode(x, j, z);
                            node2.type = PathType.BLOCKED;
                            node2.costMalus = -1.0F;
                            return node2;
                        }

                        if (i++ >= this.mob.getMaxFallDistance()) {
                            PathPoint node3 = this.getNode(x, y, z);
                            node3.type = PathType.BLOCKED;
                            node3.costMalus = -1.0F;
                            return node3;
                        }

                        blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
                        f = this.mob.getPathfindingMalus(blockPathTypes);
                        if (blockPathTypes != PathType.OPEN && f >= 0.0F) {
                            node = this.getNode(x, y, z);
                            node.type = blockPathTypes;
                            node.costMalus = Math.max(node.costMalus, f);
                            break;
                        }

                        if (f < 0.0F) {
                            PathPoint node4 = this.getNode(x, y, z);
                            node4.type = PathType.BLOCKED;
                            node4.costMalus = -1.0F;
                            return node4;
                        }
                    }
                }

                if (blockPathTypes == PathType.FENCE) {
                    node = this.getNode(x, y, z);
                    node.closed = true;
                    node.type = blockPathTypes;
                    node.costMalus = blockPathTypes.getMalus();
                }

                return node;
            } else {
                return node;
            }
        }
    }

    private boolean hasCollisions(AxisAlignedBB box) {
        return this.collisionCache.computeIfAbsent(box, (object) -> {
            return !this.level.getCubes(this.mob, box);
        });
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z, EntityInsentient mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
        EnumSet<PathType> enumSet = EnumSet.noneOf(PathType.class);
        PathType blockPathTypes = PathType.BLOCKED;
        BlockPosition blockPos = mob.getChunkCoordinates();
        blockPathTypes = this.getBlockPathTypes(world, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canEnterOpenDoors, enumSet, blockPathTypes, blockPos);
        if (enumSet.contains(PathType.FENCE)) {
            return PathType.FENCE;
        } else if (enumSet.contains(PathType.UNPASSABLE_RAIL)) {
            return PathType.UNPASSABLE_RAIL;
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

            return blockPathTypes == PathType.OPEN && mob.getPathfindingMalus(blockPathTypes2) == 0.0F && sizeX <= 1 ? PathType.OPEN : blockPathTypes2;
        }
    }

    public PathType getBlockPathTypes(IBlockAccess world, int x, int y, int z, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors, EnumSet<PathType> nearbyTypes, PathType type, BlockPosition pos) {
        for(int i = 0; i < sizeX; ++i) {
            for(int j = 0; j < sizeY; ++j) {
                for(int k = 0; k < sizeZ; ++k) {
                    int l = i + x;
                    int m = j + y;
                    int n = k + z;
                    PathType blockPathTypes = this.getBlockPathType(world, l, m, n);
                    blockPathTypes = this.evaluateBlockPathType(world, canOpenDoors, canEnterOpenDoors, pos, blockPathTypes);
                    if (i == 0 && j == 0 && k == 0) {
                        type = blockPathTypes;
                    }

                    nearbyTypes.add(blockPathTypes);
                }
            }
        }

        return type;
    }

    protected PathType evaluateBlockPathType(IBlockAccess world, boolean canOpenDoors, boolean canEnterOpenDoors, BlockPosition pos, PathType type) {
        if (type == PathType.DOOR_WOOD_CLOSED && canOpenDoors && canEnterOpenDoors) {
            type = PathType.WALKABLE_DOOR;
        }

        if (type == PathType.DOOR_OPEN && !canEnterOpenDoors) {
            type = PathType.BLOCKED;
        }

        if (type == PathType.RAIL && !(world.getType(pos).getBlock() instanceof BlockMinecartTrackAbstract) && !(world.getType(pos.below()).getBlock() instanceof BlockMinecartTrackAbstract)) {
            type = PathType.UNPASSABLE_RAIL;
        }

        if (type == PathType.LEAVES) {
            type = PathType.BLOCKED;
        }

        return type;
    }

    private PathType getBlockPathType(EntityInsentient entity, BlockPosition pos) {
        return this.getCachedBlockType(entity, pos.getX(), pos.getY(), pos.getZ());
    }

    protected PathType getCachedBlockType(EntityInsentient entity, int x, int y, int z) {
        return this.pathTypesByPosCache.computeIfAbsent(BlockPosition.asLong(x, y, z), (l) -> {
            return this.getBlockPathType(this.level, x, y, z, entity, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
        });
    }

    @Override
    public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
        return getBlockPathTypeStatic(world, new BlockPosition.MutableBlockPosition(x, y, z));
    }

    public static PathType getBlockPathTypeStatic(IBlockAccess world, BlockPosition.MutableBlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        PathType blockPathTypes = getBlockPathTypeRaw(world, pos);
        if (blockPathTypes == PathType.OPEN && j >= world.getMinBuildHeight() + 1) {
            PathType blockPathTypes2 = getBlockPathTypeRaw(world, pos.set(i, j - 1, k));
            blockPathTypes = blockPathTypes2 != PathType.WALKABLE && blockPathTypes2 != PathType.OPEN && blockPathTypes2 != PathType.WATER && blockPathTypes2 != PathType.LAVA ? PathType.WALKABLE : PathType.OPEN;
            if (blockPathTypes2 == PathType.DAMAGE_FIRE) {
                blockPathTypes = PathType.DAMAGE_FIRE;
            }

            if (blockPathTypes2 == PathType.DAMAGE_CACTUS) {
                blockPathTypes = PathType.DAMAGE_CACTUS;
            }

            if (blockPathTypes2 == PathType.DAMAGE_OTHER) {
                blockPathTypes = PathType.DAMAGE_OTHER;
            }

            if (blockPathTypes2 == PathType.STICKY_HONEY) {
                blockPathTypes = PathType.STICKY_HONEY;
            }
        }

        if (blockPathTypes == PathType.WALKABLE) {
            blockPathTypes = checkNeighbourBlocks(world, pos.set(i, j, k), blockPathTypes);
        }

        return blockPathTypes;
    }

    public static PathType checkNeighbourBlocks(IBlockAccess world, BlockPosition.MutableBlockPosition pos, PathType nodeType) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();

        for(int l = -1; l <= 1; ++l) {
            for(int m = -1; m <= 1; ++m) {
                for(int n = -1; n <= 1; ++n) {
                    if (l != 0 || n != 0) {
                        pos.set(i + l, j + m, k + n);
                        IBlockData blockState = world.getType(pos);
                        if (blockState.is(Blocks.CACTUS)) {
                            return PathType.DANGER_CACTUS;
                        }

                        if (blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                            return PathType.DANGER_OTHER;
                        }

                        if (isBurningBlock(blockState)) {
                            return PathType.DANGER_FIRE;
                        }

                        if (world.getFluid(pos).is(TagsFluid.WATER)) {
                            return PathType.WATER_BORDER;
                        }
                    }
                }
            }
        }

        return nodeType;
    }

    protected static PathType getBlockPathTypeRaw(IBlockAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        Block block = blockState.getBlock();
        Material material = blockState.getMaterial();
        if (blockState.isAir()) {
            return PathType.OPEN;
        } else if (!blockState.is(TagsBlock.TRAPDOORS) && !blockState.is(Blocks.LILY_PAD) && !blockState.is(Blocks.BIG_DRIPLEAF)) {
            if (blockState.is(Blocks.POWDER_SNOW)) {
                return PathType.POWDER_SNOW;
            } else if (blockState.is(Blocks.CACTUS)) {
                return PathType.DAMAGE_CACTUS;
            } else if (blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                return PathType.DAMAGE_OTHER;
            } else if (blockState.is(Blocks.HONEY_BLOCK)) {
                return PathType.STICKY_HONEY;
            } else if (blockState.is(Blocks.COCOA)) {
                return PathType.COCOA;
            } else {
                Fluid fluidState = world.getFluid(pos);
                if (fluidState.is(TagsFluid.LAVA)) {
                    return PathType.LAVA;
                } else if (isBurningBlock(blockState)) {
                    return PathType.DAMAGE_FIRE;
                } else if (BlockDoor.isWoodenDoor(blockState) && !blockState.get(BlockDoor.OPEN)) {
                    return PathType.DOOR_WOOD_CLOSED;
                } else if (block instanceof BlockDoor && material == Material.METAL && !blockState.get(BlockDoor.OPEN)) {
                    return PathType.DOOR_IRON_CLOSED;
                } else if (block instanceof BlockDoor && blockState.get(BlockDoor.OPEN)) {
                    return PathType.DOOR_OPEN;
                } else if (block instanceof BlockMinecartTrackAbstract) {
                    return PathType.RAIL;
                } else if (block instanceof BlockLeaves) {
                    return PathType.LEAVES;
                } else if (!blockState.is(TagsBlock.FENCES) && !blockState.is(TagsBlock.WALLS) && (!(block instanceof BlockFenceGate) || blockState.get(BlockFenceGate.OPEN))) {
                    if (!blockState.isPathfindable(world, pos, PathMode.LAND)) {
                        return PathType.BLOCKED;
                    } else {
                        return fluidState.is(TagsFluid.WATER) ? PathType.WATER : PathType.OPEN;
                    }
                } else {
                    return PathType.FENCE;
                }
            }
        } else {
            return PathType.TRAPDOOR;
        }
    }

    public static boolean isBurningBlock(IBlockData state) {
        return state.is(TagsBlock.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK) || BlockCampfire.isLitCampfire(state) || state.is(Blocks.LAVA_CAULDRON);
    }
}
