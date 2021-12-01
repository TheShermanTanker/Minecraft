package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalNearestVillage extends PathfinderGoal {
    private static final int DISTANCE_THRESHOLD = 10;
    private final EntityCreature mob;
    private final int interval;
    @Nullable
    private BlockPosition wantedPos;

    public PathfinderGoalNearestVillage(EntityCreature mob, int searchRange) {
        this.mob = mob;
        this.interval = reducedTickDelay(searchRange);
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isVehicle()) {
            return false;
        } else if (this.mob.level.isDay()) {
            return false;
        } else if (this.mob.getRandom().nextInt(this.interval) != 0) {
            return false;
        } else {
            WorldServer serverLevel = (WorldServer)this.mob.level;
            BlockPosition blockPos = this.mob.getChunkCoordinates();
            if (!serverLevel.isCloseToVillage(blockPos, 6)) {
                return false;
            } else {
                Vec3D vec3 = LandRandomPos.getPos(this.mob, 15, 7, (blockPosx) -> {
                    return (double)(-serverLevel.sectionsToVillage(SectionPosition.of(blockPosx)));
                });
                this.wantedPos = vec3 == null ? null : new BlockPosition(vec3);
                return this.wantedPos != null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.wantedPos != null && !this.mob.getNavigation().isDone() && this.mob.getNavigation().getTargetPos().equals(this.wantedPos);
    }

    @Override
    public void tick() {
        if (this.wantedPos != null) {
            NavigationAbstract pathNavigation = this.mob.getNavigation();
            if (pathNavigation.isDone() && !this.wantedPos.closerThan(this.mob.getPositionVector(), 10.0D)) {
                Vec3D vec3 = Vec3D.atBottomCenterOf(this.wantedPos);
                Vec3D vec32 = this.mob.getPositionVector();
                Vec3D vec33 = vec32.subtract(vec3);
                vec3 = vec33.scale(0.4D).add(vec3);
                Vec3D vec34 = vec3.subtract(vec32).normalize().scale(10.0D).add(vec32);
                BlockPosition blockPos = new BlockPosition(vec34);
                blockPos = this.mob.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, blockPos);
                if (!pathNavigation.moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0D)) {
                    this.moveRandomly();
                }
            }

        }
    }

    private void moveRandomly() {
        Random random = this.mob.getRandom();
        BlockPosition blockPos = this.mob.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, this.mob.getChunkCoordinates().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
        this.mob.getNavigation().moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0D);
    }
}
