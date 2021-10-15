package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.math.Vector3fa;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLookSmoothSwim;
import net.minecraft.world.entity.ai.control.ControllerMoveSmoothSwim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.IBucketable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderAmphibious;
import net.minecraft.world.phys.Vec3D;

public class EntityAxolotl extends EntityAnimal implements LerpingModel, IBucketable {
    public static final int TOTAL_PLAYDEAD_TIME = 200;
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super EntityAxolotl>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.AXOLOTL_ATTACKABLES, SensorType.AXOLOTL_TEMPTATIONS);
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.PLAY_DEAD_TICKS, MemoryModuleType.NEAREST_ATTACKABLE, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.HAS_HUNTING_COOLDOWN);
    private static final DataWatcherObject<Integer> DATA_VARIANT = DataWatcher.defineId(EntityAxolotl.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_PLAYING_DEAD = DataWatcher.defineId(EntityAxolotl.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> FROM_BUCKET = DataWatcher.defineId(EntityAxolotl.class, DataWatcherRegistry.BOOLEAN);
    public static final double PLAYER_REGEN_DETECTION_RANGE = 20.0D;
    public static final int RARE_VARIANT_CHANCE = 1200;
    private static final int AXOLOTL_TOTAL_AIR_SUPPLY = 6000;
    public static final String VARIANT_TAG = "Variant";
    private static final int REHYDRATE_AIR_SUPPLY = 1800;
    private static final int REGEN_BUFF_MAX_DURATION = 2400;
    private final Map<String, Vector3fa> modelRotationValues = Maps.newHashMap();
    private static final int REGEN_BUFF_BASE_DURATION = 100;

    public EntityAxolotl(EntityTypes<? extends EntityAxolotl> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new EntityAxolotl.ControllerMoveAxolotl(this);
        this.lookControl = new EntityAxolotl.ControllerLookAxolotl(this, 20);
        this.maxUpStep = 1.0F;
    }

    @Override
    public Map<String, Vector3fa> getModelRotationValues() {
        return this.modelRotationValues;
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return 0.0F;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_VARIANT, 0);
        this.entityData.register(DATA_PLAYING_DEAD, false);
        this.entityData.register(FROM_BUCKET, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Variant", this.getVariant().getId());
        nbt.setBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setVariant(EntityAxolotl.Variant.BY_ID[nbt.getInt("Variant")]);
        this.setFromBucket(nbt.getBoolean("FromBucket"));
    }

    @Override
    public void playAmbientSound() {
        if (!this.isPlayingDead()) {
            super.playAmbientSound();
        }
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        boolean bl = false;
        if (spawnReason == EnumMobSpawn.BUCKET) {
            return entityData;
        } else {
            if (entityData instanceof EntityAxolotl.GroupDataAxolotl) {
                if (((EntityAxolotl.GroupDataAxolotl)entityData).getGroupSize() >= 2) {
                    bl = true;
                }
            } else {
                entityData = new EntityAxolotl.GroupDataAxolotl(EntityAxolotl.Variant.getCommonSpawnVariant(this.level.random), EntityAxolotl.Variant.getCommonSpawnVariant(this.level.random));
            }

            this.setVariant(((EntityAxolotl.GroupDataAxolotl)entityData).getVariant(this.level.random));
            if (bl) {
                this.setAgeRaw(-24000);
            }

            return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        }
    }

    @Override
    public void entityBaseTick() {
        int i = this.getAirTicks();
        super.entityBaseTick();
        if (!this.isNoAI()) {
            this.handleAirSupply(i);
        }

    }

    protected void handleAirSupply(int air) {
        if (this.isAlive() && !this.isInWaterRainOrBubble()) {
            this.setAirTicks(air - 1);
            if (this.getAirTicks() == -20) {
                this.setAirTicks(0);
                this.damageEntity(DamageSource.DRY_OUT, 2.0F);
            }
        } else {
            this.setAirTicks(this.getMaxAirSupply());
        }

    }

    public void rehydrate() {
        int i = this.getAirTicks() + 1800;
        this.setAirTicks(Math.min(i, this.getMaxAirSupply()));
    }

    @Override
    public int getMaxAirSupply() {
        return 6000;
    }

    public EntityAxolotl.Variant getVariant() {
        return EntityAxolotl.Variant.BY_ID[this.entityData.get(DATA_VARIANT)];
    }

    public void setVariant(EntityAxolotl.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getId());
    }

    private static boolean useRareVariant(Random random) {
        return random.nextInt(1200) == 0;
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.WATER;
    }

    public void setPlayingDead(boolean playingDead) {
        this.entityData.set(DATA_PLAYING_DEAD, playingDead);
    }

    public boolean isPlayingDead() {
        return this.entityData.get(DATA_PLAYING_DEAD);
    }

    @Override
    public boolean isFromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        EntityAxolotl axolotl = EntityTypes.AXOLOTL.create(world);
        if (axolotl != null) {
            EntityAxolotl.Variant variant;
            if (useRareVariant(this.random)) {
                variant = EntityAxolotl.Variant.getRareSpawnVariant(this.random);
            } else {
                variant = this.random.nextBoolean() ? this.getVariant() : ((EntityAxolotl)entity).getVariant();
            }

            axolotl.setVariant(variant);
            axolotl.setPersistent();
        }

        return axolotl;
    }

    @Override
    public double getMeleeAttackRangeSqr(EntityLiving target) {
        return 1.5D + (double)target.getWidth() * 2.0D;
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return TagsItem.AXOLOTL_TEMPT_ITEMS.isTagged(stack.getItem());
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return true;
    }

    @Override
    protected void mobTick() {
        this.level.getMethodProfiler().enter("axolotlBrain");
        this.getBehaviorController().tick((WorldServer)this.level, this);
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("axolotlActivityUpdate");
        AxolotlAI.updateActivity(this);
        this.level.getMethodProfiler().exit();
        if (!this.isNoAI()) {
            Optional<Integer> optional = this.getBehaviorController().getMemory(MemoryModuleType.PLAY_DEAD_TICKS);
            this.setPlayingDead(optional.isPresent() && optional.get() > 0);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 14.0D).add(GenericAttributes.MOVEMENT_SPEED, 1.0D).add(GenericAttributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new EntityAxolotl.NavigationAxolotl(this, world);
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = target.damageEntity(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
            this.playSound(SoundEffects.AXOLOTL_ATTACK, 1.0F, 1.0F);
        }

        return bl;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        float f = this.getHealth();
        if (!this.level.isClientSide && !this.isNoAI() && this.level.random.nextInt(3) == 0 && ((float)this.level.random.nextInt(3) < amount || f / this.getMaxHealth() < 0.5F) && amount < f && this.isInWater() && (source.getEntity() != null || source.getDirectEntity() != null) && !this.isPlayingDead()) {
            this.brain.setMemory(MemoryModuleType.PLAY_DEAD_TICKS, 200);
        }

        return super.damageEntity(source, amount);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.655F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        return IBucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public void setBucketName(ItemStack stack) {
        IBucketable.saveDefaultDataToBucketTag(this, stack);
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        compoundTag.setInt("Variant", this.getVariant().getId());
        compoundTag.setInt("Age", this.getAge());
        BehaviorController<?> brain = this.getBehaviorController();
        if (brain.hasMemory(MemoryModuleType.HAS_HUNTING_COOLDOWN)) {
            compoundTag.setLong("HuntingCooldown", brain.getTimeUntilExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN));
        }

    }

    @Override
    public void loadFromBucketTag(NBTTagCompound nbt) {
        IBucketable.loadDefaultDataFromBucketTag(this, nbt);
        this.setVariant(EntityAxolotl.Variant.BY_ID[nbt.getInt("Variant")]);
        if (nbt.hasKey("Age")) {
            this.setAgeRaw(nbt.getInt("Age"));
        }

        if (nbt.hasKey("HuntingCooldown")) {
            this.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, nbt.getLong("HuntingCooldown"));
        }

    }

    @Override
    public ItemStack getBucketItem() {
        return new ItemStack(Items.AXOLOTL_BUCKET);
    }

    @Override
    public SoundEffect getPickupSound() {
        return SoundEffects.BUCKET_FILL_AXOLOTL;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.isPlayingDead() && super.canBeSeenAsEnemy();
    }

    public static void onStopAttacking(EntityAxolotl axolotl) {
        Optional<EntityLiving> optional = axolotl.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (optional.isPresent()) {
            World level = axolotl.level;
            EntityLiving livingEntity = optional.get();
            if (livingEntity.isDeadOrDying()) {
                DamageSource damageSource = livingEntity.getLastDamageSource();
                if (damageSource != null) {
                    Entity entity = damageSource.getEntity();
                    if (entity != null && entity.getEntityType() == EntityTypes.PLAYER) {
                        EntityHuman player = (EntityHuman)entity;
                        List<EntityHuman> list = level.getEntitiesOfClass(EntityHuman.class, axolotl.getBoundingBox().inflate(20.0D));
                        if (list.contains(player)) {
                            axolotl.applySupportingEffects(player);
                        }
                    }
                }
            }

        }
    }

    public void applySupportingEffects(EntityHuman player) {
        MobEffect mobEffectInstance = player.getEffect(MobEffects.REGENERATION);
        int i = mobEffectInstance != null ? mobEffectInstance.getDuration() : 0;
        if (i < 2400) {
            i = Math.min(2400, 100 + i);
            player.addEffect(new MobEffect(MobEffects.REGENERATION, i, 0), this);
        }

        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    @Override
    public boolean isSpecialPersistence() {
        return super.isSpecialPersistence() || this.isFromBucket();
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.AXOLOTL_HURT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.AXOLOTL_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isInWater() ? SoundEffects.AXOLOTL_IDLE_WATER : SoundEffects.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEffect getSoundSplash() {
        return SoundEffects.AXOLOTL_SPLASH;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.AXOLOTL_SWIM;
    }

    @Override
    protected BehaviorController.Provider<EntityAxolotl> brainProvider() {
        return BehaviorController.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return AxolotlAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public BehaviorController<EntityAxolotl> getBehaviorController() {
        return super.getBehaviorController();
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendEntityBrain(this);
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.9D));
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    protected void usePlayerItem(EntityHuman player, EnumHand hand, ItemStack stack) {
        if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
            player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        } else {
            super.usePlayerItem(player, hand, stack);
        }

    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    class ControllerLookAxolotl extends ControllerLookSmoothSwim {
        public ControllerLookAxolotl(EntityAxolotl axolotl, int maxYawDifference) {
            super(axolotl, maxYawDifference);
        }

        @Override
        public void tick() {
            if (!EntityAxolotl.this.isPlayingDead()) {
                super.tick();
            }

        }
    }

    static class ControllerMoveAxolotl extends ControllerMoveSmoothSwim {
        private final EntityAxolotl axolotl;

        public ControllerMoveAxolotl(EntityAxolotl axolotl) {
            super(axolotl, 85, 10, 0.1F, 0.5F, false);
            this.axolotl = axolotl;
        }

        @Override
        public void tick() {
            if (!this.axolotl.isPlayingDead()) {
                super.tick();
            }

        }
    }

    public static class GroupDataAxolotl extends EntityAgeable.GroupDataAgeable {
        public final EntityAxolotl.Variant[] types;

        public GroupDataAxolotl(EntityAxolotl.Variant... variants) {
            super(false);
            this.types = variants;
        }

        public EntityAxolotl.Variant getVariant(Random random) {
            return this.types[random.nextInt(this.types.length)];
        }
    }

    static class NavigationAxolotl extends NavigationGuardian {
        NavigationAxolotl(EntityAxolotl axolotl, World world) {
            super(axolotl, world);
        }

        @Override
        protected boolean canUpdatePath() {
            return true;
        }

        @Override
        protected Pathfinder createPathFinder(int range) {
            this.nodeEvaluator = new PathfinderAmphibious(false);
            return new Pathfinder(this.nodeEvaluator, range);
        }

        @Override
        public boolean isStableDestination(BlockPosition pos) {
            return !this.level.getType(pos.below()).isAir();
        }
    }

    public static enum Variant {
        LUCY(0, "lucy", true),
        WILD(1, "wild", true),
        GOLD(2, "gold", true),
        CYAN(3, "cyan", true),
        BLUE(4, "blue", false);

        public static final EntityAxolotl.Variant[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(EntityAxolotl.Variant::getId)).toArray((i) -> {
            return new EntityAxolotl.Variant[i];
        });
        private final int id;
        private final String name;
        private final boolean common;

        private Variant(int id, String name, boolean natural) {
            this.id = id;
            this.name = name;
            this.common = natural;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public static EntityAxolotl.Variant getCommonSpawnVariant(Random random) {
            return getSpawnVariant(random, true);
        }

        public static EntityAxolotl.Variant getRareSpawnVariant(Random random) {
            return getSpawnVariant(random, false);
        }

        private static EntityAxolotl.Variant getSpawnVariant(Random random, boolean includeUnnatural) {
            EntityAxolotl.Variant[] variants = Arrays.stream(BY_ID).filter((variant) -> {
                return variant.common == includeUnnatural;
            }).toArray((i) -> {
                return new EntityAxolotl.Variant[i];
            });
            return SystemUtils.getRandom(variants, random);
        }
    }
}
