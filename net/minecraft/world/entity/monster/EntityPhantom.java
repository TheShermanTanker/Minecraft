package net.minecraft.world.entity.monster;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFlying;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.control.EntityAIBodyControl;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;

public class EntityPhantom extends EntityFlying implements IMonster {
    public static final float FLAP_DEGREES_PER_TICK = 7.448451F;
    public static final int TICKS_PER_FLAP = MathHelper.ceil(24.166098F);
    private static final DataWatcherObject<Integer> ID_SIZE = DataWatcher.defineId(EntityPhantom.class, DataWatcherRegistry.INT);
    Vec3D moveTargetPoint = Vec3D.ZERO;
    BlockPosition anchorPoint = BlockPosition.ZERO;
    EntityPhantom.AttackPhase attackPhase = EntityPhantom.AttackPhase.CIRCLE;

    public EntityPhantom(EntityTypes<? extends EntityPhantom> type, World world) {
        super(type, world);
        this.xpReward = 5;
        this.moveControl = new EntityPhantom.ControllerMovePhantom(this);
        this.lookControl = new EntityPhantom.ControllerLookPhantom(this);
    }

    @Override
    public boolean isFlapping() {
        return (this.getUniqueFlapTickOffset() + this.tickCount) % TICKS_PER_FLAP == 0;
    }

