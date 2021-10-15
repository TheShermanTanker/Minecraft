package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.PathfinderGoalUtil;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalMoveThroughVillage extends PathfinderGoal {
    protected final EntityCreature mob;
    private final double speedModifier;
    private PathEntity path;
    private BlockPosition poiPos;
    private final boolean onlyAtNight;
    private final List<BlockPosition> visited = Lists.newArrayList();
    private final int distanceToPoi;
    private final BooleanSupplier canDealWithDoors;

    public PathfinderGoalMoveThroughVillage(EntityCreature entity, double speed, boolean requiresNighttime, int distance, BooleanSupplier doorPassingThroughGetter) {
        this.mob = entity;
        this.speedModifier = speed;
        this.onlyAtNight = requiresNighttime;
        this.distanceToPoi = distance;
        this.canDealWithDoors = doorPassingThroughGetter;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        if (!PathfinderGoalUtil.hasGroundPathNavigation(entity)) {
            throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
        }
    }

    @Override
    public boolean canUse() {
        if (!PathfinderGoalUtil.hasGroundPathNavigation(this.mob)) {
            return false;
        } else {
            this.updateVisited();
            if (this.onlyAtNight && this.mob.level.isDay()) {
                return false;
            } else {
                WorldServer serverLevel = (WorldServer)this.mob.level;
                BlockPosition blockPos = this.mob.getChunkCoordinates();
                if (!serverLevel.isCloseToVillage(blockPos, 6)) {
                    return false;
                } else {
                    Vec3D vec3 = LandRandomPos.getPos(this.mob, 15, 7, (blockPos2x) -> {
                        if (!serverLevel.isVillage(blockPos2x)) {
                            return Double.NEGATIVE_INFINITY;
                        } else {
                            Optional<BlockPosition> optional = serverLevel.getPoiManager().find(VillagePlaceType.ALL, this::hasNotVisited, blockPos2x, 10, VillagePlace.Occupancy.IS_OCCUPIED);
                            return !optional.isPresent() ? Double.NEGATIVE_INFINITY : -optional.get().distSqr(blockPos);
                        }
                    });
                    if (vec3 == null) {
                        return false;
                    } else {
                        Optional<BlockPosition> optional = serverLevel.getPoiManager().find(VillagePlaceType.ALL, this::hasNotVisited, new BlockPosition(vec3), 10, VillagePlace.Occupancy.IS_OCCUPIED);
                        if (!optional.isPresent()) {
                            return false;
                        } else {
                            this.poiPos = optional.get().immutableCopy();
                            Navigation groundPathNavigation = (Navigation)this.mob.getNavigation();
                            boolean bl = groundPathNavigation.canOpenDoors();
                            groundPathNavigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
                            this.path = groundPathNavigation.createPath(this.poiPos, 0);
                            groundPathNavigation.setCanOpenDoors(bl);
                            if (this.path == null) {
                                Vec3D vec32 = DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3D.atBottomCenterOf(this.poiPos), (double)((float)Math.PI / 2F));
                                if (vec32 == null) {
                                    return false;
                                }

                                groundPathNavigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
                                this.path = this.mob.getNavigation().createPath(vec32.x, vec32.y, vec32.z, 0);
                                groundPathNavigation.setCanOpenDoors(bl);
                                if (this.path == null) {
                                    return false;
                                }
                            }

                            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                                PathPoint node = this.path.getNode(i);
                                BlockPosition blockPos2 = new BlockPosition(node.x, node.y + 1, node.z);
                                if (BlockDoor.isWoodenDoor(this.mob.level, blockPos2)) {
                                    this.path = this.mob.getNavigation().createPath((double)node.x, (double)node.y, (double)node.z, 0);
                                    break;
                                }
                            }

                            return this.path != null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.getNavigation().isDone()) {
            return false;
        } else {
            return !this.poiPos.closerThan(this.mob.getPositionVector(), (double)(this.mob.getWidth() + (float)this.distanceToPoi));
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
    }

    @Override
    public void stop() {
        if (this.mob.getNavigation().isDone() || this.poiPos.closerThan(this.mob.getPositionVector(), (double)this.distanceToPoi)) {
            this.visited.add(this.poiPos);
        }

    }

    private boolean hasNotVisited(BlockPosition pos) {
        for(BlockPosition blockPos : this.visited) {
            if (Objects.equals(pos, blockPos)) {
                return false;
            }
        }

        return true;
    }

    private void updateVisited() {
        if (this.visited.size() > 15) {
            this.visited.remove(0);
        }

    }
}
