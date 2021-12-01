package net.minecraft.world.entity.boss.wither;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.BossBattleServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.BossBattle;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.IPowerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMoveFlying;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomFly;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.IRangedEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntityWither extends EntityMonster implements IPowerable, IRangedEntity {
    private static final DataWatcherObject<Integer> DATA_TARGET_A = DataWatcher.defineId(EntityWither.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_TARGET_B = DataWatcher.defineId(EntityWither.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_TARGET_C = DataWatcher.defineId(EntityWither.class, DataWatcherRegistry.INT);
    private static final List<DataWatcherObject<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
    private static final DataWatcherObject<Integer> DATA_ID_INV = DataWatcher.defineId(EntityWither.class, DataWatcherRegistry.INT);
    private static final int INVULNERABLE_TICKS = 220;
    private final float[] xRotHeads = new float[2];
    private final float[] yRotHeads = new float[2];
    private final float[] xRotOHeads = new float[2];
    private final float[] yRotOHeads = new float[2];
    private final int[] nextHeadUpdate = new int[2];
    private final int[] idleHeadUpdates = new int[2];
    private int destroyBlocksTick;
    public final BossBattleServer bossEvent = (BossBattleServer)(new BossBattleServer(this.getScoreboardDisplayName(), BossBattle.BarColor.PURPLE, BossBattle.BarStyle.PROGRESS)).setDarkenSky(true);
    private static final Predicate<EntityLiving> LIVING_ENTITY_SELECTOR = (entity) -> {
        return entity.getMonsterType() != EnumMonsterType.UNDEAD && entity.attackable();
    };
    private static final PathfinderTargetCondition TARGETING_CONDITIONS = PathfinderTargetCondition.forCombat().range(20.0D).selector(LIVING_ENTITY_SELECTOR);

    public EntityWither(EntityTypes<? extends EntityWither> type, World world) {
        super(type, world);
        this.moveControl = new ControllerMoveFlying(this, 10, false);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 50;
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        NavigationFlying flyingPathNavigation = new NavigationFlying(this, world);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new EntityWither.PathfinderGoalWitherSpawn());
        this.goalSelector.addGoal(2, new PathfinderGoalArrowAttack(this, 1.0D, 40, 20.0F));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomFly(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(this));
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityLiving.class, 0, false, false, LIVING_ENTITY_SELECTOR));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TARGET_A, 0);
        this.entityData.register(DATA_TARGET_B, 0);
        this.entityData.register(DATA_TARGET_C, 0);
        this.entityData.register(DATA_ID_INV, 0);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Invul", this.getInvul());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setInvul(nbt.getInt("Invul"));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getScoreboardDisplayName());
        }

    }

    @Override
    public void setCustomName(@Nullable IChatBaseComponent name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getScoreboardDisplayName());
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.WITHER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.WITHER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.WITHER_DEATH;
    }

    @Override
    public void movementTick() {
        Vec3D vec3 = this.getMot().multiply(1.0D, 0.6D, 1.0D);
        if (!this.level.isClientSide && this.getHeadTarget(0) > 0) {
            Entity entity = this.level.getEntity(this.getHeadTarget(0));
            if (entity != null) {
                double d = vec3.y;
                if (this.locY() < entity.locY() || !this.isPowered() && this.locY() < entity.locY() + 5.0D) {
                    d = Math.max(0.0D, d);
                    d = d + (0.3D - d * (double)0.6F);
                }

                vec3 = new Vec3D(vec3.x, d, vec3.z);
                Vec3D vec32 = new Vec3D(entity.locX() - this.locX(), 0.0D, entity.locZ() - this.locZ());
                if (vec32.horizontalDistanceSqr() > 9.0D) {
                    Vec3D vec33 = vec32.normalize();
                    vec3 = vec3.add(vec33.x * 0.3D - vec3.x * 0.6D, 0.0D, vec33.z * 0.3D - vec3.z * 0.6D);
                }
            }
        }

        this.setMot(vec3);
        if (vec3.horizontalDistanceSqr() > 0.05D) {
            this.setYRot((float)MathHelper.atan2(vec3.z, vec3.x) * (180F / (float)Math.PI) - 90.0F);
        }

        super.movementTick();

        for(int i = 0; i < 2; ++i) {
            this.yRotOHeads[i] = this.yRotHeads[i];
            this.xRotOHeads[i] = this.xRotHeads[i];
        }

        for(int j = 0; j < 2; ++j) {
            int k = this.getHeadTarget(j + 1);
            Entity entity2 = null;
            if (k > 0) {
                entity2 = this.level.getEntity(k);
            }

            if (entity2 != null) {
                double e = this.getHeadX(j + 1);
                double f = this.getHeadY(j + 1);
                double g = this.getHeadZ(j + 1);
                double h = entity2.locX() - e;
                double l = entity2.getHeadY() - f;
                double m = entity2.locZ() - g;
                double n = Math.sqrt(h * h + m * m);
                float o = (float)(MathHelper.atan2(m, h) * (double)(180F / (float)Math.PI)) - 90.0F;
                float p = (float)(-(MathHelper.atan2(l, n) * (double)(180F / (float)Math.PI)));
                this.xRotHeads[j] = this.rotlerp(this.xRotHeads[j], p, 40.0F);
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], o, 10.0F);
            } else {
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], this.yBodyRot, 10.0F);
            }
        }

        boolean bl = this.isPowered();

        for(int q = 0; q < 3; ++q) {
            double r = this.getHeadX(q);
            double s = this.getHeadY(q);
            double t = this.getHeadZ(q);
            this.level.addParticle(Particles.SMOKE, r + this.random.nextGaussian() * (double)0.3F, s + this.random.nextGaussian() * (double)0.3F, t + this.random.nextGaussian() * (double)0.3F, 0.0D, 0.0D, 0.0D);
            if (bl && this.level.random.nextInt(4) == 0) {
                this.level.addParticle(Particles.ENTITY_EFFECT, r + this.random.nextGaussian() * (double)0.3F, s + this.random.nextGaussian() * (double)0.3F, t + this.random.nextGaussian() * (double)0.3F, (double)0.7F, (double)0.7F, 0.5D);
            }
        }

        if (this.getInvul() > 0) {
            for(int u = 0; u < 3; ++u) {
                this.level.addParticle(Particles.ENTITY_EFFECT, this.locX() + this.random.nextGaussian(), this.locY() + (double)(this.random.nextFloat() * 3.3F), this.locZ() + this.random.nextGaussian(), (double)0.7F, (double)0.7F, (double)0.9F);
            }
        }

    }

    @Override
    protected void mobTick() {
        if (this.getInvul() > 0) {
            int i = this.getInvul() - 1;
            this.bossEvent.setProgress(1.0F - (float)i / 220.0F);
            if (i <= 0) {
                Explosion.Effect blockInteraction = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Explosion.Effect.DESTROY : Explosion.Effect.NONE;
                this.level.createExplosion(this, this.locX(), this.getHeadY(), this.locZ(), 7.0F, false, blockInteraction);
                if (!this.isSilent()) {
                    this.level.broadcastWorldEvent(1023, this.getChunkCoordinates(), 0);
                }
            }

            this.setInvul(i);
            if (this.tickCount % 10 == 0) {
                this.heal(10.0F);
            }

        } else {
            super.mobTick();

            for(int j = 1; j < 3; ++j) {
                if (this.tickCount >= this.nextHeadUpdate[j - 1]) {
                    this.nextHeadUpdate[j - 1] = this.tickCount + 10 + this.random.nextInt(10);
                    if (this.level.getDifficulty() == EnumDifficulty.NORMAL || this.level.getDifficulty() == EnumDifficulty.HARD) {
                        int var10001 = j - 1;
                        int var10003 = this.idleHeadUpdates[j - 1];
                        this.idleHeadUpdates[var10001] = this.idleHeadUpdates[j - 1] + 1;
                        if (var10003 > 15) {
                            float f = 10.0F;
                            float g = 5.0F;
                            double d = MathHelper.nextDouble(this.random, this.locX() - 10.0D, this.locX() + 10.0D);
                            double e = MathHelper.nextDouble(this.random, this.locY() - 5.0D, this.locY() + 5.0D);
                            double h = MathHelper.nextDouble(this.random, this.locZ() - 10.0D, this.locZ() + 10.0D);
                            this.performRangedAttack(j + 1, d, e, h, true);
                            this.idleHeadUpdates[j - 1] = 0;
                        }
                    }

                    int k = this.getHeadTarget(j);
                    if (k > 0) {
                        EntityLiving livingEntity = (EntityLiving)this.level.getEntity(k);
                        if (livingEntity != null && this.canAttack(livingEntity) && !(this.distanceToSqr(livingEntity) > 900.0D) && this.hasLineOfSight(livingEntity)) {
                            this.performRangedAttack(j + 1, livingEntity);
                            this.nextHeadUpdate[j - 1] = this.tickCount + 40 + this.random.nextInt(20);
                            this.idleHeadUpdates[j - 1] = 0;
                        } else {
                            this.setHeadTarget(j, 0);
                        }
                    } else {
                        List<EntityLiving> list = this.level.getNearbyEntities(EntityLiving.class, TARGETING_CONDITIONS, this, this.getBoundingBox().grow(20.0D, 8.0D, 20.0D));
                        if (!list.isEmpty()) {
                            EntityLiving livingEntity2 = list.get(this.random.nextInt(list.size()));
                            this.setHeadTarget(j, livingEntity2.getId());
                        }
                    }
                }
            }

            if (this.getGoalTarget() != null) {
                this.setHeadTarget(0, this.getGoalTarget().getId());
            } else {
                this.setHeadTarget(0, 0);
            }

            if (this.destroyBlocksTick > 0) {
                --this.destroyBlocksTick;
                if (this.destroyBlocksTick == 0 && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    int l = MathHelper.floor(this.locY());
                    int m = MathHelper.floor(this.locX());
                    int n = MathHelper.floor(this.locZ());
                    boolean bl = false;

                    for(int o = -1; o <= 1; ++o) {
                        for(int p = -1; p <= 1; ++p) {
                            for(int q = 0; q <= 3; ++q) {
                                int r = m + o;
                                int s = l + q;
                                int t = n + p;
                                BlockPosition blockPos = new BlockPosition(r, s, t);
                                IBlockData blockState = this.level.getType(blockPos);
                                if (canDestroy(blockState)) {
                                    bl = this.level.destroyBlock(blockPos, true, this) || bl;
                                }
                            }
                        }
                    }

                    if (bl) {
                        this.level.triggerEffect((EntityHuman)null, 1022, this.getChunkCoordinates(), 0);
                    }
                }
            }

            if (this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    public static boolean canDestroy(IBlockData block) {
        return !block.isAir() && !block.is(TagsBlock.WITHER_IMMUNE);
    }

    public void beginSpawnSequence() {
        this.setInvul(220);
        this.bossEvent.setProgress(0.0F);
        this.setHealth(this.getMaxHealth() / 3.0F);
    }

    @Override
    public void makeStuckInBlock(IBlockData state, Vec3D multiplier) {
    }

    @Override
    public void startSeenByPlayer(EntityPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(EntityPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private double getHeadX(int headIndex) {
        if (headIndex <= 0) {
            return this.locX();
        } else {
            float f = (this.yBodyRot + (float)(180 * (headIndex - 1))) * ((float)Math.PI / 180F);
            float g = MathHelper.cos(f);
            return this.locX() + (double)g * 1.3D;
        }
    }

    private double getHeadY(int headIndex) {
        return headIndex <= 0 ? this.locY() + 3.0D : this.locY() + 2.2D;
    }

    private double getHeadZ(int headIndex) {
        if (headIndex <= 0) {
            return this.locZ();
        } else {
            float f = (this.yBodyRot + (float)(180 * (headIndex - 1))) * ((float)Math.PI / 180F);
            float g = MathHelper.sin(f);
            return this.locZ() + (double)g * 1.3D;
        }
    }

    private float rotlerp(float prevAngle, float desiredAngle, float maxDifference) {
        float f = MathHelper.wrapDegrees(desiredAngle - prevAngle);
        if (f > maxDifference) {
            f = maxDifference;
        }

        if (f < -maxDifference) {
            f = -maxDifference;
        }

        return prevAngle + f;
    }

    private void performRangedAttack(int headIndex, EntityLiving target) {
        this.performRangedAttack(headIndex, target.locX(), target.locY() + (double)target.getHeadHeight() * 0.5D, target.locZ(), headIndex == 0 && this.random.nextFloat() < 0.001F);
    }

    private void performRangedAttack(int headIndex, double targetX, double targetY, double targetZ, boolean charged) {
        if (!this.isSilent()) {
            this.level.triggerEffect((EntityHuman)null, 1024, this.getChunkCoordinates(), 0);
        }

        double d = this.getHeadX(headIndex);
        double e = this.getHeadY(headIndex);
        double f = this.getHeadZ(headIndex);
        double g = targetX - d;
        double h = targetY - e;
        double i = targetZ - f;
        EntityWitherSkull witherSkull = new EntityWitherSkull(this.level, this, g, h, i);
        witherSkull.setShooter(this);
        if (charged) {
            witherSkull.setCharged(true);
        }

        witherSkull.setPositionRaw(d, e, f);
        this.level.addEntity(witherSkull);
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        this.performRangedAttack(0, target);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (source != DamageSource.DROWN && !(source.getEntity() instanceof EntityWither)) {
            if (this.getInvul() > 0 && source != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                if (this.isPowered()) {
                    Entity entity = source.getDirectEntity();
                    if (entity instanceof EntityArrow) {
                        return false;
                    }
                }

                Entity entity2 = source.getEntity();
                if (entity2 != null && !(entity2 instanceof EntityHuman) && entity2 instanceof EntityLiving && ((EntityLiving)entity2).getMonsterType() == this.getMonsterType()) {
                    return false;
                } else {
                    if (this.destroyBlocksTick <= 0) {
                        this.destroyBlocksTick = 20;
                    }

                    for(int i = 0; i < this.idleHeadUpdates.length; ++i) {
                        this.idleHeadUpdates[i] += 3;
                    }

                    return super.damageEntity(source, amount);
                }
            }
        } else {
            return false;
        }
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        EntityItem itemEntity = this.spawnAtLocation(Items.NETHER_STAR);
        if (itemEntity != null) {
            itemEntity.setExtendedLifetime();
        }

    }

    @Override
    public void checkDespawn() {
        if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.die();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffect effect, @Nullable Entity source) {
        return false;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 300.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.6F).add(GenericAttributes.FLYING_SPEED, (double)0.6F).add(GenericAttributes.FOLLOW_RANGE, 40.0D).add(GenericAttributes.ARMOR, 4.0D);
    }

    public float getHeadYRot(int headIndex) {
        return this.yRotHeads[headIndex];
    }

    public float getHeadXRot(int headIndex) {
        return this.xRotHeads[headIndex];
    }

    public int getInvul() {
        return this.entityData.get(DATA_ID_INV);
    }

    public void setInvul(int ticks) {
        this.entityData.set(DATA_ID_INV, ticks);
    }

    public int getHeadTarget(int headIndex) {
        return this.entityData.get(DATA_TARGETS.get(headIndex));
    }

    public void setHeadTarget(int headIndex, int id) {
        this.entityData.set(DATA_TARGETS.get(headIndex), id);
    }

    @Override
    public boolean isPowered() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean canPortal() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffect effect) {
        return effect.getMobEffect() == MobEffectList.WITHER ? false : super.canBeAffected(effect);
    }

    class PathfinderGoalWitherSpawn extends PathfinderGoal {
        public PathfinderGoalWitherSpawn() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.JUMP, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            return EntityWither.this.getInvul() > 0;
        }
    }
}
