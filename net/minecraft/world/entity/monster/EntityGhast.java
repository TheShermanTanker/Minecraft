package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityFlying;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityLargeFireball;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityGhast extends EntityFlying implements IMonster {
    private static final DataWatcherObject<Boolean> DATA_IS_CHARGING = DataWatcher.defineId(EntityGhast.class, DataWatcherRegistry.BOOLEAN);
    private int explosionPower = 1;

    public EntityGhast(EntityTypes<? extends EntityGhast> type, World world) {
        super(type, world);
        this.xpReward = 5;
        this.moveControl = new EntityGhast.ControllerGhast(this);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(5, new EntityGhast.PathfinderGoalGhastIdleMove(this));
        this.goalSelector.addGoal(7, new EntityGhast.PathfinderGoalGhastMoveTowardsTarget(this));
        this.goalSelector.addGoal(7, new EntityGhast.PathfinderGoalGhastAttackTarget(this));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, (entity) -> {
            return Math.abs(entity.locY() - this.locY()) <= 4.0D;
        }));
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean shooting) {
        this.entityData.set(DATA_IS_CHARGING, shooting);
    }

    public int getPower() {
        return this.explosionPower;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (source.getDirectEntity() instanceof EntityLargeFireball && source.getEntity() instanceof EntityHuman) {
            super.damageEntity(source, 1000.0F);
            return true;
        } else {
            return super.damageEntity(source, amount);
        }
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_IS_CHARGING, false);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.FOLLOW_RANGE, 100.0D);
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.GHAST_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.GHAST_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.GHAST_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 5.0F;
    }

    public static boolean checkGhastSpawnRules(EntityTypes<EntityGhast> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && random.nextInt(20) == 0 && checkMobSpawnRules(type, world, spawnReason, pos, random);
    }

    @Override
    public int getMaxSpawnGroup() {
        return 1;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setByte("ExplosionPower", (byte)this.explosionPower);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("ExplosionPower", 99)) {
            this.explosionPower = nbt.getByte("ExplosionPower");
        }

    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 2.6F;
    }

    static class ControllerGhast extends ControllerMove {
        private final EntityGhast ghast;
        private int floatDuration;

        public ControllerGhast(EntityGhast ghast) {
            super(ghast);
            this.ghast = ghast;
        }

        @Override
        public void tick() {
            if (this.operation == ControllerMove.Operation.MOVE_TO) {
                if (this.floatDuration-- <= 0) {
                    this.floatDuration += this.ghast.getRandom().nextInt(5) + 2;
                    Vec3D vec3 = new Vec3D(this.wantedX - this.ghast.locX(), this.wantedY - this.ghast.locY(), this.wantedZ - this.ghast.locZ());
                    double d = vec3.length();
                    vec3 = vec3.normalize();
                    if (this.canReach(vec3, MathHelper.ceil(d))) {
                        this.ghast.setMot(this.ghast.getMot().add(vec3.scale(0.1D)));
                    } else {
                        this.operation = ControllerMove.Operation.WAIT;
                    }
                }

            }
        }

        private boolean canReach(Vec3D direction, int steps) {
            AxisAlignedBB aABB = this.ghast.getBoundingBox();

            for(int i = 1; i < steps; ++i) {
                aABB = aABB.move(direction);
                if (!this.ghast.level.getCubes(this.ghast, aABB)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class PathfinderGoalGhastAttackTarget extends PathfinderGoal {
        private final EntityGhast ghast;
        public int chargeTime;

        public PathfinderGoalGhastAttackTarget(EntityGhast ghast) {
            this.ghast = ghast;
        }

        @Override
        public boolean canUse() {
            return this.ghast.getGoalTarget() != null;
        }

        @Override
        public void start() {
            this.chargeTime = 0;
        }

        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = this.ghast.getGoalTarget();
            if (livingEntity != null) {
                double d = 64.0D;
                if (livingEntity.distanceToSqr(this.ghast) < 4096.0D && this.ghast.hasLineOfSight(livingEntity)) {
                    World level = this.ghast.level;
                    ++this.chargeTime;
                    if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                        level.triggerEffect((EntityHuman)null, 1015, this.ghast.getChunkCoordinates(), 0);
                    }

                    if (this.chargeTime == 20) {
                        double e = 4.0D;
                        Vec3D vec3 = this.ghast.getViewVector(1.0F);
                        double f = livingEntity.locX() - (this.ghast.locX() + vec3.x * 4.0D);
                        double g = livingEntity.getY(0.5D) - (0.5D + this.ghast.getY(0.5D));
                        double h = livingEntity.locZ() - (this.ghast.locZ() + vec3.z * 4.0D);
                        if (!this.ghast.isSilent()) {
                            level.triggerEffect((EntityHuman)null, 1016, this.ghast.getChunkCoordinates(), 0);
                        }

                        EntityLargeFireball largeFireball = new EntityLargeFireball(level, this.ghast, f, g, h, this.ghast.getPower());
                        largeFireball.setPosition(this.ghast.locX() + vec3.x * 4.0D, this.ghast.getY(0.5D) + 0.5D, largeFireball.locZ() + vec3.z * 4.0D);
                        level.addEntity(largeFireball);
                        this.chargeTime = -40;
                    }
                } else if (this.chargeTime > 0) {
                    --this.chargeTime;
                }

                this.ghast.setCharging(this.chargeTime > 10);
            }
        }
    }

    static class PathfinderGoalGhastIdleMove extends PathfinderGoal {
        private final EntityGhast ghast;

        public PathfinderGoalGhastIdleMove(EntityGhast ghast) {
            this.ghast = ghast;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            ControllerMove moveControl = this.ghast.getControllerMove();
            if (!moveControl.hasWanted()) {
                return true;
            } else {
                double d = moveControl.getWantedX() - this.ghast.locX();
                double e = moveControl.getWantedY() - this.ghast.locY();
                double f = moveControl.getWantedZ() - this.ghast.locZ();
                double g = d * d + e * e + f * f;
                return g < 1.0D || g > 3600.0D;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Random random = this.ghast.getRandom();
            double d = this.ghast.locX() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
            double e = this.ghast.locY() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
            double f = this.ghast.locZ() + (double)((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
            this.ghast.getControllerMove().setWantedPosition(d, e, f, 1.0D);
        }
    }

    static class PathfinderGoalGhastMoveTowardsTarget extends PathfinderGoal {
        private final EntityGhast ghast;

        public PathfinderGoalGhastMoveTowardsTarget(EntityGhast ghast) {
            this.ghast = ghast;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.ghast.getGoalTarget() == null) {
                Vec3D vec3 = this.ghast.getMot();
                this.ghast.setYRot(-((float)MathHelper.atan2(vec3.x, vec3.z)) * (180F / (float)Math.PI));
                this.ghast.yBodyRot = this.ghast.getYRot();
            } else {
                EntityLiving livingEntity = this.ghast.getGoalTarget();
                double d = 64.0D;
                if (livingEntity.distanceToSqr(this.ghast) < 4096.0D) {
                    double e = livingEntity.locX() - this.ghast.locX();
                    double f = livingEntity.locZ() - this.ghast.locZ();
                    this.ghast.setYRot(-((float)MathHelper.atan2(e, f)) * (180F / (float)Math.PI));
                    this.ghast.yBodyRot = this.ghast.getYRot();
                }
            }

        }
    }
}
