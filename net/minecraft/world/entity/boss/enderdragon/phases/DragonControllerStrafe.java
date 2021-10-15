package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityDragonFireball;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DragonControllerStrafe extends DragonControllerAbstract {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int FIREBALL_CHARGE_AMOUNT = 5;
    private int fireballCharge;
    private PathEntity currentPath;
    private Vec3D targetLocation;
    private EntityLiving attackTarget;
    private boolean holdingPatternClockwise;

    public DragonControllerStrafe(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick() {
        if (this.attackTarget == null) {
            LOGGER.warn("Skipping player strafe phase because no player was found");
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
        } else {
            if (this.currentPath != null && this.currentPath.isDone()) {
                double d = this.attackTarget.locX();
                double e = this.attackTarget.locZ();
                double f = d - this.dragon.locX();
                double g = e - this.dragon.locZ();
                double h = Math.sqrt(f * f + g * g);
                double i = Math.min((double)0.4F + h / 80.0D - 1.0D, 10.0D);
                this.targetLocation = new Vec3D(d, this.attackTarget.locY() + i, e);
            }

            double j = this.targetLocation == null ? 0.0D : this.targetLocation.distanceToSqr(this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
            if (j < 100.0D || j > 22500.0D) {
                this.findNewTarget();
            }

            double k = 64.0D;
            if (this.attackTarget.distanceToSqr(this.dragon) < 4096.0D) {
                if (this.dragon.hasLineOfSight(this.attackTarget)) {
                    ++this.fireballCharge;
                    Vec3D vec3 = (new Vec3D(this.attackTarget.locX() - this.dragon.locX(), 0.0D, this.attackTarget.locZ() - this.dragon.locZ())).normalize();
                    Vec3D vec32 = (new Vec3D((double)MathHelper.sin(this.dragon.getYRot() * ((float)Math.PI / 180F)), 0.0D, (double)(-MathHelper.cos(this.dragon.getYRot() * ((float)Math.PI / 180F))))).normalize();
                    float l = (float)vec32.dot(vec3);
                    float m = (float)(Math.acos((double)l) * (double)(180F / (float)Math.PI));
                    m = m + 0.5F;
                    if (this.fireballCharge >= 5 && m >= 0.0F && m < 10.0F) {
                        double n = 1.0D;
                        Vec3D vec33 = this.dragon.getViewVector(1.0F);
                        double o = this.dragon.head.locX() - vec33.x * 1.0D;
                        double p = this.dragon.head.getY(0.5D) + 0.5D;
                        double q = this.dragon.head.locZ() - vec33.z * 1.0D;
                        double r = this.attackTarget.locX() - o;
                        double s = this.attackTarget.getY(0.5D) - p;
                        double t = this.attackTarget.locZ() - q;
                        if (!this.dragon.isSilent()) {
                            this.dragon.level.levelEvent((EntityHuman)null, 1017, this.dragon.getChunkCoordinates(), 0);
                        }

                        EntityDragonFireball dragonFireball = new EntityDragonFireball(this.dragon.level, this.dragon, r, s, t);
                        dragonFireball.setPositionRotation(o, p, q, 0.0F, 0.0F);
                        this.dragon.level.addEntity(dragonFireball);
                        this.fireballCharge = 0;
                        if (this.currentPath != null) {
                            while(!this.currentPath.isDone()) {
                                this.currentPath.advance();
                            }
                        }

                        this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
                    }
                } else if (this.fireballCharge > 0) {
                    --this.fireballCharge;
                }
            } else if (this.fireballCharge > 0) {
                --this.fireballCharge;
            }

        }
    }

    private void findNewTarget() {
        if (this.currentPath == null || this.currentPath.isDone()) {
            int i = this.dragon.findClosestNode();
            int j = i;
            if (this.dragon.getRandom().nextInt(8) == 0) {
                this.holdingPatternClockwise = !this.holdingPatternClockwise;
                j = i + 6;
            }

            if (this.holdingPatternClockwise) {
                ++j;
            } else {
                --j;
            }

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
            if (this.currentPath != null) {
                this.currentPath.advance();
            }
        }

        this.navigateToNextPathNode();
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
    public void begin() {
        this.fireballCharge = 0;
        this.targetLocation = null;
        this.currentPath = null;
        this.attackTarget = null;
    }

    public void setTarget(EntityLiving targetEntity) {
        this.attackTarget = targetEntity;
        int i = this.dragon.findClosestNode();
        int j = this.dragon.findClosestNode(this.attackTarget.locX(), this.attackTarget.locY(), this.attackTarget.locZ());
        int k = this.attackTarget.getBlockX();
        int l = this.attackTarget.getBlockZ();
        double d = (double)k - this.dragon.locX();
        double e = (double)l - this.dragon.locZ();
        double f = Math.sqrt(d * d + e * e);
        double g = Math.min((double)0.4F + f / 80.0D - 1.0D, 10.0D);
        int m = MathHelper.floor(this.attackTarget.locY() + g);
        PathPoint node = new PathPoint(k, m, l);
        this.currentPath = this.dragon.findPath(i, j, node);
        if (this.currentPath != null) {
            this.currentPath.advance();
            this.navigateToNextPathNode();
        }

    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerStrafe> getControllerPhase() {
        return DragonControllerPhase.STRAFE_PLAYER;
    }
}
