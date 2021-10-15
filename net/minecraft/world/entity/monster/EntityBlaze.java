package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntitySmallFireball;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityBlaze extends EntityMonster {
    private float allowedHeightOffset = 0.5F;
    private int nextHeightOffsetChangeTick;
    private static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityBlaze.class, DataWatcherRegistry.BYTE);

    public EntityBlaze(EntityTypes<? extends EntityBlaze> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
        this.xpReward = 10;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(4, new EntityBlaze.PathfinderGoalBlazeFireball(this));
        this.goalSelector.addGoal(5, new PathfinderGoalMoveTowardsRestriction(this, 1.0D));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStrollLand(this, 1.0D, 0.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.ATTACK_DAMAGE, 6.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.23F).add(GenericAttributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.BLAZE_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.BLAZE_DEATH;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public void movementTick() {
        if (!this.onGround && this.getMot().y < 0.0D) {
            this.setMot(this.getMot().multiply(1.0D, 0.6D, 1.0D));
        }

        if (this.level.isClientSide) {
            if (this.random.nextInt(24) == 0 && !this.isSilent()) {
                this.level.playLocalSound(this.locX() + 0.5D, this.locY() + 0.5D, this.locZ() + 0.5D, SoundEffects.BLAZE_BURN, this.getSoundCategory(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
            }

            for(int i = 0; i < 2; ++i) {
                this.level.addParticle(Particles.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
        }

        super.movementTick();
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected void mobTick() {
        --this.nextHeightOffsetChangeTick;
        if (this.nextHeightOffsetChangeTick <= 0) {
            this.nextHeightOffsetChangeTick = 100;
            this.allowedHeightOffset = 0.5F + (float)this.random.nextGaussian() * 3.0F;
        }

        EntityLiving livingEntity = this.getGoalTarget();
        if (livingEntity != null && livingEntity.getHeadY() > this.getHeadY() + (double)this.allowedHeightOffset && this.canAttack(livingEntity)) {
            Vec3D vec3 = this.getMot();
            this.setMot(this.getMot().add(0.0D, ((double)0.3F - vec3.y) * (double)0.3F, 0.0D));
            this.hasImpulse = true;
        }

        super.mobTick();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isBurning() {
        return this.isCharged();
    }

    private boolean isCharged() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    void setCharged(boolean fireActive) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (fireActive) {
            b = (byte)(b | 1);
        } else {
            b = (byte)(b & -2);
        }

        this.entityData.set(DATA_FLAGS_ID, b);
    }

    static class PathfinderGoalBlazeFireball extends PathfinderGoal {
        private final EntityBlaze blaze;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        public PathfinderGoalBlazeFireball(EntityBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = this.blaze.getGoalTarget();
            return livingEntity != null && livingEntity.isAlive() && this.blaze.canAttack(livingEntity);
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            this.blaze.setCharged(false);
            this.lastSeen = 0;
        }

        @Override
        public void tick() {
            --this.attackTime;
            EntityLiving livingEntity = this.blaze.getGoalTarget();
            if (livingEntity != null) {
                boolean bl = this.blaze.getEntitySenses().hasLineOfSight(livingEntity);
                if (bl) {
                    this.lastSeen = 0;
                } else {
                    ++this.lastSeen;
                }

                double d = this.blaze.distanceToSqr(livingEntity);
                if (d < 4.0D) {
                    if (!bl) {
                        return;
                    }

                    if (this.attackTime <= 0) {
                        this.attackTime = 20;
                        this.blaze.attackEntity(livingEntity);
                    }

                    this.blaze.getControllerMove().setWantedPosition(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ(), 1.0D);
                } else if (d < this.getFollowDistance() * this.getFollowDistance() && bl) {
                    double e = livingEntity.locX() - this.blaze.locX();
                    double f = livingEntity.getY(0.5D) - this.blaze.getY(0.5D);
                    double g = livingEntity.locZ() - this.blaze.locZ();
                    if (this.attackTime <= 0) {
                        ++this.attackStep;
                        if (this.attackStep == 1) {
                            this.attackTime = 60;
                            this.blaze.setCharged(true);
                        } else if (this.attackStep <= 4) {
                            this.attackTime = 6;
                        } else {
                            this.attackTime = 100;
                            this.attackStep = 0;
                            this.blaze.setCharged(false);
                        }

                        if (this.attackStep > 1) {
                            double h = Math.sqrt(Math.sqrt(d)) * 0.5D;
                            if (!this.blaze.isSilent()) {
                                this.blaze.level.levelEvent((EntityHuman)null, 1018, this.blaze.getChunkCoordinates(), 0);
                            }

                            for(int i = 0; i < 1; ++i) {
                                EntitySmallFireball smallFireball = new EntitySmallFireball(this.blaze.level, this.blaze, e + this.blaze.getRandom().nextGaussian() * h, f, g + this.blaze.getRandom().nextGaussian() * h);
                                smallFireball.setPosition(smallFireball.locX(), this.blaze.getY(0.5D) + 0.5D, smallFireball.locZ());
                                this.blaze.level.addEntity(smallFireball);
                            }
                        }
                    }

                    this.blaze.getControllerLook().setLookAt(livingEntity, 10.0F, 10.0F);
                } else if (this.lastSeen < 5) {
                    this.blaze.getControllerMove().setWantedPosition(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ(), 1.0D);
                }

                super.tick();
            }
        }

        private double getFollowDistance() {
            return this.blaze.getAttributeValue(GenericAttributes.FOLLOW_RANGE);
        }
    }
}
