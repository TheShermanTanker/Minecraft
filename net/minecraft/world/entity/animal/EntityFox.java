package net.minecraft.world.entity.animal;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFleeSun;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalGotoTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLeapAtTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalNearestVillage;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalWaterJumpAbstract;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSweetBerryBush;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityFox extends EntityAnimal {
    private static final DataWatcherObject<Integer> DATA_TYPE_ID = DataWatcher.defineId(EntityFox.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityFox.class, DataWatcherRegistry.BYTE);
    private static final int FLAG_SITTING = 1;
    public static final int FLAG_CROUCHING = 4;
    public static final int FLAG_INTERESTED = 8;
    public static final int FLAG_POUNCING = 16;
    private static final int FLAG_SLEEPING = 32;
    private static final int FLAG_FACEPLANTED = 64;
    private static final int FLAG_DEFENDING = 128;
    public static final DataWatcherObject<Optional<UUID>> DATA_TRUSTED_ID_0 = DataWatcher.defineId(EntityFox.class, DataWatcherRegistry.OPTIONAL_UUID);
    public static final DataWatcherObject<Optional<UUID>> DATA_TRUSTED_ID_1 = DataWatcher.defineId(EntityFox.class, DataWatcherRegistry.OPTIONAL_UUID);
    static final Predicate<EntityItem> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive();
    };
    private static final Predicate<Entity> TRUSTED_TARGET_SELECTOR = (entity) -> {
        if (!(entity instanceof EntityLiving)) {
            return false;
        } else {
            EntityLiving livingEntity = (EntityLiving)entity;
            return livingEntity.getLastHurtMob() != null && livingEntity.getLastHurtMobTimestamp() < livingEntity.tickCount + 600;
        }
    };
    static final Predicate<Entity> STALKABLE_PREY = (entity) -> {
        return entity instanceof EntityChicken || entity instanceof EntityRabbit;
    };
    private static final Predicate<Entity> AVOID_PLAYERS = (entity) -> {
        return !entity.isDiscrete() && IEntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity);
    };
    private static final int MIN_TICKS_BEFORE_EAT = 600;
    private PathfinderGoal landTargetGoal;
    private PathfinderGoal turtleEggTargetGoal;
    private PathfinderGoal fishTargetGoal;
    private float interestedAngle;
    private float interestedAngleO;
    float crouchAmount;
    float crouchAmountO;
    private int ticksSinceEaten;

    public EntityFox(EntityTypes<? extends EntityFox> type, World world) {
        super(type, world);
        this.lookControl = new EntityFox.FoxLookControl();
        this.moveControl = new EntityFox.FoxMoveControl();
        this.setPathfindingMalus(PathType.DANGER_OTHER, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0F);
        this.setCanPickupLoot(true);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TRUSTED_ID_0, Optional.empty());
        this.entityData.register(DATA_TRUSTED_ID_1, Optional.empty());
        this.entityData.register(DATA_TYPE_ID, 0);
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    protected void initPathfinder() {
        this.landTargetGoal = new PathfinderGoalNearestAttackableTarget<>(this, EntityAnimal.class, 10, false, false, (entity) -> {
            return entity instanceof EntityChicken || entity instanceof EntityRabbit;
        });
        this.turtleEggTargetGoal = new PathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, 10, false, false, EntityTurtle.BABY_ON_LAND_SELECTOR);
        this.fishTargetGoal = new PathfinderGoalNearestAttackableTarget<>(this, EntityFish.class, 20, false, false, (entity) -> {
            return entity instanceof EntityFishSchool;
        });
        this.goalSelector.addGoal(0, new EntityFox.FoxFloatGoal());
        this.goalSelector.addGoal(1, new EntityFox.FaceplantGoal());
        this.goalSelector.addGoal(2, new EntityFox.FoxPanicGoal(2.2D));
        this.goalSelector.addGoal(3, new EntityFox.FoxBreedGoal(1.0D));
        this.goalSelector.addGoal(4, new PathfinderGoalAvoidTarget<>(this, EntityHuman.class, 16.0F, 1.6D, 1.4D, (entity) -> {
            return AVOID_PLAYERS.test(entity) && !this.trusts(entity.getUniqueID()) && !this.isDefending();
        }));
        this.goalSelector.addGoal(4, new PathfinderGoalAvoidTarget<>(this, EntityWolf.class, 8.0F, 1.6D, 1.4D, (entity) -> {
            return !((EntityWolf)entity).isTamed() && !this.isDefending();
        }));
        this.goalSelector.addGoal(4, new PathfinderGoalAvoidTarget<>(this, EntityPolarBear.class, 8.0F, 1.6D, 1.4D, (entity) -> {
            return !this.isDefending();
        }));
        this.goalSelector.addGoal(5, new EntityFox.StalkPreyGoal());
        this.goalSelector.addGoal(6, new EntityFox.FoxPounceGoal());
        this.goalSelector.addGoal(6, new EntityFox.SeekShelterGoal(1.25D));
        this.goalSelector.addGoal(7, new EntityFox.FoxMeleeAttackGoal((double)1.2F, true));
        this.goalSelector.addGoal(7, new EntityFox.SleepGoal());
        this.goalSelector.addGoal(8, new EntityFox.FoxFollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(9, new EntityFox.FoxStrollThroughVillageGoal(32, 200));
        this.goalSelector.addGoal(10, new EntityFox.FoxEatBerriesGoal((double)1.2F, 12, 1));
        this.goalSelector.addGoal(10, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.addGoal(11, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(11, new EntityFox.FoxSearchForItemsGoal());
        this.goalSelector.addGoal(12, new EntityFox.FoxLookAtPlayerGoal(this, EntityHuman.class, 24.0F));
        this.goalSelector.addGoal(13, new EntityFox.PerchAndSearchGoal());
        this.targetSelector.addGoal(3, new EntityFox.DefendTrustedTargetGoal(EntityLiving.class, false, false, (entity) -> {
            return TRUSTED_TARGET_SELECTOR.test(entity) && !this.trusts(entity.getUniqueID());
        }));
    }

    @Override
    public SoundEffect getEatingSound(ItemStack stack) {
        return SoundEffects.FOX_EAT;
    }

    @Override
    public void movementTick() {
        if (!this.level.isClientSide && this.isAlive() && this.doAITick()) {
            ++this.ticksSinceEaten;
            ItemStack itemStack = this.getEquipment(EnumItemSlot.MAINHAND);
            if (this.canEat(itemStack)) {
                if (this.ticksSinceEaten > 600) {
                    ItemStack itemStack2 = itemStack.finishUsingItem(this.level, this);
                    if (!itemStack2.isEmpty()) {
                        this.setSlot(EnumItemSlot.MAINHAND, itemStack2);
                    }

                    this.ticksSinceEaten = 0;
                } else if (this.ticksSinceEaten > 560 && this.random.nextFloat() < 0.1F) {
                    this.playSound(this.getEatingSound(itemStack), 1.0F, 1.0F);
                    this.level.broadcastEntityEffect(this, (byte)45);
                }
            }

            EntityLiving livingEntity = this.getGoalTarget();
            if (livingEntity == null || !livingEntity.isAlive()) {
                this.setCrouching(false);
                this.setIsInterested(false);
            }
        }

        if (this.isSleeping() || this.isFrozen()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }

        super.movementTick();
        if (this.isDefending() && this.random.nextFloat() < 0.05F) {
            this.playSound(SoundEffects.FOX_AGGRO, 1.0F, 1.0F);
        }

    }

    @Override
    protected boolean isFrozen() {
        return this.isDeadOrDying();
    }

    private boolean canEat(ItemStack stack) {
        return stack.getItem().isFood() && this.getGoalTarget() == null && this.onGround && !this.isSleeping();
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        if (this.random.nextFloat() < 0.2F) {
            float f = this.random.nextFloat();
            ItemStack itemStack;
            if (f < 0.05F) {
                itemStack = new ItemStack(Items.EMERALD);
            } else if (f < 0.2F) {
                itemStack = new ItemStack(Items.EGG);
            } else if (f < 0.4F) {
                itemStack = this.random.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
            } else if (f < 0.6F) {
                itemStack = new ItemStack(Items.WHEAT);
            } else if (f < 0.8F) {
                itemStack = new ItemStack(Items.LEATHER);
            } else {
                itemStack = new ItemStack(Items.FEATHER);
            }

            this.setSlot(EnumItemSlot.MAINHAND, itemStack);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 45) {
            ItemStack itemStack = this.getEquipment(EnumItemSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                for(int i = 0; i < 8; ++i) {
                    Vec3D vec3 = (new Vec3D(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)).xRot(-this.getXRot() * ((float)Math.PI / 180F)).yRot(-this.getYRot() * ((float)Math.PI / 180F));
                    this.level.addParticle(new ParticleParamItem(Particles.ITEM, itemStack), this.locX() + this.getLookDirection().x / 2.0D, this.locY(), this.locZ() + this.getLookDirection().z / 2.0D, vec3.x, vec3.y + 0.05D, vec3.z);
                }
            }
        } else {
            super.handleEntityEvent(status);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.FOLLOW_RANGE, 32.0D).add(GenericAttributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public EntityFox getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityFox fox = EntityTypes.FOX.create(serverLevel);
        fox.setFoxType(this.random.nextBoolean() ? this.getFoxType() : ((EntityFox)ageableMob).getFoxType());
        return fox;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        Optional<ResourceKey<BiomeBase>> optional = world.getBiomeName(this.getChunkCoordinates());
        EntityFox.Type type = EntityFox.Type.byBiome(optional);
        boolean bl = false;
        if (entityData instanceof EntityFox.FoxGroupData) {
            type = ((EntityFox.FoxGroupData)entityData).type;
            if (((EntityFox.FoxGroupData)entityData).getGroupSize() >= 2) {
                bl = true;
            }
        } else {
            entityData = new EntityFox.FoxGroupData(type);
        }

        this.setFoxType(type);
        if (bl) {
            this.setAgeRaw(-24000);
        }

        if (world instanceof WorldServer) {
            this.initializePathFinderGoals();
        }

        this.populateDefaultEquipmentSlots(difficulty);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    private void initializePathFinderGoals() {
        if (this.getFoxType() == EntityFox.Type.RED) {
            this.targetSelector.addGoal(4, this.landTargetGoal);
            this.targetSelector.addGoal(4, this.turtleEggTargetGoal);
            this.targetSelector.addGoal(6, this.fishTargetGoal);
        } else {
            this.targetSelector.addGoal(4, this.fishTargetGoal);
            this.targetSelector.addGoal(6, this.landTargetGoal);
            this.targetSelector.addGoal(6, this.turtleEggTargetGoal);
        }

    }

    @Override
    protected void usePlayerItem(EntityHuman player, EnumHand hand, ItemStack stack) {
        if (this.isBreedItem(stack)) {
            this.playSound(this.getEatingSound(stack), 1.0F, 1.0F);
        }

        super.usePlayerItem(player, hand, stack);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? dimensions.height * 0.85F : 0.4F;
    }

    public EntityFox.Type getFoxType() {
        return EntityFox.Type.byId(this.entityData.get(DATA_TYPE_ID));
    }

    public void setFoxType(EntityFox.Type type) {
        this.entityData.set(DATA_TYPE_ID, type.getId());
    }

    List<UUID> getTrustedUUIDs() {
        List<UUID> list = Lists.newArrayList();
        list.add(this.entityData.get(DATA_TRUSTED_ID_0).orElse((UUID)null));
        list.add(this.entityData.get(DATA_TRUSTED_ID_1).orElse((UUID)null));
        return list;
    }

    void addTrustedUUID(@Nullable UUID uuid) {
        if (this.entityData.get(DATA_TRUSTED_ID_0).isPresent()) {
            this.entityData.set(DATA_TRUSTED_ID_1, Optional.ofNullable(uuid));
        } else {
            this.entityData.set(DATA_TRUSTED_ID_0, Optional.ofNullable(uuid));
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        List<UUID> list = this.getTrustedUUIDs();
        NBTTagList listTag = new NBTTagList();

        for(UUID uUID : list) {
            if (uUID != null) {
                listTag.add(GameProfileSerializer.createUUID(uUID));
            }
        }

        nbt.set("Trusted", listTag);
        nbt.setBoolean("Sleeping", this.isSleeping());
        nbt.setString("Type", this.getFoxType().getName());
        nbt.setBoolean("Sitting", this.isSitting());
        nbt.setBoolean("Crouching", this.isCrouching());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        NBTTagList listTag = nbt.getList("Trusted", 11);

        for(int i = 0; i < listTag.size(); ++i) {
            this.addTrustedUUID(GameProfileSerializer.loadUUID(listTag.get(i)));
        }

        this.setSleeping(nbt.getBoolean("Sleeping"));
        this.setFoxType(EntityFox.Type.byName(nbt.getString("Type")));
        this.setSitting(nbt.getBoolean("Sitting"));
        this.setCrouching(nbt.getBoolean("Crouching"));
        if (this.level instanceof WorldServer) {
            this.initializePathFinderGoals();
        }

    }

    public boolean isSitting() {
        return this.getFlag(1);
    }

    public void setSitting(boolean sitting) {
        this.setFlag(1, sitting);
    }

    public boolean isFaceplanted() {
        return this.getFlag(64);
    }

    public void setFaceplanted(boolean walking) {
        this.setFlag(64, walking);
    }

    public boolean isDefending() {
        return this.getFlag(128);
    }

    public void setDefending(boolean aggressive) {
        this.setFlag(128, aggressive);
    }

    @Override
    public boolean isSleeping() {
        return this.getFlag(32);
    }

    public void setSleeping(boolean sleeping) {
        this.setFlag(32, sleeping);
    }

    private void setFlag(int mask, boolean value) {
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | mask));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~mask));
        }

    }

    private boolean getFlag(int bitmask) {
        return (this.entityData.get(DATA_FLAGS_ID) & bitmask) != 0;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(stack);
        if (!this.getEquipment(equipmentSlot).isEmpty()) {
            return false;
        } else {
            return equipmentSlot == EnumItemSlot.MAINHAND && super.canTakeItem(stack);
        }
    }

    @Override
    public boolean canPickup(ItemStack stack) {
        Item item = stack.getItem();
        ItemStack itemStack = this.getEquipment(EnumItemSlot.MAINHAND);
        return itemStack.isEmpty() || this.ticksSinceEaten > 0 && item.isFood() && !itemStack.getItem().isFood();
    }

    private void spitOutItem(ItemStack stack) {
        if (!stack.isEmpty() && !this.level.isClientSide) {
            EntityItem itemEntity = new EntityItem(this.level, this.locX() + this.getLookDirection().x, this.locY() + 1.0D, this.locZ() + this.getLookDirection().z, stack);
            itemEntity.setPickupDelay(40);
            itemEntity.setThrower(this.getUniqueID());
            this.playSound(SoundEffects.FOX_SPIT, 1.0F, 1.0F);
            this.level.addEntity(itemEntity);
        }
    }

    private void dropItemStack(ItemStack stack) {
        EntityItem itemEntity = new EntityItem(this.level, this.locX(), this.locY(), this.locZ(), stack);
        this.level.addEntity(itemEntity);
    }

    @Override
    protected void pickUpItem(EntityItem item) {
        ItemStack itemStack = item.getItemStack();
        if (this.canPickup(itemStack)) {
            int i = itemStack.getCount();
            if (i > 1) {
                this.dropItemStack(itemStack.cloneAndSubtract(i - 1));
            }

            this.spitOutItem(this.getEquipment(EnumItemSlot.MAINHAND));
            this.onItemPickup(item);
            this.setSlot(EnumItemSlot.MAINHAND, itemStack.cloneAndSubtract(1));
            this.handDropChances[EnumItemSlot.MAINHAND.getIndex()] = 2.0F;
            this.receive(item, itemStack.getCount());
            item.die();
            this.ticksSinceEaten = 0;
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.doAITick()) {
            boolean bl = this.isInWater();
            if (bl || this.getGoalTarget() != null || this.level.isThundering()) {
                this.wakeUp();
            }

            if (bl || this.isSleeping()) {
                this.setSitting(false);
            }

            if (this.isFaceplanted() && this.level.random.nextFloat() < 0.2F) {
                BlockPosition blockPos = this.getChunkCoordinates();
                IBlockData blockState = this.level.getType(blockPos);
                this.level.triggerEffect(2001, blockPos, Block.getCombinedId(blockState));
            }
        }

        this.interestedAngleO = this.interestedAngle;
        if (this.isInterested()) {
            this.interestedAngle += (1.0F - this.interestedAngle) * 0.4F;
        } else {
            this.interestedAngle += (0.0F - this.interestedAngle) * 0.4F;
        }

        this.crouchAmountO = this.crouchAmount;
        if (this.isCrouching()) {
            this.crouchAmount += 0.2F;
            if (this.crouchAmount > 3.0F) {
                this.crouchAmount = 3.0F;
            }
        } else {
            this.crouchAmount = 0.0F;
        }

    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return stack.is(TagsItem.FOX_FOOD);
    }

    @Override
    protected void onOffspringSpawnedFromEgg(EntityHuman player, EntityInsentient child) {
        ((EntityFox)child).addTrustedUUID(player.getUniqueID());
    }

    public boolean isPouncing() {
        return this.getFlag(16);
    }

    public void setIsPouncing(boolean chasing) {
        this.setFlag(16, chasing);
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public boolean isFullyCrouched() {
        return this.crouchAmount == 3.0F;
    }

    public void setCrouching(boolean crouching) {
        this.setFlag(4, crouching);
    }

    @Override
    public boolean isCrouching() {
        return this.getFlag(4);
    }

    public void setIsInterested(boolean rollingHead) {
        this.setFlag(8, rollingHead);
    }

    public boolean isInterested() {
        return this.getFlag(8);
    }

    public float getHeadRollAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.interestedAngleO, this.interestedAngle) * 0.11F * (float)Math.PI;
    }

    public float getCrouchAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.crouchAmountO, this.crouchAmount);
    }

    @Override
    public void setGoalTarget(@Nullable EntityLiving target) {
        if (this.isDefending() && target == null) {
            this.setDefending(false);
        }

        super.setGoalTarget(target);
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return MathHelper.ceil((fallDistance - 5.0F) * damageMultiplier);
    }

    void wakeUp() {
        this.setSleeping(false);
    }

    void clearStates() {
        this.setIsInterested(false);
        this.setCrouching(false);
        this.setSitting(false);
        this.setSleeping(false);
        this.setDefending(false);
        this.setFaceplanted(false);
    }

    boolean canMove() {
        return !this.isSleeping() && !this.isSitting() && !this.isFaceplanted();
    }

    @Override
    public void playAmbientSound() {
        SoundEffect soundEvent = this.getSoundAmbient();
        if (soundEvent == SoundEffects.FOX_SCREECH) {
            this.playSound(soundEvent, 2.0F, this.getVoicePitch());
        } else {
            super.playAmbientSound();
        }

    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.isSleeping()) {
            return SoundEffects.FOX_SLEEP;
        } else {
            if (!this.level.isDay() && this.random.nextFloat() < 0.1F) {
                List<EntityHuman> list = this.level.getEntitiesOfClass(EntityHuman.class, this.getBoundingBox().grow(16.0D, 16.0D, 16.0D), IEntitySelector.NO_SPECTATORS);
                if (list.isEmpty()) {
                    return SoundEffects.FOX_SCREECH;
                }
            }

            return SoundEffects.FOX_AMBIENT;
        }
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.FOX_HURT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.FOX_DEATH;
    }

    boolean trusts(UUID uuid) {
        return this.getTrustedUUIDs().contains(uuid);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        ItemStack itemStack = this.getEquipment(EnumItemSlot.MAINHAND);
        if (!itemStack.isEmpty()) {
            this.spawnAtLocation(itemStack);
            this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
        }

        super.dropAllDeathLoot(source);
    }

    public static boolean isPathClear(EntityFox fox, EntityLiving chasedEntity) {
        double d = chasedEntity.locZ() - fox.locZ();
        double e = chasedEntity.locX() - fox.locX();
        double f = d / e;
        int i = 6;

        for(int j = 0; j < 6; ++j) {
            double g = f == 0.0D ? 0.0D : d * (double)((float)j / 6.0F);
            double h = f == 0.0D ? e * (double)((float)j / 6.0F) : g / f;

            for(int k = 1; k < 4; ++k) {
                if (!fox.level.getType(new BlockPosition(fox.locX() + h, fox.locY() + (double)k, fox.locZ() + g)).getMaterial().isReplaceable()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.55F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    class DefendTrustedTargetGoal extends PathfinderGoalNearestAttackableTarget<EntityLiving> {
        @Nullable
        private EntityLiving trustedLastHurtBy;
        private EntityLiving trustedLastHurt;
        private int timestamp;

        public DefendTrustedTargetGoal(Class<EntityLiving> targetEntityClass, boolean checkVisibility, @Nullable boolean checkCanNavigate, Predicate<EntityLiving> targetPredicate) {
            super(EntityFox.this, targetEntityClass, 10, checkVisibility, checkCanNavigate, targetPredicate);
        }

        @Override
        public boolean canUse() {
            if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
                return false;
            } else {
                for(UUID uUID : EntityFox.this.getTrustedUUIDs()) {
                    if (uUID != null && EntityFox.this.level instanceof WorldServer) {
                        Entity entity = ((WorldServer)EntityFox.this.level).getEntity(uUID);
                        if (entity instanceof EntityLiving) {
                            EntityLiving livingEntity = (EntityLiving)entity;
                            this.trustedLastHurt = livingEntity;
                            this.trustedLastHurtBy = livingEntity.getLastDamager();
                            int i = livingEntity.getLastHurtByMobTimestamp();
                            return i != this.timestamp && this.canAttack(this.trustedLastHurtBy, this.targetConditions);
                        }
                    }
                }

                return false;
            }
        }

        @Override
        public void start() {
            this.setTarget(this.trustedLastHurtBy);
            this.target = this.trustedLastHurtBy;
            if (this.trustedLastHurt != null) {
                this.timestamp = this.trustedLastHurt.getLastHurtByMobTimestamp();
            }

            EntityFox.this.playSound(SoundEffects.FOX_AGGRO, 1.0F, 1.0F);
            EntityFox.this.setDefending(true);
            EntityFox.this.wakeUp();
            super.start();
        }
    }

    class FaceplantGoal extends PathfinderGoal {
        int countdown;

        public FaceplantGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK, PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            return EntityFox.this.isFaceplanted();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && this.countdown > 0;
        }

        @Override
        public void start() {
            this.countdown = 40;
        }

        @Override
        public void stop() {
            EntityFox.this.setFaceplanted(false);
        }

        @Override
        public void tick() {
            --this.countdown;
        }
    }

    public class FoxAlertableEntitiesSelector implements Predicate<EntityLiving> {
        @Override
        public boolean test(EntityLiving livingEntity) {
            if (livingEntity instanceof EntityFox) {
                return false;
            } else if (!(livingEntity instanceof EntityChicken) && !(livingEntity instanceof EntityRabbit) && !(livingEntity instanceof EntityMonster)) {
                if (livingEntity instanceof EntityTameableAnimal) {
                    return !((EntityTameableAnimal)livingEntity).isTamed();
                } else if (!(livingEntity instanceof EntityHuman) || !livingEntity.isSpectator() && !((EntityHuman)livingEntity).isCreative()) {
                    if (EntityFox.this.trusts(livingEntity.getUniqueID())) {
                        return false;
                    } else {
                        return !livingEntity.isSleeping() && !livingEntity.isDiscrete();
                    }
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    abstract class FoxBehaviorGoal extends PathfinderGoal {
        private final PathfinderTargetCondition alertableTargeting = PathfinderTargetCondition.forCombat().range(12.0D).ignoreLineOfSight().selector(EntityFox.this.new FoxAlertableEntitiesSelector());

        protected boolean hasShelter() {
            BlockPosition blockPos = new BlockPosition(EntityFox.this.locX(), EntityFox.this.getBoundingBox().maxY, EntityFox.this.locZ());
            return !EntityFox.this.level.canSeeSky(blockPos) && EntityFox.this.getWalkTargetValue(blockPos) >= 0.0F;
        }

        protected boolean alertable() {
            return !EntityFox.this.level.getNearbyEntities(EntityLiving.class, this.alertableTargeting, EntityFox.this, EntityFox.this.getBoundingBox().grow(12.0D, 6.0D, 12.0D)).isEmpty();
        }
    }

    class FoxBreedGoal extends PathfinderGoalBreed {
        public FoxBreedGoal(double chance) {
            super(EntityFox.this, chance);
        }

        @Override
        public void start() {
            ((EntityFox)this.animal).clearStates();
            ((EntityFox)this.partner).clearStates();
            super.start();
        }

        @Override
        protected void breed() {
            WorldServer serverLevel = (WorldServer)this.level;
            EntityFox fox = (EntityFox)this.animal.createChild(serverLevel, this.partner);
            if (fox != null) {
                EntityPlayer serverPlayer = this.animal.getBreedCause();
                EntityPlayer serverPlayer2 = this.partner.getBreedCause();
                EntityPlayer serverPlayer3 = serverPlayer;
                if (serverPlayer != null) {
                    fox.addTrustedUUID(serverPlayer.getUniqueID());
                } else {
                    serverPlayer3 = serverPlayer2;
                }

                if (serverPlayer2 != null && serverPlayer != serverPlayer2) {
                    fox.addTrustedUUID(serverPlayer2.getUniqueID());
                }

                if (serverPlayer3 != null) {
                    serverPlayer3.awardStat(StatisticList.ANIMALS_BRED);
                    CriterionTriggers.BRED_ANIMALS.trigger(serverPlayer3, this.animal, this.partner, fox);
                }

                this.animal.setAgeRaw(6000);
                this.partner.setAgeRaw(6000);
                this.animal.resetLove();
                this.partner.resetLove();
                fox.setAgeRaw(-24000);
                fox.setPositionRotation(this.animal.locX(), this.animal.locY(), this.animal.locZ(), 0.0F, 0.0F);
                serverLevel.addAllEntities(fox);
                this.level.broadcastEntityEffect(this.animal, (byte)18);
                if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                    this.level.addEntity(new EntityExperienceOrb(this.level, this.animal.locX(), this.animal.locY(), this.animal.locZ(), this.animal.getRandom().nextInt(7) + 1));
                }

            }
        }
    }

    public class FoxEatBerriesGoal extends PathfinderGoalGotoTarget {
        private static final int WAIT_TICKS = 40;
        protected int ticksWaited;

        public FoxEatBerriesGoal(double speed, int range, int maxYDifference) {
            super(EntityFox.this, speed, range, maxYDifference);
        }

        @Override
        public double acceptedDistance() {
            return 2.0D;
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            IBlockData blockState = world.getType(pos);
            return blockState.is(Blocks.SWEET_BERRY_BUSH) && blockState.get(BlockSweetBerryBush.AGE) >= 2 || CaveVines.hasGlowBerries(blockState);
        }

        @Override
        public void tick() {
            if (this.isReachedTarget()) {
                if (this.ticksWaited >= 40) {
                    this.onReachedTarget();
                } else {
                    ++this.ticksWaited;
                }
            } else if (!this.isReachedTarget() && EntityFox.this.random.nextFloat() < 0.05F) {
                EntityFox.this.playSound(SoundEffects.FOX_SNIFF, 1.0F, 1.0F);
            }

            super.tick();
        }

        protected void onReachedTarget() {
            if (EntityFox.this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                IBlockData blockState = EntityFox.this.level.getType(this.blockPos);
                if (blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                    this.pickSweetBerries(blockState);
                } else if (CaveVines.hasGlowBerries(blockState)) {
                    this.pickGlowBerry(blockState);
                }

            }
        }

        private void pickGlowBerry(IBlockData state) {
            CaveVines.harvest(state, EntityFox.this.level, this.blockPos);
        }

        private void pickSweetBerries(IBlockData state) {
            int i = state.get(BlockSweetBerryBush.AGE);
            state.set(BlockSweetBerryBush.AGE, Integer.valueOf(1));
            int j = 1 + EntityFox.this.level.random.nextInt(2) + (i == 3 ? 1 : 0);
            ItemStack itemStack = EntityFox.this.getEquipment(EnumItemSlot.MAINHAND);
            if (itemStack.isEmpty()) {
                EntityFox.this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
                --j;
            }

            if (j > 0) {
                Block.popResource(EntityFox.this.level, this.blockPos, new ItemStack(Items.SWEET_BERRIES, j));
            }

            EntityFox.this.playSound(SoundEffects.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
            EntityFox.this.level.setTypeAndData(this.blockPos, state.set(BlockSweetBerryBush.AGE, Integer.valueOf(1)), 2);
        }

        @Override
        public boolean canUse() {
            return !EntityFox.this.isSleeping() && super.canUse();
        }

        @Override
        public void start() {
            this.ticksWaited = 0;
            EntityFox.this.setSitting(false);
            super.start();
        }
    }

    class FoxFloatGoal extends PathfinderGoalFloat {
        public FoxFloatGoal() {
            super(EntityFox.this);
        }

        @Override
        public void start() {
            super.start();
            EntityFox.this.clearStates();
        }

        @Override
        public boolean canUse() {
            return EntityFox.this.isInWater() && EntityFox.this.getFluidHeight(TagsFluid.WATER) > 0.25D || EntityFox.this.isInLava();
        }
    }

    class FoxFollowParentGoal extends PathfinderGoalFollowParent {
        private final EntityFox fox;

        public FoxFollowParentGoal(EntityFox fox, double speed) {
            super(fox, speed);
            this.fox = fox;
        }

        @Override
        public boolean canUse() {
            return !this.fox.isDefending() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.fox.isDefending() && super.canContinueToUse();
        }

        @Override
        public void start() {
            this.fox.clearStates();
            super.start();
        }
    }

    public static class FoxGroupData extends EntityAgeable.GroupDataAgeable {
        public final EntityFox.Type type;

        public FoxGroupData(EntityFox.Type type) {
            super(false);
            this.type = type;
        }
    }

    class FoxLookAtPlayerGoal extends PathfinderGoalLookAtPlayer {
        public FoxLookAtPlayerGoal(EntityInsentient fox, Class<? extends EntityLiving> targetType, float range) {
            super(fox, targetType, range);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !EntityFox.this.isFaceplanted() && !EntityFox.this.isInterested();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && !EntityFox.this.isFaceplanted() && !EntityFox.this.isInterested();
        }
    }

    public class FoxLookControl extends ControllerLook {
        public FoxLookControl() {
            super(EntityFox.this);
        }

        @Override
        public void tick() {
            if (!EntityFox.this.isSleeping()) {
                super.tick();
            }

        }

        @Override
        protected boolean resetXRotOnTick() {
            return !EntityFox.this.isPouncing() && !EntityFox.this.isCrouching() && !EntityFox.this.isInterested() && !EntityFox.this.isFaceplanted();
        }
    }

    class FoxMeleeAttackGoal extends PathfinderGoalMeleeAttack {
        public FoxMeleeAttackGoal(double speed, boolean pauseWhenIdle) {
            super(EntityFox.this, speed, pauseWhenIdle);
        }

        @Override
        protected void checkAndPerformAttack(EntityLiving target, double squaredDistance) {
            double d = this.getAttackReachSqr(target);
            if (squaredDistance <= d && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.mob.attackEntity(target);
                EntityFox.this.playSound(SoundEffects.FOX_BITE, 1.0F, 1.0F);
            }

        }

        @Override
        public void start() {
            EntityFox.this.setIsInterested(false);
            super.start();
        }

        @Override
        public boolean canUse() {
            return !EntityFox.this.isSitting() && !EntityFox.this.isSleeping() && !EntityFox.this.isCrouching() && !EntityFox.this.isFaceplanted() && super.canUse();
        }
    }

    class FoxMoveControl extends ControllerMove {
        public FoxMoveControl() {
            super(EntityFox.this);
        }

        @Override
        public void tick() {
            if (EntityFox.this.canMove()) {
                super.tick();
            }

        }
    }

    class FoxPanicGoal extends PathfinderGoalPanic {
        public FoxPanicGoal(double speed) {
            super(EntityFox.this, speed);
        }

        @Override
        public boolean canUse() {
            return !EntityFox.this.isDefending() && super.canUse();
        }
    }

    public class FoxPounceGoal extends PathfinderGoalWaterJumpAbstract {
        @Override
        public boolean canUse() {
            if (!EntityFox.this.isFullyCrouched()) {
                return false;
            } else {
                EntityLiving livingEntity = EntityFox.this.getGoalTarget();
                if (livingEntity != null && livingEntity.isAlive()) {
                    if (livingEntity.getAdjustedDirection() != livingEntity.getDirection()) {
                        return false;
                    } else {
                        boolean bl = EntityFox.isPathClear(EntityFox.this, livingEntity);
                        if (!bl) {
                            EntityFox.this.getNavigation().createPath(livingEntity, 0);
                            EntityFox.this.setCrouching(false);
                            EntityFox.this.setIsInterested(false);
                        }

                        return bl;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            EntityLiving livingEntity = EntityFox.this.getGoalTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                double d = EntityFox.this.getMot().y;
                return (!(d * d < (double)0.05F) || !(Math.abs(EntityFox.this.getXRot()) < 15.0F) || !EntityFox.this.onGround) && !EntityFox.this.isFaceplanted();
            } else {
                return false;
            }
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            EntityFox.this.setJumping(true);
            EntityFox.this.setIsPouncing(true);
            EntityFox.this.setIsInterested(false);
            EntityLiving livingEntity = EntityFox.this.getGoalTarget();
            EntityFox.this.getControllerLook().setLookAt(livingEntity, 60.0F, 30.0F);
            Vec3D vec3 = (new Vec3D(livingEntity.locX() - EntityFox.this.locX(), livingEntity.locY() - EntityFox.this.locY(), livingEntity.locZ() - EntityFox.this.locZ())).normalize();
            EntityFox.this.setMot(EntityFox.this.getMot().add(vec3.x * 0.8D, 0.9D, vec3.z * 0.8D));
            EntityFox.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            EntityFox.this.setCrouching(false);
            EntityFox.this.crouchAmount = 0.0F;
            EntityFox.this.crouchAmountO = 0.0F;
            EntityFox.this.setIsInterested(false);
            EntityFox.this.setIsPouncing(false);
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = EntityFox.this.getGoalTarget();
            if (livingEntity != null) {
                EntityFox.this.getControllerLook().setLookAt(livingEntity, 60.0F, 30.0F);
            }

            if (!EntityFox.this.isFaceplanted()) {
                Vec3D vec3 = EntityFox.this.getMot();
                if (vec3.y * vec3.y < (double)0.03F && EntityFox.this.getXRot() != 0.0F) {
                    EntityFox.this.setXRot(MathHelper.rotlerp(EntityFox.this.getXRot(), 0.0F, 0.2F));
                } else {
                    double d = vec3.horizontalDistance();
                    double e = Math.signum(-vec3.y) * Math.acos(d / vec3.length()) * (double)(180F / (float)Math.PI);
                    EntityFox.this.setXRot((float)e);
                }
            }

            if (livingEntity != null && EntityFox.this.distanceTo(livingEntity) <= 2.0F) {
                EntityFox.this.attackEntity(livingEntity);
            } else if (EntityFox.this.getXRot() > 0.0F && EntityFox.this.onGround && (float)EntityFox.this.getMot().y != 0.0F && EntityFox.this.level.getType(EntityFox.this.getChunkCoordinates()).is(Blocks.SNOW)) {
                EntityFox.this.setXRot(60.0F);
                EntityFox.this.setGoalTarget((EntityLiving)null);
                EntityFox.this.setFaceplanted(true);
            }

        }
    }

    class FoxSearchForItemsGoal extends PathfinderGoal {
        public FoxSearchForItemsGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!EntityFox.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty()) {
                return false;
            } else if (EntityFox.this.getGoalTarget() == null && EntityFox.this.getLastDamager() == null) {
                if (!EntityFox.this.canMove()) {
                    return false;
                } else if (EntityFox.this.getRandom().nextInt(10) != 0) {
                    return false;
                } else {
                    List<EntityItem> list = EntityFox.this.level.getEntitiesOfClass(EntityItem.class, EntityFox.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityFox.ALLOWED_ITEMS);
                    return !list.isEmpty() && EntityFox.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty();
                }
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            List<EntityItem> list = EntityFox.this.level.getEntitiesOfClass(EntityItem.class, EntityFox.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityFox.ALLOWED_ITEMS);
            ItemStack itemStack = EntityFox.this.getEquipment(EnumItemSlot.MAINHAND);
            if (itemStack.isEmpty() && !list.isEmpty()) {
                EntityFox.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }

        @Override
        public void start() {
            List<EntityItem> list = EntityFox.this.level.getEntitiesOfClass(EntityItem.class, EntityFox.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityFox.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                EntityFox.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }
    }

    class FoxStrollThroughVillageGoal extends PathfinderGoalNearestVillage {
        public FoxStrollThroughVillageGoal(int unused, int searchRange) {
            super(EntityFox.this, searchRange);
        }

        @Override
        public void start() {
            EntityFox.this.clearStates();
            super.start();
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.canFoxMove();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.canFoxMove();
        }

        private boolean canFoxMove() {
            return !EntityFox.this.isSleeping() && !EntityFox.this.isSitting() && !EntityFox.this.isDefending() && EntityFox.this.getGoalTarget() == null;
        }
    }

    class PerchAndSearchGoal extends EntityFox.FoxBehaviorGoal {
        private double relX;
        private double relZ;
        private int lookTime;
        private int looksRemaining;

        public PerchAndSearchGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            return EntityFox.this.getLastDamager() == null && EntityFox.this.getRandom().nextFloat() < 0.02F && !EntityFox.this.isSleeping() && EntityFox.this.getGoalTarget() == null && EntityFox.this.getNavigation().isDone() && !this.alertable() && !EntityFox.this.isPouncing() && !EntityFox.this.isCrouching();
        }

        @Override
        public boolean canContinueToUse() {
            return this.looksRemaining > 0;
        }

        @Override
        public void start() {
            this.resetLook();
            this.looksRemaining = 2 + EntityFox.this.getRandom().nextInt(3);
            EntityFox.this.setSitting(true);
            EntityFox.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            EntityFox.this.setSitting(false);
        }

        @Override
        public void tick() {
            --this.lookTime;
            if (this.lookTime <= 0) {
                --this.looksRemaining;
                this.resetLook();
            }

            EntityFox.this.getControllerLook().setLookAt(EntityFox.this.locX() + this.relX, EntityFox.this.getHeadY(), EntityFox.this.locZ() + this.relZ, (float)EntityFox.this.getMaxHeadYRot(), (float)EntityFox.this.getMaxHeadXRot());
        }

        private void resetLook() {
            double d = (Math.PI * 2D) * EntityFox.this.getRandom().nextDouble();
            this.relX = Math.cos(d);
            this.relZ = Math.sin(d);
            this.lookTime = 80 + EntityFox.this.getRandom().nextInt(20);
        }
    }

    class SeekShelterGoal extends PathfinderGoalFleeSun {
        private int interval = 100;

        public SeekShelterGoal(double speed) {
            super(EntityFox.this, speed);
        }

        @Override
        public boolean canUse() {
            if (!EntityFox.this.isSleeping() && this.mob.getGoalTarget() == null) {
                if (EntityFox.this.level.isThundering()) {
                    return true;
                } else if (this.interval > 0) {
                    --this.interval;
                    return false;
                } else {
                    this.interval = 100;
                    BlockPosition blockPos = this.mob.getChunkCoordinates();
                    return EntityFox.this.level.isDay() && EntityFox.this.level.canSeeSky(blockPos) && !((WorldServer)EntityFox.this.level).isVillage(blockPos) && this.setWantedPos();
                }
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            EntityFox.this.clearStates();
            super.start();
        }
    }

    class SleepGoal extends EntityFox.FoxBehaviorGoal {
        private static final int WAIT_TIME_BEFORE_SLEEP = 140;
        private int countdown = EntityFox.this.random.nextInt(140);

        public SleepGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK, PathfinderGoal.Type.JUMP));
        }

        @Override
        public boolean canUse() {
            if (EntityFox.this.xxa == 0.0F && EntityFox.this.yya == 0.0F && EntityFox.this.zza == 0.0F) {
                return this.canSleep() || EntityFox.this.isSleeping();
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return EntityFox.this.level.isDay() && this.hasShelter() && !this.alertable() && !EntityFox.this.isInPowderSnow;
            }
        }

        @Override
        public void stop() {
            this.countdown = EntityFox.this.random.nextInt(140);
            EntityFox.this.clearStates();
        }

        @Override
        public void start() {
            EntityFox.this.setSitting(false);
            EntityFox.this.setCrouching(false);
            EntityFox.this.setIsInterested(false);
            EntityFox.this.setJumping(false);
            EntityFox.this.setSleeping(true);
            EntityFox.this.getNavigation().stop();
            EntityFox.this.getControllerMove().setWantedPosition(EntityFox.this.locX(), EntityFox.this.locY(), EntityFox.this.locZ(), 0.0D);
        }
    }

    class StalkPreyGoal extends PathfinderGoal {
        public StalkPreyGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            if (EntityFox.this.isSleeping()) {
                return false;
            } else {
                EntityLiving livingEntity = EntityFox.this.getGoalTarget();
                return livingEntity != null && livingEntity.isAlive() && EntityFox.STALKABLE_PREY.test(livingEntity) && EntityFox.this.distanceToSqr(livingEntity) > 36.0D && !EntityFox.this.isCrouching() && !EntityFox.this.isInterested() && !EntityFox.this.jumping;
            }
        }

        @Override
        public void start() {
            EntityFox.this.setSitting(false);
            EntityFox.this.setFaceplanted(false);
        }

        @Override
        public void stop() {
            EntityLiving livingEntity = EntityFox.this.getGoalTarget();
            if (livingEntity != null && EntityFox.isPathClear(EntityFox.this, livingEntity)) {
                EntityFox.this.setIsInterested(true);
                EntityFox.this.setCrouching(true);
                EntityFox.this.getNavigation().stop();
                EntityFox.this.getControllerLook().setLookAt(livingEntity, (float)EntityFox.this.getMaxHeadYRot(), (float)EntityFox.this.getMaxHeadXRot());
            } else {
                EntityFox.this.setIsInterested(false);
                EntityFox.this.setCrouching(false);
            }

        }

        @Override
        public void tick() {
            EntityLiving livingEntity = EntityFox.this.getGoalTarget();
            EntityFox.this.getControllerLook().setLookAt(livingEntity, (float)EntityFox.this.getMaxHeadYRot(), (float)EntityFox.this.getMaxHeadXRot());
            if (EntityFox.this.distanceToSqr(livingEntity) <= 36.0D) {
                EntityFox.this.setIsInterested(true);
                EntityFox.this.setCrouching(true);
                EntityFox.this.getNavigation().stop();
            } else {
                EntityFox.this.getNavigation().moveTo(livingEntity, 1.5D);
            }

        }
    }

    public static enum Type {
        RED(0, "red", Biomes.TAIGA, Biomes.TAIGA_HILLS, Biomes.TAIGA_MOUNTAINS, Biomes.GIANT_TREE_TAIGA, Biomes.GIANT_SPRUCE_TAIGA, Biomes.GIANT_TREE_TAIGA_HILLS, Biomes.GIANT_SPRUCE_TAIGA_HILLS),
        SNOW(1, "snow", Biomes.SNOWY_TAIGA, Biomes.SNOWY_TAIGA_HILLS, Biomes.SNOWY_TAIGA_MOUNTAINS);

        private static final EntityFox.Type[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(EntityFox.Type::getId)).toArray((i) -> {
            return new EntityFox.Type[i];
        });
        private static final Map<String, EntityFox.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(EntityFox.Type::getName, (type) -> {
            return type;
        }));
        private final int id;
        private final String name;
        private final List<ResourceKey<BiomeBase>> biomes;

        private Type(int id, String key, ResourceKey<BiomeBase>... biomes) {
            this.id = id;
            this.name = key;
            this.biomes = Arrays.asList(biomes);
        }

        public String getName() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public static EntityFox.Type byName(String name) {
            return BY_NAME.getOrDefault(name, RED);
        }

        public static EntityFox.Type byId(int id) {
            if (id < 0 || id > BY_ID.length) {
                id = 0;
            }

            return BY_ID[id];
        }

        public static EntityFox.Type byBiome(Optional<ResourceKey<BiomeBase>> biome) {
            return biome.isPresent() && SNOW.biomes.contains(biome.get()) ? SNOW : RED;
        }
    }
}
