package net.minecraft.world.entity.animal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMoveFlying;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowOwner;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPerch;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomFly;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSit;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityParrot extends EntityPerchable implements EntityBird {
    private static final DataWatcherObject<Integer> DATA_VARIANT_ID = DataWatcher.defineId(EntityParrot.class, DataWatcherRegistry.INT);
    private static final Predicate<EntityInsentient> NOT_PARROT_PREDICATE = new Predicate<EntityInsentient>() {
        @Override
        public boolean test(@Nullable EntityInsentient mob) {
            return mob != null && EntityParrot.MOB_SOUND_MAP.containsKey(mob.getEntityType());
        }
    };
    private static final Item POISONOUS_FOOD = Items.COOKIE;
    private static final Set<Item> TAME_FOOD = Sets.newHashSet(Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS);
    private static final int VARIANTS = 5;
    static final Map<EntityTypes<?>, SoundEffect> MOB_SOUND_MAP = SystemUtils.make(Maps.newHashMap(), (map) -> {
        map.put(EntityTypes.BLAZE, SoundEffects.PARROT_IMITATE_BLAZE);
        map.put(EntityTypes.CAVE_SPIDER, SoundEffects.PARROT_IMITATE_SPIDER);
        map.put(EntityTypes.CREEPER, SoundEffects.PARROT_IMITATE_CREEPER);
        map.put(EntityTypes.DROWNED, SoundEffects.PARROT_IMITATE_DROWNED);
        map.put(EntityTypes.ELDER_GUARDIAN, SoundEffects.PARROT_IMITATE_ELDER_GUARDIAN);
        map.put(EntityTypes.ENDER_DRAGON, SoundEffects.PARROT_IMITATE_ENDER_DRAGON);
        map.put(EntityTypes.ENDERMITE, SoundEffects.PARROT_IMITATE_ENDERMITE);
        map.put(EntityTypes.EVOKER, SoundEffects.PARROT_IMITATE_EVOKER);
        map.put(EntityTypes.GHAST, SoundEffects.PARROT_IMITATE_GHAST);
        map.put(EntityTypes.GUARDIAN, SoundEffects.PARROT_IMITATE_GUARDIAN);
        map.put(EntityTypes.HOGLIN, SoundEffects.PARROT_IMITATE_HOGLIN);
        map.put(EntityTypes.HUSK, SoundEffects.PARROT_IMITATE_HUSK);
        map.put(EntityTypes.ILLUSIONER, SoundEffects.PARROT_IMITATE_ILLUSIONER);
        map.put(EntityTypes.MAGMA_CUBE, SoundEffects.PARROT_IMITATE_MAGMA_CUBE);
        map.put(EntityTypes.PHANTOM, SoundEffects.PARROT_IMITATE_PHANTOM);
        map.put(EntityTypes.PIGLIN, SoundEffects.PARROT_IMITATE_PIGLIN);
        map.put(EntityTypes.PIGLIN_BRUTE, SoundEffects.PARROT_IMITATE_PIGLIN_BRUTE);
        map.put(EntityTypes.PILLAGER, SoundEffects.PARROT_IMITATE_PILLAGER);
        map.put(EntityTypes.RAVAGER, SoundEffects.PARROT_IMITATE_RAVAGER);
        map.put(EntityTypes.SHULKER, SoundEffects.PARROT_IMITATE_SHULKER);
        map.put(EntityTypes.SILVERFISH, SoundEffects.PARROT_IMITATE_SILVERFISH);
        map.put(EntityTypes.SKELETON, SoundEffects.PARROT_IMITATE_SKELETON);
        map.put(EntityTypes.SLIME, SoundEffects.PARROT_IMITATE_SLIME);
        map.put(EntityTypes.SPIDER, SoundEffects.PARROT_IMITATE_SPIDER);
        map.put(EntityTypes.STRAY, SoundEffects.PARROT_IMITATE_STRAY);
        map.put(EntityTypes.VEX, SoundEffects.PARROT_IMITATE_VEX);
        map.put(EntityTypes.VINDICATOR, SoundEffects.PARROT_IMITATE_VINDICATOR);
        map.put(EntityTypes.WITCH, SoundEffects.PARROT_IMITATE_WITCH);
        map.put(EntityTypes.WITHER, SoundEffects.PARROT_IMITATE_WITHER);
        map.put(EntityTypes.WITHER_SKELETON, SoundEffects.PARROT_IMITATE_WITHER_SKELETON);
        map.put(EntityTypes.ZOGLIN, SoundEffects.PARROT_IMITATE_ZOGLIN);
        map.put(EntityTypes.ZOMBIE, SoundEffects.PARROT_IMITATE_ZOMBIE);
        map.put(EntityTypes.ZOMBIE_VILLAGER, SoundEffects.PARROT_IMITATE_ZOMBIE_VILLAGER);
    });
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0F;
    private float nextFlap = 1.0F;
    private boolean partyParrot;
    private BlockPosition jukebox;

    public EntityParrot(EntityTypes<? extends EntityParrot> type, World world) {
        super(type, world);
        this.moveControl = new ControllerMoveFlying(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setVariant(this.random.nextInt(5));
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(false);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalPanic(this, 1.25D));
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(2, new PathfinderGoalSit(this));
        this.goalSelector.addGoal(2, new PathfinderGoalFollowOwner(this, 1.0D, 5.0F, 1.0F, true));
        this.goalSelector.addGoal(2, new PathfinderGoalRandomFly(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalPerch(this));
        this.goalSelector.addGoal(3, new PathfinderGoalFollowEntity(this, 1.0D, 3.0F, 7.0F));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 6.0D).add(GenericAttributes.FLYING_SPEED, (double)0.4F).add(GenericAttributes.MOVEMENT_SPEED, (double)0.2F);
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
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.6F;
    }

    @Override
    public void movementTick() {
        if (this.jukebox == null || !this.jukebox.closerThan(this.getPositionVector(), 3.46D) || !this.level.getType(this.jukebox).is(Blocks.JUKEBOX)) {
            this.partyParrot = false;
            this.jukebox = null;
        }

        if (this.level.random.nextInt(400) == 0) {
            imitateNearbyMobs(this.level, this);
        }

        super.movementTick();
        this.calculateFlapping();
    }

    @Override
    public void setRecordPlayingNearby(BlockPosition songPosition, boolean playing) {
        this.jukebox = songPosition;
        this.partyParrot = playing;
    }

    public boolean isPartyParrot() {
        return this.partyParrot;
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = (float)((double)this.flapSpeed + (double)(!this.onGround && !this.isPassenger() ? 4 : -1) * 0.3D);
        this.flapSpeed = MathHelper.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping = (float)((double)this.flapping * 0.9D);
        Vec3D vec3 = this.getMot();
        if (!this.onGround && vec3.y < 0.0D) {
            this.setMot(vec3.multiply(1.0D, 0.6D, 1.0D));
        }

        this.flap += this.flapping * 2.0F;
    }

    public static boolean imitateNearbyMobs(World world, Entity parrot) {
        if (parrot.isAlive() && !parrot.isSilent() && world.random.nextInt(2) == 0) {
            List<EntityInsentient> list = world.getEntitiesOfClass(EntityInsentient.class, parrot.getBoundingBox().inflate(20.0D), NOT_PARROT_PREDICATE);
            if (!list.isEmpty()) {
                EntityInsentient mob = list.get(world.random.nextInt(list.size()));
                if (!mob.isSilent()) {
                    SoundEffect soundEvent = getImitatedSound(mob.getEntityType());
                    world.playSound((EntityHuman)null, parrot.locX(), parrot.locY(), parrot.locZ(), soundEvent, parrot.getSoundCategory(), 0.7F, getPitch(world.random));
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isTamed() && TAME_FOOD.contains(itemStack.getItem())) {
            if (!player.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            if (!this.isSilent()) {
                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PARROT_EAT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }

            if (!this.level.isClientSide) {
                if (this.random.nextInt(10) == 0) {
                    this.tame(player);
                    this.level.broadcastEntityEffect(this, (byte)7);
                } else {
                    this.level.broadcastEntityEffect(this, (byte)6);
                }
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (itemStack.is(POISONOUS_FOOD)) {
            if (!player.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            this.addEffect(new MobEffect(MobEffects.POISON, 900));
            if (player.isCreative() || !this.isInvulnerable()) {
                this.damageEntity(DamageSource.playerAttack(player), Float.MAX_VALUE);
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (!this.isFlying() && this.isTamed() && this.isOwnedBy(player)) {
            if (!this.level.isClientSide) {
                this.setWillSit(!this.isWillSit());
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return false;
    }

    public static boolean checkParrotSpawnRules(EntityTypes<EntityParrot> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        IBlockData blockState = world.getType(pos.below());
        return (blockState.is(TagsBlock.LEAVES) || blockState.is(Blocks.GRASS_BLOCK) || blockState.is(TagsBlock.LOGS) || blockState.is(Blocks.AIR)) && world.getLightLevel(pos, 0) > 8;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
    }

    @Override
    public boolean mate(EntityAnimal other) {
        return false;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return null;
    }

    @Override
    public boolean attackEntity(Entity target) {
        return target.damageEntity(DamageSource.mobAttack(this), 3.0F);
    }

    @Nullable
    @Override
    public SoundEffect getSoundAmbient() {
        return getAmbient(this.level, this.level.random);
    }

    public static SoundEffect getAmbient(World world, Random random) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL && random.nextInt(1000) == 0) {
            List<EntityTypes<?>> list = Lists.newArrayList(MOB_SOUND_MAP.keySet());
            return getImitatedSound(list.get(random.nextInt(list.size())));
        } else {
            return SoundEffects.PARROT_AMBIENT;
        }
    }

    private static SoundEffect getImitatedSound(EntityTypes<?> imitate) {
        return MOB_SOUND_MAP.getOrDefault(imitate, SoundEffects.PARROT_AMBIENT);
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PARROT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PARROT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.PARROT_STEP, 0.15F, 1.0F);
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(SoundEffects.PARROT_FLY, 0.15F, 1.0F);
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return getPitch(this.random);
    }

    public static float getPitch(Random random) {
        return (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!(entity instanceof EntityHuman)) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            this.setWillSit(false);
            return super.damageEntity(source, amount);
        }
    }

    public int getVariant() {
        return MathHelper.clamp(this.entityData.get(DATA_VARIANT_ID), 0, 4);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_VARIANT_ID, 0);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Variant", this.getVariant());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setVariant(nbt.getInt("Variant"));
    }

    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.5F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }
}
