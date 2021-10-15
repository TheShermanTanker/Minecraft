package net.minecraft.world.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityCreature extends EntityInsentient {
    protected EntityCreature(EntityTypes<? extends EntityCreature> type, World world) {
        super(type, world);
    }

    public float getWalkTargetValue(BlockPosition pos) {
        return this.getWalkTargetValue(pos, this.level);
    }

    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return 0.0F;
    }

    @Override
    public boolean checkSpawnRules(GeneratorAccess world, EnumMobSpawn spawnReason) {
        return this.getWalkTargetValue(this.getChunkCoordinates(), world) >= 0.0F;
    }

    public boolean isPathFinding() {
        return !this.getNavigation().isDone();
    }

    @Override
    protected void tickLeash() {
        super.tickLeash();
        Entity entity = this.getLeashHolder();
        if (entity != null && entity.level == this.level) {
            this.restrictTo(entity.getChunkCoordinates(), 5);
            float f = this.distanceTo(entity);
            if (this instanceof EntityTameableAnimal && ((EntityTameableAnimal)this).isSitting()) {
                if (f > 10.0F) {
                    this.unleash(true, true);
                }

                return;
            }

            this.onLeashDistance(f);
            if (f > 10.0F) {
                this.unleash(true, true);
                this.goalSelector.disableControlFlag(PathfinderGoal.Type.MOVE);
            } else if (f > 6.0F) {
                double d = (entity.locX() - this.locX()) / (double)f;
                double e = (entity.locY() - this.locY()) / (double)f;
                double g = (entity.locZ() - this.locZ()) / (double)f;
                this.setMot(this.getMot().add(Math.copySign(d * d * 0.4D, d), Math.copySign(e * e * 0.4D, e), Math.copySign(g * g * 0.4D, g)));
            } else {
                this.goalSelector.enableControlFlag(PathfinderGoal.Type.MOVE);
                float h = 2.0F;
                Vec3D vec3 = (new Vec3D(entity.locX() - this.locX(), entity.locY() - this.locY(), entity.locZ() - this.locZ())).normalize().scale((double)Math.max(f - 2.0F, 0.0F));
                this.getNavigation().moveTo(this.locX() + vec3.x, this.locY() + vec3.y, this.locZ() + vec3.z, this.followLeashSpeed());
            }
        }

    }

    protected double followLeashSpeed() {
        return 1.0D;
    }

    protected void onLeashDistance(float leashLength) {
    }
}
