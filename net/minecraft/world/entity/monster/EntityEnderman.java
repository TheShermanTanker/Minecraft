package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSourceIndirect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class EntityEnderman extends EntityMonster implements IEntityAngerable {
    private static final UUID SPEED_MODIFIER_ATTACKING_UUID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
    private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(SPEED_MODIFIER_ATTACKING_UUID, "Attacking speed boost", (double)0.15F, AttributeModifier.Operation.ADDITION);
    private static final int DELAY_BETWEEN_CREEPY_STARE_SOUND = 400;
    private static final int MIN_DEAGGRESSION_TIME = 600;
    private static final DataWatcherObject<Optional<IBlockData>> DATA_CARRY_STATE = DataWatcher.defineId(EntityEnderman.class, DataWatcherRegistry.BLOCK_STATE);
    private static final DataWatcherObject<Boolean> DATA_CREEPY = DataWatcher.defineId(EntityEnderman.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_STARED_AT = DataWatcher.defineId(EntityEnderman.class, DataWatcherRegistry.BOOLEAN);
    private int lastStareSound = Integer.MIN_VALUE;
    private int targetChangeTime;
    private static final IntProviderUniform PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public EntityEnderman(EntityTypes<? extends EntityEnderman> type, World world) {
        super(type, world);
        this.maxUpStep = 1.0F;
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityEnderman.EndermanFreezeWhenLookedAt(this));
        this.goalSelector.addGoal(2, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStrollLand(this, 1.0D, 0.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.goalSelector.addGoal(10, new EntityEnderman.PathfinderGoalEndermanPlaceBlock(this));
        this.goalSelector.addGoal(11, new EntityEnderman.PathfinderGoalEndermanPickupBlock(this));
        this.targetSelector.addGoal(1, new EntityEnderman.PathfinderGoalPlayerWhoLookedAtTarget(this, this::isAngryAt));
        this.targetSelector.addGoal(2, new PathfinderGoalHurtByTarget(this));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityEndermite.class, true, false));
        this.targetSelector.addGoal(4, new PathfinderGoalUniversalAngerReset<>(this, false));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 40.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.ATTACK_DAMAGE, 7.0D).add(GenericAttributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public void setGoalTarget(@Nullable EntityLiving target) {
        super.setGoalTarget(target);
        AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
        if (target == null) {
            this.targetChangeTime = 0;
            this.entityData.set(DATA_CREEPY, false);
            this.entityData.set(DATA_STARED_AT, false);
            attributeInstance.removeModifier(SPEED_MODIFIER_ATTACKING);
        } else {
            this.targetChangeTime = this.tickCount;
            this.entityData.set(DATA_CREEPY, true);
            if (!attributeInstance.hasModifier(SPEED_MODIFIER_ATTACKING)) {
                attributeInstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
            }
        }

    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_CARRY_STATE, Optional.empty());
        this.entityData.register(DATA_CREEPY, false);
        this.entityData.register(DATA_STARED_AT, false);
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setAnger(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    @Override
    public int getAnger() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Nullable
    @Override
    public UUID getAngerTarget() {
        return this.persistentAngerTarget;
    }

    public void playStareSound() {
        if (this.tickCount >= this.lastStareSound + 400) {
            this.lastStareSound = this.tickCount;
            if (!this.isSilent()) {
                this.level.playLocalSound(this.locX(), this.getHeadY(), this.locZ(), SoundEffects.ENDERMAN_STARE, this.getSoundCategory(), 2.5F, 1.0F, false);
            }
        }

    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_CREEPY.equals(data) && this.hasBeenStaredAt() && this.level.isClientSide) {
            this.playStareSound();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        IBlockData blockState = this.getCarried();
        if (blockState != null) {
            nbt.set("carriedBlockState", GameProfileSerializer.writeBlockState(blockState));
        }

        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        IBlockData blockState = null;
        if (nbt.hasKeyOfType("carriedBlockState", 10)) {
            blockState = GameProfileSerializer.readBlockState(nbt.getCompound("carriedBlockState"));
            if (blockState.isAir()) {
                blockState = null;
            }
        }

        this.setCarried(blockState);
        this.readPersistentAngerSaveData(this.level, nbt);
    }

    boolean isLookingAtMe(EntityHuman player) {
        ItemStack itemStack = player.getInventory().armor.get(3);
        if (itemStack.is(Blocks.CARVED_PUMPKIN.getItem())) {
            return false;
        } else {
            Vec3D vec3 = player.getViewVector(1.0F).normalize();
            Vec3D vec32 = new Vec3D(this.locX() - player.locX(), this.getHeadY() - player.getHeadY(), this.locZ() - player.locZ());
            double d = vec32.length();
            vec32 = vec32.normalize();
            double e = vec3.dot(vec32);
            return e > 1.0D - 0.025D / d ? player.hasLineOfSight(this) : false;
        }
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 2.55F;
    }

    @Override
    public void movementTick() {
        if (this.level.isClientSide) {
            for(int i = 0; i < 2; ++i) {
                this.level.addParticle(Particles.PORTAL, this.getRandomX(0.5D), this.getRandomY() - 0.25D, this.getRandomZ(0.5D), (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(), (this.random.nextDouble() - 0.5D) * 2.0D);
            }
        }

        this.jumping = false;
        if (!this.level.isClientSide) {
            this.updatePersistentAnger((WorldServer)this.level, true);
        }

        super.movementTick();
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected void mobTick() {
        if (this.level.isDay() && this.tickCount >= this.targetChangeTime + 600) {
            float f = this.getBrightness();
            if (f > 0.5F && this.level.canSeeSky(this.getChunkCoordinates()) && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F) {
                this.setGoalTarget((EntityLiving)null);
                this.teleport();
            }
        }

        super.mobTick();
    }

    protected boolean teleport() {
        if (!this.level.isClientSide() && this.isAlive()) {
            double d = this.locX() + (this.random.nextDouble() - 0.5D) * 64.0D;
            double e = this.locY() + (double)(this.random.nextInt(64) - 32);
            double f = this.locZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
            return this.teleport(d, e, f);
        } else {
            return false;
        }
    }

    boolean teleportTowards(Entity entity) {
        Vec3D vec3 = new Vec3D(this.locX() - entity.locX(), this.getY(0.5D) - entity.getHeadY(), this.locZ() - entity.locZ());
        vec3 = vec3.normalize();
        double d = 16.0D;
        double e = this.locX() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3.x * 16.0D;
        double f = this.locY() + (double)(this.random.nextInt(16) - 8) - vec3.y * 16.0D;
        double g = this.locZ() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3.z * 16.0D;
        return this.teleport(e, f, g);
    }

    private boolean teleport(double x, double y, double z) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, y, z);

        while(mutableBlockPos.getY() > this.level.getMinBuildHeight() && !this.level.getType(mutableBlockPos).getMaterial().isSolid()) {
            mutableBlockPos.move(EnumDirection.DOWN);
        }

        IBlockData blockState = this.level.getType(mutableBlockPos);
        boolean bl = blockState.getMaterial().isSolid();
        boolean bl2 = blockState.getFluid().is(TagsFluid.WATER);
        if (bl && !bl2) {
            boolean bl3 = this.randomTeleport(x, y, z, true);
            if (bl3 && !this.isSilent()) {
                this.level.playSound((EntityHuman)null, this.xo, this.yo, this.zo, SoundEffects.ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
                this.playSound(SoundEffects.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }

            return bl3;
        } else {
            return false;
        }
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isCreepy() ? SoundEffects.ENDERMAN_SCREAM : SoundEffects.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ENDERMAN_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ENDERMAN_DEATH;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        IBlockData blockState = this.getCarried();
        if (blockState != null) {
            this.spawnAtLocation(blockState.getBlock());
        }

    }

    public void setCarried(@Nullable IBlockData state) {
        this.entityData.set(DATA_CARRY_STATE, Optional.ofNullable(state));
    }

    @Nullable
    public IBlockData getCarried() {
        return this.entityData.get(DATA_CARRY_STATE).orElse((IBlockData)null);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (source instanceof EntityDamageSourceIndirect) {
            Entity entity = source.getDirectEntity();
            boolean bl;
            if (entity instanceof EntityPotion) {
                bl = this.hurtWithCleanWater(source, (EntityPotion)entity, amount);
            } else {
                bl = false;
            }

            for(int i = 0; i < 64; ++i) {
                if (this.teleport()) {
                    return true;
                }
            }

            return bl;
        } else {
            boolean bl3 = super.damageEntity(source, amount);
            if (!this.level.isClientSide() && !(source.getEntity() instanceof EntityLiving) && this.random.nextInt(10) != 0) {
                this.teleport();
            }

            return bl3;
        }
    }

    private boolean hurtWithCleanWater(DamageSource source, EntityPotion potion, float amount) {
        ItemStack itemStack = potion.getSuppliedItem();
        PotionRegistry potion2 = PotionUtil.getPotion(itemStack);
        List<MobEffect> list = PotionUtil.getEffects(itemStack);
        boolean bl = potion2 == Potions.WATER && list.isEmpty();
        return bl ? super.damageEntity(source, amount) : false;
    }

    public boolean isCreepy() {
        return this.entityData.get(DATA_CREEPY);
    }

    public boolean hasBeenStaredAt() {
        return this.entityData.get(DATA_STARED_AT);
    }

    public void setBeingStaredAt() {
        this.entityData.set(DATA_STARED_AT, true);
    }

    @Override
    public boolean isSpecialPersistence() {
        return super.isSpecialPersistence() || this.getCarried() != null;
    }

    static class EndermanFreezeWhenLookedAt extends PathfinderGoal {
        private final EntityEnderman enderman;
        @Nullable
        private EntityLiving target;

        public EndermanFreezeWhenLookedAt(EntityEnderman enderman) {
            this.enderman = enderman;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            this.target = this.enderman.getGoalTarget();
            if (!(this.target instanceof EntityHuman)) {
                return false;
            } else {
                double d = this.target.distanceToSqr(this.enderman);
                return d > 256.0D ? false : this.enderman.isLookingAtMe((EntityHuman)this.target);
            }
        }

        @Override
        public void start() {
            this.enderman.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.enderman.getControllerLook().setLookAt(this.target.locX(), this.target.getHeadY(), this.target.locZ());
        }
    }

    static class PathfinderGoalEndermanPickupBlock extends PathfinderGoal {
        private final EntityEnderman enderman;

        public PathfinderGoalEndermanPickupBlock(EntityEnderman enderman) {
            this.enderman = enderman;
        }

        @Override
        public boolean canUse() {
            if (this.enderman.getCarried() != null) {
                return false;
            } else if (!this.enderman.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            } else {
                return this.enderman.getRandom().nextInt(reducedTickDelay(20)) == 0;
            }
        }

        @Override
        public void tick() {
            Random random = this.enderman.getRandom();
            World level = this.enderman.level;
            int i = MathHelper.floor(this.enderman.locX() - 2.0D + random.nextDouble() * 4.0D);
            int j = MathHelper.floor(this.enderman.locY() + random.nextDouble() * 3.0D);
            int k = MathHelper.floor(this.enderman.locZ() - 2.0D + random.nextDouble() * 4.0D);
            BlockPosition blockPos = new BlockPosition(i, j, k);
            IBlockData blockState = level.getType(blockPos);
            Vec3D vec3 = new Vec3D((double)this.enderman.getBlockX() + 0.5D, (double)j + 0.5D, (double)this.enderman.getBlockZ() + 0.5D);
            Vec3D vec32 = new Vec3D((double)i + 0.5D, (double)j + 0.5D, (double)k + 0.5D);
            MovingObjectPositionBlock blockHitResult = level.rayTrace(new RayTrace(vec3, vec32, RayTrace.BlockCollisionOption.OUTLINE, RayTrace.FluidCollisionOption.NONE, this.enderman));
            boolean bl = blockHitResult.getBlockPosition().equals(blockPos);
            if (blockState.is(TagsBlock.ENDERMAN_HOLDABLE) && bl) {
                level.removeBlock(blockPos, false);
                level.gameEvent(this.enderman, GameEvent.BLOCK_DESTROY, blockPos);
                this.enderman.setCarried(blockState.getBlock().getBlockData());
            }

        }
    }

    static class PathfinderGoalEndermanPlaceBlock extends PathfinderGoal {
        private final EntityEnderman enderman;

        public PathfinderGoalEndermanPlaceBlock(EntityEnderman enderman) {
            this.enderman = enderman;
        }

        @Override
        public boolean canUse() {
            if (this.enderman.getCarried() == null) {
                return false;
            } else if (!this.enderman.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            } else {
                return this.enderman.getRandom().nextInt(reducedTickDelay(2000)) == 0;
            }
        }

        @Override
        public void tick() {
            Random random = this.enderman.getRandom();
            World level = this.enderman.level;
            int i = MathHelper.floor(this.enderman.locX() - 1.0D + random.nextDouble() * 2.0D);
            int j = MathHelper.floor(this.enderman.locY() + random.nextDouble() * 2.0D);
            int k = MathHelper.floor(this.enderman.locZ() - 1.0D + random.nextDouble() * 2.0D);
            BlockPosition blockPos = new BlockPosition(i, j, k);
            IBlockData blockState = level.getType(blockPos);
            BlockPosition blockPos2 = blockPos.below();
            IBlockData blockState2 = level.getType(blockPos2);
            IBlockData blockState3 = this.enderman.getCarried();
            if (blockState3 != null) {
                blockState3 = Block.updateFromNeighbourShapes(blockState3, this.enderman.level, blockPos);
                if (this.canPlaceBlock(level, blockPos, blockState3, blockState, blockState2, blockPos2)) {
                    level.setTypeAndData(blockPos, blockState3, 3);
                    level.gameEvent(this.enderman, GameEvent.BLOCK_PLACE, blockPos);
                    this.enderman.setCarried((IBlockData)null);
                }

            }
        }

        private boolean canPlaceBlock(World world, BlockPosition posAbove, IBlockData carriedState, IBlockData stateAbove, IBlockData state, BlockPosition pos) {
            return stateAbove.isAir() && !state.isAir() && !state.is(Blocks.BEDROCK) && state.isCollisionShapeFullBlock(world, pos) && carriedState.canPlace(world, posAbove) && world.getEntities(this.enderman, AxisAlignedBB.unitCubeFromLowerCorner(Vec3D.atLowerCornerOf(posAbove))).isEmpty();
        }
    }

    static class PathfinderGoalPlayerWhoLookedAtTarget extends PathfinderGoalNearestAttackableTarget<EntityHuman> {
        private final EntityEnderman enderman;
        @Nullable
        private EntityHuman pendingTarget;
        private int aggroTime;
        private int teleportTime;
        private final PathfinderTargetCondition startAggroTargetConditions;
        private final PathfinderTargetCondition continueAggroTargetConditions = PathfinderTargetCondition.forCombat().ignoreLineOfSight();

        public PathfinderGoalPlayerWhoLookedAtTarget(EntityEnderman enderman, @Nullable Predicate<EntityLiving> targetPredicate) {
            super(enderman, EntityHuman.class, 10, false, false, targetPredicate);
            this.enderman = enderman;
            this.startAggroTargetConditions = PathfinderTargetCondition.forCombat().range(this.getFollowDistance()).selector((playerEntity) -> {
                return enderman.isLookingAtMe((EntityHuman)playerEntity);
            });
        }

        @Override
        public boolean canUse() {
            this.pendingTarget = this.enderman.level.getNearestPlayer(this.startAggroTargetConditions, this.enderman);
            return this.pendingTarget != null;
        }

        @Override
        public void start() {
            this.aggroTime = this.adjustedTickDelay(5);
            this.teleportTime = 0;
            this.enderman.setBeingStaredAt();
        }

        @Override
        public void stop() {
            this.pendingTarget = null;
            super.stop();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.pendingTarget != null) {
                if (!this.enderman.isLookingAtMe(this.pendingTarget)) {
                    return false;
                } else {
                    this.enderman.lookAt(this.pendingTarget, 10.0F, 10.0F);
                    return true;
                }
            } else {
                return this.target != null && this.continueAggroTargetConditions.test(this.enderman, this.target) ? true : super.canContinueToUse();
            }
        }

        @Override
        public void tick() {
            if (this.enderman.getGoalTarget() == null) {
                super.setTarget((EntityLiving)null);
            }

            if (this.pendingTarget != null) {
                if (--this.aggroTime <= 0) {
                    this.target = this.pendingTarget;
                    this.pendingTarget = null;
                    super.start();
                }
            } else {
                if (this.target != null && !this.enderman.isPassenger()) {
                    if (this.enderman.isLookingAtMe((EntityHuman)this.target)) {
                        if (this.target.distanceToSqr(this.enderman) < 16.0D) {
                            this.enderman.teleport();
                        }

                        this.teleportTime = 0;
                    } else if (this.target.distanceToSqr(this.enderman) > 256.0D && this.teleportTime++ >= this.adjustedTickDelay(30) && this.enderman.teleportTowards(this.target)) {
                        this.teleportTime = 0;
                    }
                }

                super.tick();
            }

        }
    }
}
