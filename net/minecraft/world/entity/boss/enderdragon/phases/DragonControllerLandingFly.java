package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerLandingFly extends DragonControllerAbstract {
    private static final PathfinderTargetCondition NEAR_EGG_TARGETING = PathfinderTargetCondition.forCombat().ignoreLineOfSight();
    private PathEntity currentPath;
    private Vec3D targetLocation;

    public DragonControllerLandingFly(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public DragonControllerPhase<DragonControllerLandingFly> getControllerPhase() {
        return DragonControllerPhase.LANDING_APPROACH;
    }

    @Override
    public void begin() {
        this.currentPath = null;
        this.targetLocation = null;
    }

    @Override
    public void doServerTick() {
        double d = this.targetLocation == null ? 0.0D : this.targetLocation.distanceToSqr(this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
        if (d < 100.0D || d > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            this.findNewTarget();
        }

    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    private void findNewTarget() {
        if (this.currentPath == null || this.currentPath.isDone()) {
            int i = this.dragon.findClosestNode();
            BlockPosition blockPos = this.dragon.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION);
            EntityHuman player = this.dragon.level.getNearestPlayer(NEAR_EGG_TARGETING, this.dragon, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
            int j;
            if (player != null) {
                Vec3D vec3 = (new Vec3D(player.locX(), 0.0D, player.locZ())).normalize();
                j = this.dragon.findClosestNode(-vec3.x * 40.0D, 105.0D, -vec3.z * 40.0D);
            } else {
                j = this.dragon.findClosestNode(40.0D, (double)blockPos.getY(), 0.0D);
            }

            PathPoint node = new PathPoint(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            this.currentPath = this.dragon.findPath(i, j, node);
            if (this.currentPath != null) {
                this.currentPath.advance();
            }
        }

        this.navigateToNextPathNode();
        if (this.currentPath != null && this.currentPath.isDone()) {
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.LANDING);
        }

    }

    private void navigateToNextPathNode() {
        if (this.currentPath != null && !this.currentPath.isDone()) {
            BaseBlockPosition vec3i = this.currentPath.getNextNodePos();
            this.currentPath.advance();
            double d = (double)vec3i.getX();
            double e = (double)vec3i.getZ();

            double f;
            do {
                f = (double)((float)vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F);
            } while(f < (double)vec3i.getY());

            this.targetLocation = new Vec3D(d, f, e);
        }

    }
}
