package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.phys.Vec3D;

public class EntitySlime extends EntityInsentient implements IMonster {
    private static final DataWatcherObject<Integer> ID_SIZE = DataWatcher.defineId(EntitySlime.class, DataWatcherRegistry.INT);
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 127;
    public float targetSquish;
    public float squish;
    public float oSquish;
    private boolean wasOnGround;

    public EntitySlime(EntityTypes<? extends EntitySlime> type, World world) {
        super(type, world);
        this.moveControl = new EntitySlime.ControllerMoveSlime(this);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new EntitySlime.PathfinderGoalSlimeRandomJump(this));
        this.goalSelector.addGoal(2, new EntitySlime.PathfinderGoalSlimeNearestPlayer(this));
        this.goalSelector.addGoal(3, new EntitySlime.PathfinderGoalSlimeRandomDirection(this));
        this.goalSelector.addGoal(5, new EntitySlime.PathfinderGoalSlimeIdle(this));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, (livingEntity) -> {
            return Math.abs(livingEntity.locY() - this.locY()) <= 4.0D;
        }));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(ID_SIZE, 1);
    }

    public void setSize(int size, boolean heal) {
        int i = MathHelper.clamp(size, 1, 127);
        this.entityData.set(ID_SIZE, i);
        this.reapplyPosition();
        this.updateSize();
        this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue((double)(i * i));
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((double)(0.2F + 0.1F * (float)i));
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue((double)i);
        if (heal) {
            this.setHealth(this.getMaxHealth());
        }

        this.xpReward = i;
    }

    public int getSize() {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Size", this.getSize() - 1);
        nbt.setBoolean("wasOnGround", this.wasOnGround);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.setSize(nbt.getInt("Size") + 1, false);
        super.loadData(nbt);
        this.wasOnGround = nbt.getBoolean("wasOnGround");
    }

    public boolean isTiny() {
        return this.getSize() <= 1;
    }

    protected ParticleParam getParticleType() {
        return Particles.ITEM_SLIME;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return this.getSize() > 0;
    }

    @Override
    public void tick() {
        this.squish += (this.targetSquish - this.squish) * 0.5F;
        this.oSquish = this.squish;
        super.tick();
        if (this.onGround && !this.wasOnGround) {
            int i = this.getSize();

            for(int j = 0; j < i * 8; ++j) {
                float f = this.random.nextFloat() * ((float)Math.PI * 2F);
                float g = this.random.nextFloat() * 0.5F + 0.5F;
                float h = MathHelper.sin(f) * (float)i * 0.5F * g;
                float k = MathHelper.cos(f) * (float)i * 0.5F * g;
                this.level.addParticle(this.getParticleType(), this.locX() + (double)h, this.locY(), this.locZ() + (double)k, 0.0D, 0.0D, 0.0D);
            }

            this.playSound(this.getSoundSquish(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.targetSquish = -0.5F;
        } else if (!this.onGround && this.wasOnGround) {
            this.targetSquish = 1.0F;
        }

        this.wasOnGround = this.onGround;
        this.decreaseSquish();
    }

    protected void decreaseSquish() {
        this.targetSquish *= 0.6F;
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    @Override
    public void updateSize() {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        super.updateSize();
        this.setPosition(d, e, f);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (ID_SIZE.equals(data)) {
            this.updateSize();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
            if (this.isInWater() && this.random.nextInt(20) == 0) {
                this.doWaterSplashEffect();
            }
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public EntityTypes<? extends EntitySlime> getEntityType() {
        return super.getEntityType();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        int i = this.getSize();
        if (!this.level.isClientSide && i > 1 && this.isDeadOrDying()) {
            IChatBaseComponent component = this.getCustomName();
            boolean bl = this.isNoAI();
            float f = (float)i / 4.0F;
            int j = i / 2;
            int k = 2 + this.random.nextInt(3);

            for(int l = 0; l < k; ++l) {
                float g = ((float)(l % 2) - 0.5F) * f;
                float h = ((float)(l / 2) - 0.5F) * f;
                EntitySlime slime = this.getEntityType().create(this.level);
                if (this.isPersistent()) {
                    slime.setPersistent();
                }

                slime.setCustomName(component);
                slime.setNoAI(bl);
                slime.setInvulnerable(this.isInvulnerable());
                slime.setSize(j, true);
                slime.setPositionRotation(this.locX() + (double)g, this.locY() + 0.5D, this.locZ() + (double)h, this.random.nextFloat() * 360.0F, 0.0F);
                this.level.addEntity(slime);
            }
        }

        super.remove(reason);
    }

    @Override
    public void collide(Entity entity) {
        super.collide(entity);
        if (entity instanceof EntityIronGolem && this.isDealsDamage()) {
            this.dealDamage((EntityLiving)entity);
        }

    }

    @Override
    public void pickup(EntityHuman player) {
        if (this.isDealsDamage()) {
            this.dealDamage(player);
        }

    }

    protected void dealDamage(EntityLiving target) {
        if (this.isAlive()) {
            int i = this.getSize();
            if (this.distanceToSqr(target) < 0.6D * (double)i * 0.6D * (double)i && this.hasLineOfSight(target) && target.damageEntity(DamageSource.mobAttack(this), this.getAttackDamage())) {
                this.playSound(SoundEffects.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                this.doEnchantDamageEffects(this, target);
            }
        }

    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.625F * dimensions.height;
    }

    protected boolean isDealsDamage() {
        return !this.isTiny() && this.doAITick();
    }

    protected float getAttackDamage() {
        return (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isTiny() ? SoundEffects.SLIME_HURT_SMALL : SoundEffects.SLIME_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isTiny() ? SoundEffects.SLIME_DEATH_SMALL : SoundEffects.SLIME_DEATH;
    }

    protected SoundEffect getSoundSquish() {
        return this.isTiny() ? SoundEffects.SLIME_SQUISH_SMALL : SoundEffects.SLIME_SQUISH;
    }

    @Override
    protected MinecraftKey getDefaultLootTable() {
        return this.getSize() == 1 ? this.getEntityType().getDefaultLootTable() : LootTables.EMPTY;
    }

    public static boolean checkSlimeSpawnRules(EntityTypes<EntitySlime> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL) {
            if (Objects.equals(world.getBiomeName(pos), Optional.of(Biomes.SWAMP)) && pos.getY() > 50 && pos.getY() < 70 && random.nextFloat() < 0.5F && random.nextFloat() < world.getMoonBrightness() && world.getLightLevel(pos) <= random.nextInt(8)) {
                return checkMobSpawnRules(type, world, spawnReason, pos, random);
            }

            if (!(world instanceof GeneratorAccessSeed)) {
                return false;
            }

            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(pos);
            boolean bl = SeededRandom.seedSlimeChunk(chunkPos.x, chunkPos.z, ((GeneratorAccessSeed)world).getSeed(), 987234911L).nextInt(10) == 0;
            if (random.nextInt(10) == 0 && bl && pos.getY() < 40) {
                return checkMobSpawnRules(type, world, spawnReason, pos, random);
            }
        }

        return false;
    }

    @Override
    public float getSoundVolume() {
        return 0.4F * (float)this.getSize();
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }

    protected boolean doPlayJumpSound() {
        return this.getSize() > 0;
    }

    @Override
    protected void jump() {
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x, (double)this.getJumpPower(), vec3.z);
        this.hasImpulse = true;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        int i = this.random.nextInt(3);
        if (i < 2 && this.random.nextFloat() < 0.5F * difficulty.getSpecialMultiplier()) {
            ++i;
        }

        int j = 1 << i;
        this.setSize(j, true);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    float getSoundPitch() {
        float f = this.isTiny() ? 1.4F : 0.8F;
        return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * f;
    }

    protected SoundEffect getSoundJump() {
        return this.isTiny() ? SoundEffects.SLIME_JUMP_SMALL : SoundEffects.SLIME_JUMP;
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return super.getDimensions(pose).scale(0.255F * (float)this.getSize());
    }

    static class ControllerMoveSlime extends ControllerMove {
        private float yRot;
        private int jumpDelay;
        private final EntitySlime slime;
        private boolean isAggressive;

        public ControllerMoveSlime(EntitySlime slime) {
            super(slime);
            this.slime = slime;
            this.yRot = 180.0F * slime.getYRot() / (float)Math.PI;
        }

        public void setDirection(float targetYaw, boolean jumpOften) {
            this.yRot = targetYaw;
            this.isAggressive = jumpOften;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = ControllerMove.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();
            if (this.operation != ControllerMove.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = ControllerMove.Operation.WAIT;
                if (this.mob.isOnGround()) {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = this.slime.getJumpDelay();
                        if (this.isAggressive) {
                            this.jumpDelay /= 3;
                        }

                        this.slime.getControllerJump().jump();
                        if (this.slime.doPlayJumpSound()) {
                            this.slime.playSound(this.slime.getSoundJump(), this.slime.getSoundVolume(), this.slime.getSoundPitch());
                        }
                    } else {
                        this.slime.xxa = 0.0F;
                        this.slime.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED)));
                }

            }
        }
    }

    static class PathfinderGoalSlimeIdle extends PathfinderGoal {
        private final EntitySlime slime;

        public PathfinderGoalSlimeIdle(EntitySlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            return !this.slime.isPassenger();
        }

        @Override
        public void tick() {
            ((EntitySlime.ControllerMoveSlime)this.slime.getControllerMove()).setWantedMovement(1.0D);
        }
    }

    static class PathfinderGoalSlimeNearestPlayer extends PathfinderGoal {
        private final EntitySlime slime;
        private int growTiredTimer;

        public PathfinderGoalSlimeNearestPlayer(EntitySlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = this.slime.getGoalTarget();
            if (livingEntity == null) {
                return false;
            } else {
                return !this.slime.canAttack(livingEntity) ? false : this.slime.getControllerMove() instanceof EntitySlime.ControllerMoveSlime;
            }
        }

        @Override
        public void start() {
            this.growTiredTimer = reducedTickDelay(300);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            EntityLiving livingEntity = this.slime.getGoalTarget();
            if (livingEntity == null) {
                return false;
            } else if (!this.slime.canAttack(livingEntity)) {
                return false;
            } else {
                return --this.growTiredTimer > 0;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = this.slime.getGoalTarget();
            if (livingEntity != null) {
                this.slime.lookAt(livingEntity, 10.0F, 10.0F);
            }

            ((EntitySlime.ControllerMoveSlime)this.slime.getControllerMove()).setDirection(this.slime.getYRot(), this.slime.isDealsDamage());
        }
    }

    static class PathfinderGoalSlimeRandomDirection extends PathfinderGoal {
        private final EntitySlime slime;
        private float chosenDegrees;
        private int nextRandomizeTime;

        public PathfinderGoalSlimeRandomDirection(EntitySlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.slime.getGoalTarget() == null && (this.slime.onGround || this.slime.isInWater() || this.slime.isInLava() || this.slime.hasEffect(MobEffectList.LEVITATION)) && this.slime.getControllerMove() instanceof EntitySlime.ControllerMoveSlime;
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + this.slime.getRandom().nextInt(60));
                this.chosenDegrees = (float)this.slime.getRandom().nextInt(360);
            }

            ((EntitySlime.ControllerMoveSlime)this.slime.getControllerMove()).setDirection(this.chosenDegrees, false);
        }
    }

    static class PathfinderGoalSlimeRandomJump extends PathfinderGoal {
        private final EntitySlime slime;

        public PathfinderGoalSlimeRandomJump(EntitySlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
            slime.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (this.slime.isInWater() || this.slime.isInLava()) && this.slime.getControllerMove() instanceof EntitySlime.ControllerMoveSlime;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.slime.getRandom().nextFloat() < 0.8F) {
                this.slime.getControllerJump().jump();
            }

            ((EntitySlime.ControllerMoveSlime)this.slime.getControllerMove()).setWantedMovement(1.2D);
        }
    }
}
