package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.animal.EntitySquid;
import net.minecraft.world.entity.animal.axolotl.EntityAxolotl;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityGuardian extends EntityMonster {
    protected static final int ATTACK_TIME = 80;
    private static final DataWatcherObject<Boolean> DATA_ID_MOVING = DataWatcher.defineId(EntityGuardian.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_ID_ATTACK_TARGET = DataWatcher.defineId(EntityGuardian.class, DataWatcherRegistry.INT);
    private float clientSideTailAnimation;
    private float clientSideTailAnimationO;
    private float clientSideTailAnimationSpeed;
    private float clientSideSpikesAnimation;
    private float clientSideSpikesAnimationO;
    private EntityLiving clientSideCachedAttackTarget;
    private int clientSideAttackTime;
    private boolean clientSideTouchedGround;
    public PathfinderGoalRandomStroll randomStrollGoal;

    public EntityGuardian(EntityTypes<? extends EntityGuardian> type, World world) {
        super(type, world);
        this.xpReward = 10;
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new EntityGuardian.ControllerMoveGuardian(this);
        this.clientSideTailAnimation = this.random.nextFloat();
        this.clientSideTailAnimationO = this.clientSideTailAnimation;
    }

    @Override
    protected void initPathfinder() {
        PathfinderGoalMoveTowardsRestriction moveTowardsRestrictionGoal = new PathfinderGoalMoveTowardsRestriction(this, 1.0D);
        this.randomStrollGoal = new PathfinderGoalRandomStroll(this, 1.0D, 80);
        this.goalSelector.addGoal(4, new EntityGuardian.PathfinderGoalGuardianAttack(this));
        this.goalSelector.addGoal(5, moveTowardsRestrictionGoal);
        this.goalSelector.addGoal(7, this.randomStrollGoal);
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityGuardian.class, 12.0F, 0.01F));
        this.goalSelector.addGoal(9, new PathfinderGoalRandomLookaround(this));
        this.randomStrollGoal.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        moveTowardsRestrictionGoal.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityLiving.class, 10, true, false, new EntityGuardian.EntitySelectorGuardianTargetHumanSquid(this)));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.ATTACK_DAMAGE, 6.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.5D).add(GenericAttributes.FOLLOW_RANGE, 16.0D).add(GenericAttributes.MAX_HEALTH, 30.0D);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new NavigationGuardian(this, world);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_MOVING, false);
        this.entityData.register(DATA_ID_ATTACK_TARGET, 0);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.WATER;
    }

    public boolean isMoving() {
        return this.entityData.get(DATA_ID_MOVING);
    }

    void setMoving(boolean retracted) {
        this.entityData.set(DATA_ID_MOVING, retracted);
    }

    public int getAttackDuration() {
        return 80;
    }

    void setActiveAttackTarget(int entityId) {
        this.entityData.set(DATA_ID_ATTACK_TARGET, entityId);
    }

    public boolean hasActiveAttackTarget() {
        return this.entityData.get(DATA_ID_ATTACK_TARGET) != 0;
    }

    @Nullable
    public EntityLiving getActiveAttackTarget() {
        if (!this.hasActiveAttackTarget()) {
            return null;
        } else if (this.level.isClientSide) {
            if (this.clientSideCachedAttackTarget != null) {
                return this.clientSideCachedAttackTarget;
            } else {
                Entity entity = this.level.getEntity(this.entityData.get(DATA_ID_ATTACK_TARGET));
                if (entity instanceof EntityLiving) {
                    this.clientSideCachedAttackTarget = (EntityLiving)entity;
                    return this.clientSideCachedAttackTarget;
                } else {
                    return null;
                }
            }
        } else {
            return this.getGoalTarget();
        }
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        super.onSyncedDataUpdated(data);
        if (DATA_ID_ATTACK_TARGET.equals(data)) {
            this.clientSideAttackTime = 0;
            this.clientSideCachedAttackTarget = null;
        }

    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isInWaterOrBubble() ? SoundEffects.GUARDIAN_AMBIENT : SoundEffects.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isInWaterOrBubble() ? SoundEffects.GUARDIAN_HURT : SoundEffects.GUARDIAN_HURT_LAND;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isInWaterOrBubble() ? SoundEffects.GUARDIAN_DEATH : SoundEffects.GUARDIAN_DEATH_LAND;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return world.getFluid(pos).is(TagsFluid.WATER) ? 10.0F + world.getBrightness(pos) - 0.5F : super.getWalkTargetValue(pos, world);
    }

    @Override
    public void movementTick() {
        if (this.isAlive()) {
            if (this.level.isClientSide) {
                this.clientSideTailAnimationO = this.clientSideTailAnimation;
                if (!this.isInWater()) {
                    this.clientSideTailAnimationSpeed = 2.0F;
                    Vec3D vec3 = this.getMot();
                    if (vec3.y > 0.0D && this.clientSideTouchedGround && !this.isSilent()) {
                        this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), this.getSoundFlop(), this.getSoundCategory(), 1.0F, 1.0F, false);
                    }

                    this.clientSideTouchedGround = vec3.y < 0.0D && this.level.loadedAndEntityCanStandOn(this.getChunkCoordinates().below(), this);
                } else if (this.isMoving()) {
                    if (this.clientSideTailAnimationSpeed < 0.5F) {
                        this.clientSideTailAnimationSpeed = 4.0F;
                    } else {
                        this.clientSideTailAnimationSpeed += (0.5F - this.clientSideTailAnimationSpeed) * 0.1F;
                    }
                } else {
                    this.clientSideTailAnimationSpeed += (0.125F - this.clientSideTailAnimationSpeed) * 0.2F;
                }

                this.clientSideTailAnimation += this.clientSideTailAnimationSpeed;
                this.clientSideSpikesAnimationO = this.clientSideSpikesAnimation;
                if (!this.isInWaterOrBubble()) {
                    this.clientSideSpikesAnimation = this.random.nextFloat();
                } else if (this.isMoving()) {
                    this.clientSideSpikesAnimation += (0.0F - this.clientSideSpikesAnimation) * 0.25F;
                } else {
                    this.clientSideSpikesAnimation += (1.0F - this.clientSideSpikesAnimation) * 0.06F;
                }

                if (this.isMoving() && this.isInWater()) {
                    Vec3D vec32 = this.getViewVector(0.0F);

                    for(int i = 0; i < 2; ++i) {
                        this.level.addParticle(Particles.BUBBLE, this.getRandomX(0.5D) - vec32.x * 1.5D, this.getRandomY() - vec32.y * 1.5D, this.getRandomZ(0.5D) - vec32.z * 1.5D, 0.0D, 0.0D, 0.0D);
                    }
                }

                if (this.hasActiveAttackTarget()) {
                    if (this.clientSideAttackTime < this.getAttackDuration()) {
                        ++this.clientSideAttackTime;
                    }

                    EntityLiving livingEntity = this.getActiveAttackTarget();
                    if (livingEntity != null) {
                        this.getControllerLook().setLookAt(livingEntity, 90.0F, 90.0F);
                        this.getControllerLook().tick();
                        double d = (double)this.getAttackAnimationScale(0.0F);
                        double e = livingEntity.locX() - this.locX();
                        double f = livingEntity.getY(0.5D) - this.getHeadY();
                        double g = livingEntity.locZ() - this.locZ();
                        double h = Math.sqrt(e * e + f * f + g * g);
                        e = e / h;
                        f = f / h;
                        g = g / h;
                        double j = this.random.nextDouble();

                        while(j < h) {
                            j += 1.8D - d + this.random.nextDouble() * (1.7D - d);
                            this.level.addParticle(Particles.BUBBLE, this.locX() + e * j, this.getHeadY() + f * j, this.locZ() + g * j, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
            }

            if (this.isInWaterOrBubble()) {
                this.setAirTicks(300);
            } else if (this.onGround) {
                this.setMot(this.getMot().add((double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F), 0.5D, (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F)));
                this.setYRot(this.random.nextFloat() * 360.0F);
                this.onGround = false;
                this.hasImpulse = true;
            }

            if (this.hasActiveAttackTarget()) {
                this.setYRot(this.yHeadRot);
            }
        }

        super.movementTick();
    }

    protected SoundEffect getSoundFlop() {
        return SoundEffects.GUARDIAN_FLOP;
    }

    public float getTailAnimation(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.clientSideTailAnimationO, this.clientSideTailAnimation);
    }

    public float getSpikesAnimation(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.clientSideSpikesAnimationO, this.clientSideSpikesAnimation);
    }

    public float getAttackAnimationScale(float tickDelta) {
        return ((float)this.clientSideAttackTime + tickDelta) / (float)this.getAttackDuration();
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this);
    }

    public static boolean checkGuardianSpawnRules(EntityTypes<? extends EntityGuardian> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return (random.nextInt(20) == 0 || !world.canSeeSkyFromBelowWater(pos)) && world.getDifficulty() != EnumDifficulty.PEACEFUL && (spawnReason == EnumMobSpawn.SPAWNER || world.getFluid(pos).is(TagsFluid.WATER));
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (!this.isMoving() && !source.isMagic() && source.getDirectEntity() instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)source.getDirectEntity();
            if (!source.isExplosion()) {
                livingEntity.damageEntity(DamageSource.thorns(this), 2.0F);
            }
        }

        if (this.randomStrollGoal != null) {
            this.randomStrollGoal.trigger();
        }

        return super.damageEntity(source, amount);
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater()) {
            this.moveRelative(0.1F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.9D));
            if (!this.isMoving() && this.getGoalTarget() == null) {
                this.setMot(this.getMot().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(movementInput);
        }

    }

    static class ControllerMoveGuardian extends ControllerMove {
        private final EntityGuardian guardian;

        public ControllerMoveGuardian(EntityGuardian guardian) {
            super(guardian);
            this.guardian = guardian;
        }

        @Override
        public void tick() {
            if (this.operation == ControllerMove.Operation.MOVE_TO && !this.guardian.getNavigation().isDone()) {
                Vec3D vec3 = new Vec3D(this.wantedX - this.guardian.locX(), this.wantedY - this.guardian.locY(), this.wantedZ - this.guardian.locZ());
                double d = vec3.length();
                double e = vec3.x / d;
                double f = vec3.y / d;
                double g = vec3.z / d;
                float h = (float)(MathHelper.atan2(vec3.z, vec3.x) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.guardian.setYRot(this.rotlerp(this.guardian.getYRot(), h, 90.0F));
                this.guardian.yBodyRot = this.guardian.getYRot();
                float i = (float)(this.speedModifier * this.guardian.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                float j = MathHelper.lerp(0.125F, this.guardian.getSpeed(), i);
                this.guardian.setSpeed(j);
                double k = Math.sin((double)(this.guardian.tickCount + this.guardian.getId()) * 0.5D) * 0.05D;
                double l = Math.cos((double)(this.guardian.getYRot() * ((float)Math.PI / 180F)));
                double m = Math.sin((double)(this.guardian.getYRot() * ((float)Math.PI / 180F)));
                double n = Math.sin((double)(this.guardian.tickCount + this.guardian.getId()) * 0.75D) * 0.05D;
                this.guardian.setMot(this.guardian.getMot().add(k * l, n * (m + l) * 0.25D + (double)j * f * 0.1D, k * m));
                ControllerLook lookControl = this.guardian.getControllerLook();
                double o = this.guardian.locX() + e * 2.0D;
                double p = this.guardian.getHeadY() + f / d;
                double q = this.guardian.locZ() + g * 2.0D;
                double r = lookControl.getWantedX();
                double s = lookControl.getWantedY();
                double t = lookControl.getWantedZ();
                if (!lookControl.isHasWanted()) {
                    r = o;
                    s = p;
                    t = q;
                }

                this.guardian.getControllerLook().setLookAt(MathHelper.lerp(0.125D, r, o), MathHelper.lerp(0.125D, s, p), MathHelper.lerp(0.125D, t, q), 10.0F, 40.0F);
                this.guardian.setMoving(true);
            } else {
                this.guardian.setSpeed(0.0F);
                this.guardian.setMoving(false);
            }
        }
    }

    static class EntitySelectorGuardianTargetHumanSquid implements Predicate<EntityLiving> {
        private final EntityGuardian guardian;

        public EntitySelectorGuardianTargetHumanSquid(EntityGuardian owner) {
            this.guardian = owner;
        }

        @Override
        public boolean test(@Nullable EntityLiving livingEntity) {
            return (livingEntity instanceof EntityHuman || livingEntity instanceof EntitySquid || livingEntity instanceof EntityAxolotl) && livingEntity.distanceToSqr(this.guardian) > 9.0D;
        }
    }

    static class PathfinderGoalGuardianAttack extends PathfinderGoal {
        private final EntityGuardian guardian;
        private int attackTime;
        private final boolean elder;

        public PathfinderGoalGuardianAttack(EntityGuardian guardian) {
            this.guardian = guardian;
            this.elder = guardian instanceof EntityGuardianElder;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = this.guardian.getGoalTarget();
            return livingEntity != null && livingEntity.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && (this.elder || this.guardian.distanceToSqr(this.guardian.getGoalTarget()) > 9.0D);
        }

        @Override
        public void start() {
            this.attackTime = -10;
            this.guardian.getNavigation().stop();
            this.guardian.getControllerLook().setLookAt(this.guardian.getGoalTarget(), 90.0F, 90.0F);
            this.guardian.hasImpulse = true;
        }

        @Override
        public void stop() {
            this.guardian.setActiveAttackTarget(0);
            this.guardian.setGoalTarget((EntityLiving)null);
            this.guardian.randomStrollGoal.trigger();
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = this.guardian.getGoalTarget();
            this.guardian.getNavigation().stop();
            this.guardian.getControllerLook().setLookAt(livingEntity, 90.0F, 90.0F);
            if (!this.guardian.hasLineOfSight(livingEntity)) {
                this.guardian.setGoalTarget((EntityLiving)null);
            } else {
                ++this.attackTime;
                if (this.attackTime == 0) {
                    this.guardian.setActiveAttackTarget(this.guardian.getGoalTarget().getId());
                    if (!this.guardian.isSilent()) {
                        this.guardian.level.broadcastEntityEffect(this.guardian, (byte)21);
                    }
                } else if (this.attackTime >= this.guardian.getAttackDuration()) {
                    float f = 1.0F;
                    if (this.guardian.level.getDifficulty() == EnumDifficulty.HARD) {
                        f += 2.0F;
                    }

                    if (this.elder) {
                        f += 2.0F;
                    }

                    livingEntity.damageEntity(DamageSource.indirectMagic(this.guardian, this.guardian), f);
                    livingEntity.damageEntity(DamageSource.mobAttack(this.guardian), (float)this.guardian.getAttributeValue(GenericAttributes.ATTACK_DAMAGE));
                    this.guardian.setGoalTarget((EntityLiving)null);
                }

                super.tick();
            }
        }
    }
}
