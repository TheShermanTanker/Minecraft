package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerFly extends DragonControllerAbstract {
    private boolean firstTick;
    private PathEntity currentPath;
    private Vec3D targetLocation;

    public DragonControllerFly(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick() {
        if (!this.firstTick && this.currentPath != null) {
            BlockPosition blockPos = this.dragon.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION);
            if (!blockPos.closerThan(this.dragon.getPositionVector(), 10.0D)) {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
            }
        } else {
            this.firstTick = false;
            this.findNewTarget();
        }

    }

    @Override
    public void begin() {
        this.firstTick = true;
        this.currentPath = null;
        this.targetLocation = null;
    }

    private void findNewTarget() {
        int i = this.dragon.findClosestNode();
        Vec3D vec3 = this.dragon.getHeadLookVector(1.0F);
        int j = this.dragon.findClosestNode(-vec3.x * 40.0D, 105.0D, -vec3.z * 40.0D);
        if (this.dragon.getEnderDragonBattle() != null && this.dragon.getEnderDragonBattle().getCrystalsAlive() > 0) {
            j = j % 12;
            if (j < 0) {
                j += 12;
            }
        } else {
            j = j - 12;
            j = j & 7;
            j = j + 12;
        }

        this.currentPath = this.dragon.findPath(i, j, (PathPoint)null);
        this.navigateToNextPathNode();
    }

    private void navigateToNextPathNode() {
        if (this.currentPath != null) {
            this.currentPath.advance();
            if (!this.currentPath.isDone()) {
                BaseBlockPosition vec3i = this.currentPath.getNextNodePos();
                this.currentPath.advance();

                double d;
                do {
                    d = (double)((float)vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F);
                } while(d < (double)vec3i.getY());

                this.targetLocation = new Vec3D((double)vec3i.getX(), d, (double)vec3i.getZ());
            }
        }

    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerFly> getControllerPhase() {
        return DragonControllerPhase.TAKEOFF;
    }
}