    @Override
    protected EntityAIBodyControl createBodyControl() {
        return new EntityPhantom.EntityPhantomBodyControl(this);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new EntityPhantom.PathfinderGoalPhantomAttackStrategy());
        this.goalSelector.addGoal(2, new EntityPhantom.PathfinderGoalPhantomSweepAttack());
        this.goalSelector.addGoal(3, new EntityPhantom.PathfinderGoalPhantomCircleAroundAnchor());
        this.targetSelector.addGoal(1, new EntityPhantom.PathfinderGoalPhantomTargetPlayer());
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(ID_SIZE, 0);
    }

    public void setSize(int size) {
        this.entityData.set(ID_SIZE, MathHelper.clamp(size, 0, 64));
    }

    private void updatePhantomSizeInfo() {
        this.updateSize();
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue((double)(6 + this.getSize()));
    }

    public int getSize() {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.35F;
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (ID_SIZE.equals(data)) {
            this.updatePhantomSizeInfo();
        }

        super.onSyncedDataUpdated(data);
    }

    public int getUniqueFlapTickOffset() {
        return this.getId() * 3;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            float f = MathHelper.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount) * 7.448451F * ((float)Math.PI / 180F) + (float)Math.PI);
            float g = MathHelper.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount + 1) * 7.448451F * ((float)Math.PI / 180F) + (float)Math.PI);
            if (f > 0.0F && g <= 0.0F) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.PHANTOM_FLAP, this.getSoundCategory(), 0.95F + this.random.nextFloat() * 0.05F, 0.95F + this.random.nextFloat() * 0.05F, false);
            }

            int i = this.getSize();
            float h = MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F)) * (1.3F + 0.21F * (float)i);
            float j = MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)) * (1.3F + 0.21F * (float)i);
            float k = (0.3F + f * 0.45F) * ((float)i * 0.2F + 1.0F);
            this.level.addParticle(Particles.MYCELIUM, this.locX() + (double)h, this.locY() + (double)k, this.locZ() + (double)j, 0.0D, 0.0D, 0.0D);
            this.level.addParticle(Particles.MYCELIUM, this.locX() - (double)h, this.locY() + (double)k, this.locZ() - (double)j, 0.0D, 0.0D, 0.0D);
        }

    }

    @Override
    public void movementTick() {
        if (this.isAlive() && this.isSunBurnTick()) {
            this.setOnFire(8);
        }

        super.movementTick();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.anchorPoint = this.getChunkCoordinates().above(5);
        this.setSize(0);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("AX")) {
            this.anchorPoint = new BlockPosition(nbt.getInt("AX"), nbt.getInt("AY"), nbt.getInt("AZ"));
        }

        this.setSize(nbt.getInt("Size"));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("AX", this.anchorPoint.getX());
        nbt.setInt("AY", this.anchorPoint.getY());
        nbt.setInt("AZ", this.anchorPoint.getZ());
        nbt.setInt("Size", this.getSize());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PHANTOM_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PHANTOM_DEATH;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    public float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public boolean canAttackType(EntityTypes<?> type) {
        return true;
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        int i = this.getSize();
        EntitySize entityDimensions = super.getDimensions(pose);
        float f = (entityDimensions.width + 0.2F * (float)i) / entityDimensions.width;
        return entityDimensions.scale(f);
    }

    static enum AttackPhase {
        CIRCLE,
        SWOOP;
    }

    class ControllerLookPhantom extends ControllerLook {
        public ControllerLookPhantom(EntityInsentient entity) {
            super(entity);
        }

        @Override
        public void tick() {
        }
    }

    class ControllerMovePhantom extends ControllerMove {
        private float speed = 0.1F;

        public ControllerMovePhantom(EntityInsentient owner) {
            super(owner);
        }

        @Override
        public void tick() {
            if (EntityPhantom.this.horizontalCollision) {
                EntityPhantom.this.setYRot(EntityPhantom.this.getYRot() + 180.0F);
                this.speed = 0.1F;
            }

            float f = (float)(EntityPhantom.this.moveTargetPoint.x - EntityPhantom.this.locX());
            float g = (float)(EntityPhantom.this.moveTargetPoint.y - EntityPhantom.this.locY());
            float h = (float)(EntityPhantom.this.moveTargetPoint.z - EntityPhantom.this.locZ());
            double d = (double)MathHelper.sqrt(f * f + h * h);
            if (Math.abs(d) > (double)1.0E-5F) {
                double e = 1.0D - (double)MathHelper.abs(g * 0.7F) / d;
                f = (float)((double)f * e);
                h = (float)((double)h * e);
                d = (double)MathHelper.sqrt(f * f + h * h);
                double i = (double)MathHelper.sqrt(f * f + h * h + g * g);
                float j = EntityPhantom.this.getYRot();
                float k = (float)MathHelper.atan2((double)h, (double)f);
                float l = MathHelper.wrapDegrees(EntityPhantom.this.getYRot() + 90.0F);
                float m = MathHelper.wrapDegrees(k * (180F / (float)Math.PI));
                EntityPhantom.this.setYRot(MathHelper.approachDegrees(l, m, 4.0F) - 90.0F);
                EntityPhantom.this.yBodyRot = EntityPhantom.this.getYRot();
                if (MathHelper.degreesDifferenceAbs(j, EntityPhantom.this.getYRot()) < 3.0F) {
                    this.speed = MathHelper.approach(this.speed, 1.8F, 0.005F * (1.8F / this.speed));
                } else {
                    this.speed = MathHelper.approach(this.speed, 0.2F, 0.025F);
                }

                float n = (float)(-(MathHelper.atan2((double)(-g), d) * (double)(180F / (float)Math.PI)));
                EntityPhantom.this.setXRot(n);
                float o = EntityPhantom.this.getYRot() + 90.0F;
                double p = (double)(this.speed * MathHelper.cos(o * ((float)Math.PI / 180F))) * Math.abs((double)f / i);
                double q = (double)(this.speed * MathHelper.sin(o * ((float)Math.PI / 180F))) * Math.abs((double)h / i);
                double r = (double)(this.speed * MathHelper.sin(n * ((float)Math.PI / 180F))) * Math.abs((double)g / i);
                Vec3D vec3 = EntityPhantom.this.getMot();
                EntityPhantom.this.setMot(vec3.add((new Vec3D(p, r, q)).subtract(vec3).scale(0.2D)));
            }

        }
    }

    class EntityPhantomBodyControl extends EntityAIBodyControl {
        public EntityPhantomBodyControl(EntityInsentient entity) {
            super(entity);
        }

        @Override
        public void clientTick() {
            EntityPhantom.this.yHeadRot = EntityPhantom.this.yBodyRot;
            EntityPhantom.this.yBodyRot = EntityPhantom.this.getYRot();
        }
    }

    class PathfinderGoalPhantomAttackStrategy extends PathfinderGoal {
        private int nextSweepTick;

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = EntityPhantom.this.getGoalTarget();
            return livingEntity != null ? EntityPhantom.this.canAttack(EntityPhantom.this.getGoalTarget(), PathfinderTargetCondition.DEFAULT) : false;
        }

        @Override
        public void start() {
            this.nextSweepTick = 10;
            EntityPhantom.this.attackPhase = EntityPhantom.AttackPhase.CIRCLE;
            this.setAnchorAboveTarget();
        }

        @Override
        public void stop() {
            EntityPhantom.this.anchorPoint = EntityPhantom.this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, EntityPhantom.this.anchorPoint).above(10 + EntityPhantom.this.random.nextInt(20));
        }

        @Override
        public void tick() {
            if (EntityPhantom.this.attackPhase == EntityPhantom.AttackPhase.CIRCLE) {
                --this.nextSweepTick;
                if (this.nextSweepTick <= 0) {
                    EntityPhantom.this.attackPhase = EntityPhantom.AttackPhase.SWOOP;
                    this.setAnchorAboveTarget();
                    this.nextSweepTick = (8 + EntityPhantom.this.random.nextInt(4)) * 20;
                    EntityPhantom.this.playSound(SoundEffects.PHANTOM_SWOOP, 10.0F, 0.95F + EntityPhantom.this.random.nextFloat() * 0.1F);
                }
            }

        }

        private void setAnchorAboveTarget() {
            EntityPhantom.this.anchorPoint = EntityPhantom.this.getGoalTarget().getChunkCoordinates().above(20 + EntityPhantom.this.random.nextInt(20));
            if (EntityPhantom.this.anchorPoint.getY() < EntityPhantom.this.level.getSeaLevel()) {
                EntityPhantom.this.anchorPoint = new BlockPosition(EntityPhantom.this.anchorPoint.getX(), EntityPhantom.this.level.getSeaLevel() + 1, EntityPhantom.this.anchorPoint.getZ());
            }

        }
    }

    class PathfinderGoalPhantomCircleAroundAnchor extends EntityPhantom.PathfinderGoalPhantomMoveTarget {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        @Override
        public boolean canUse() {
            return EntityPhantom.this.getGoalTarget() == null || EntityPhantom.this.attackPhase == EntityPhantom.AttackPhase.CIRCLE;
        }

        @Override
        public void start() {
            this.distance = 5.0F + EntityPhantom.this.random.nextFloat() * 10.0F;
            this.height = -4.0F + EntityPhantom.this.random.nextFloat() * 9.0F;
            this.clockwise = EntityPhantom.this.random.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        @Override
        public void tick() {
            if (EntityPhantom.this.random.nextInt(350) == 0) {
                this.height = -4.0F + EntityPhantom.this.random.nextFloat() * 9.0F;
            }

            if (EntityPhantom.this.random.nextInt(250) == 0) {
                ++this.distance;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.clockwise = -this.clockwise;
                }
            }

            if (EntityPhantom.this.random.nextInt(450) == 0) {
                this.angle = EntityPhantom.this.random.nextFloat() * 2.0F * (float)Math.PI;
                this.selectNext();
            }

            if (this.touchingTarget()) {
                this.selectNext();
            }

            if (EntityPhantom.this.moveTargetPoint.y < EntityPhantom.this.locY() && !EntityPhantom.this.level.isEmpty(EntityPhantom.this.getChunkCoordinates().below(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }

            if (EntityPhantom.this.moveTargetPoint.y > EntityPhantom.this.locY() && !EntityPhantom.this.level.isEmpty(EntityPhantom.this.getChunkCoordinates().above(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }

        }

        private void selectNext() {
            if (BlockPosition.ZERO.equals(EntityPhantom.this.anchorPoint)) {
                EntityPhantom.this.anchorPoint = EntityPhantom.this.getChunkCoordinates();
            }

            this.angle += this.clockwise * 15.0F * ((float)Math.PI / 180F);
            EntityPhantom.this.moveTargetPoint = Vec3D.atLowerCornerOf(EntityPhantom.this.anchorPoint).add((double)(this.distance * MathHelper.cos(this.angle)), (double)(-4.0F + this.height), (double)(this.distance * MathHelper.sin(this.angle)));
        }
    }

    abstract class PathfinderGoalPhantomMoveTarget extends PathfinderGoal {
        public PathfinderGoalPhantomMoveTarget() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        protected boolean touchingTarget() {
            return EntityPhantom.this.moveTargetPoint.distanceToSqr(EntityPhantom.this.locX(), EntityPhantom.this.locY(), EntityPhantom.this.locZ()) < 4.0D;
        }
    }

    class PathfinderGoalPhantomSweepAttack extends EntityPhantom.PathfinderGoalPhantomMoveTarget {
        @Override
        public boolean canUse() {
            return EntityPhantom.this.getGoalTarget() != null && EntityPhantom.this.attackPhase == EntityPhantom.AttackPhase.SWOOP;
        }

        @Override
        public boolean canContinueToUse() {
            EntityLiving livingEntity = EntityPhantom.this.getGoalTarget();
            if (livingEntity == null) {
                return false;
            } else if (!livingEntity.isAlive()) {
                return false;
            } else if (!(livingEntity instanceof EntityHuman) || !((EntityHuman)livingEntity).isSpectator() && !((EntityHuman)livingEntity).isCreative()) {
                if (!this.canUse()) {
                    return false;
                } else {
                    if (EntityPhantom.this.tickCount % 20 == 0) {
                        List<EntityCat> list = EntityPhantom.this.level.getEntitiesOfClass(EntityCat.class, EntityPhantom.this.getBoundingBox().inflate(16.0D), IEntitySelector.ENTITY_STILL_ALIVE);
                        if (!list.isEmpty()) {
                            for(EntityCat cat : list) {
                                cat.hiss();
                            }

                            return false;
                        }
                    }

                    return true;
                }
            } else {
                return false;
            }
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            EntityPhantom.this.setGoalTarget((EntityLiving)null);
            EntityPhantom.this.attackPhase = EntityPhantom.AttackPhase.CIRCLE;
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = EntityPhantom.this.getGoalTarget();
            EntityPhantom.this.moveTargetPoint = new Vec3D(livingEntity.locX(), livingEntity.getY(0.5D), livingEntity.locZ());
            if (EntityPhantom.this.getBoundingBox().inflate((double)0.2F).intersects(livingEntity.getBoundingBox())) {
                EntityPhantom.this.attackEntity(livingEntity);
                EntityPhantom.this.attackPhase = EntityPhantom.AttackPhase.CIRCLE;
                if (!EntityPhantom.this.isSilent()) {
                    EntityPhantom.this.level.triggerEffect(1039, EntityPhantom.this.getChunkCoordinates(), 0);
                }
            } else if (EntityPhantom.this.horizontalCollision || EntityPhantom.this.hurtTime > 0) {
                EntityPhantom.this.attackPhase = EntityPhantom.AttackPhase.CIRCLE;
            }

        }
    }

    class PathfinderGoalPhantomTargetPlayer extends PathfinderGoal {
        private final PathfinderTargetCondition attackTargeting = PathfinderTargetCondition.forCombat().range(64.0D);
        private int nextScanTick = 20;

        @Override
        public boolean canUse() {
            if (this.nextScanTick > 0) {
                --this.nextScanTick;
                return false;
            } else {
                this.nextScanTick = 60;
                List<EntityHuman> list = EntityPhantom.this.level.getNearbyPlayers(this.attackTargeting, EntityPhantom.this, EntityPhantom.this.getBoundingBox().grow(16.0D, 64.0D, 16.0D));
                if (!list.isEmpty()) {
                    list.sort(Comparator.comparing(Entity::locY).reversed());

                    for(EntityHuman player : list) {
                        if (EntityPhantom.this.canAttack(player, PathfinderTargetCondition.DEFAULT)) {
                            EntityPhantom.this.setGoalTarget(player);
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            EntityLiving livingEntity = EntityPhantom.this.getGoalTarget();
            return livingEntity != null ? EntityPhantom.this.canAttack(livingEntity, PathfinderTargetCondition.DEFAULT) : false;
        }
    }
}
