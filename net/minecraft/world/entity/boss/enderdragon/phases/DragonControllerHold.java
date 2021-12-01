package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerHold extends DragonControllerAbstract {
    private static final PathfinderTargetCondition NEW_TARGET_TARGETING = PathfinderTargetCondition.forCombat().ignoreLineOfSight();
    @Nullable
    private PathEntity currentPath;
    @Nullable
    private Vec3D targetLocation;
    private boolean clockwise;

    public DragonControllerHold(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public DragonControllerPhase<DragonControllerHold> getControllerPhase() {
        return DragonControllerPhase.HOLDING_PATTERN;
    }

    @Override
    public void doServerTick() {
        double d = this.targetLocation == null ? 0.0D : this.targetLocation.distanceToSqr(this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
        if (d < 100.0D || d > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            this.findNewTarget();
        }

    }

    @Override
    public void begin() {
        this.currentPath = null;
        this.targetLocation = null;
    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    private void findNewTarget() {
        if (this.currentPath != null && this.currentPath.isDone()) {
            BlockPosition blockPos = this.dragon.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPosition(WorldGenEndTrophy.END_PODIUM_LOCATION));
            int i = this.dragon.getEnderDragonBattle() == null ? 0 : this.dragon.getEnderDragonBattle().getCrystalsAlive();
            if (this.dragon.getRandom().nextInt(i + 3) == 0) {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.LANDING_APPROACH);
                return;
            }

            double d = 64.0D;
            EntityHuman player = this.dragon.level.getNearestPlayer(NEW_TARGET_TARGETING, this.dragon, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
            if (player != null) {
                d = blockPos.distSqr(player.getPositionVector(), true) / 512.0D;
            }

            if (player != null && (this.dragon.getRandom().nextInt(MathHelper.abs((int)d) + 2) == 0 || this.dragon.getRandom().nextInt(i + 2) == 0)) {
                this.strafePlayer(player);
                return;
            }
        }

        if (this.currentPath == null || this.currentPath.isDone()) {
            int j = this.dragon.findClosestNode();
            int k = j;
            if (this.dragon.getRandom().nextInt(8) == 0) {
                this.clockwise = !this.clockwise;
                k = j + 6;
            }

            if (this.clockwise) {
                ++k;
            } else {
                --k;
            }

            if (this.dragon.getEnderDragonBattle() != null && this.dragon.getEnderDragonBattle().getCrystalsAlive() >= 0) {
                k = k % 12;
                if (k < 0) {
                    k += 12;
                }
            } else {
                k = k - 12;
                k = k & 7;
                k = k + 12;
            }

            this.currentPath = this.dragon.findPath(j, k, (PathPoint)null);
            if (this.currentPath != null) {
                this.currentPath.advance();
            }
        }

        this.navigateToNextPathNode();
    }

    private void strafePlayer(EntityHuman player) {
        this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.STRAFE_PLAYER);
        this.dragon.getDragonControllerManager().getPhase(DragonControllerPhase.STRAFE_PLAYER).setTarget(player);
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

    @Override
    public void onCrystalDestroyed(EntityEnderCrystal crystal, BlockPosition pos, DamageSource source, @Nullable EntityHuman player) {
        if (player != null && this.dragon.canAttack(player)) {
            this.strafePlayer(player);
        }

    }
}
