package net.minecraft.world.entity.animal;

import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.ControllerMoveFlying;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceRecord;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCrops;
import net.minecraft.world.level.block.BlockStem;
import net.minecraft.world.level.block.BlockSweetBerryBush;
import net.minecraft.world.level.block.BlockTallPlant;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IBlockFragilePlantElement;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityBee extends EntityAnimal implements IEntityAngerable, EntityBird {
    public static final float FLAP_DEGREES_PER_TICK = 120.32113F;
    public static final int TICKS_PER_FLAP = MathHelper.ceil(1.4959966F);
    private static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityBee.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Integer> DATA_REMAINING_ANGER_TIME = DataWatcher.defineId(EntityBee.class, DataWatcherRegistry.INT);
    private static final int FLAG_ROLL = 2;
    private static final int FLAG_HAS_STUNG = 4;
    private static final int FLAG_HAS_NECTAR = 8;
    private static final int STING_DEATH_COUNTDOWN = 1200;
    private static final int TICKS_BEFORE_GOING_TO_KNOWN_FLOWER = 2400;
    private static final int TICKS_WITHOUT_NECTAR_BEFORE_GOING_HOME = 3600;
    private static final int MIN_ATTACK_DIST = 4;
    private static final int MAX_CROPS_GROWABLE = 10;
    private static final int POISON_SECONDS_NORMAL = 10;
    private static final int POISON_SECONDS_HARD = 18;
    private static final int TOO_FAR_DISTANCE = 32;
    private static final int HIVE_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int PATHFIND_TO_HIVE_WHEN_CLOSER_THAN = 16;
    private static final int HIVE_SEARCH_DISTANCE = 20;
    public static final String TAG_CROPS_GROWN_SINCE_POLLINATION = "CropsGrownSincePollination";
    public static final String TAG_CANNOT_ENTER_HIVE_TICKS = "CannotEnterHiveTicks";
    public static final String TAG_TICKS_SINCE_POLLINATION = "TicksSincePollination";
    public static final String TAG_HAS_STUNG = "HasStung";
    public static final String TAG_HAS_NECTAR = "HasNectar";
    public static final String TAG_FLOWER_POS = "FlowerPos";
    public static final String TAG_HIVE_POS = "HivePos";
    private static final IntProviderUniform PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    @Nullable
    private UUID persistentAngerTarget;
    private float rollAmount;
    private float rollAmountO;
    private int timeSinceSting;
    int ticksWithoutNectarSinceExitingHive;
    public int stayOutOfHiveCountdown;
    private int numCropsGrownSincePollination;
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_HIVE = 200;
    int remainingCooldownBeforeLocatingNewHive;
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_FLOWER = 200;
    int remainingCooldownBeforeLocatingNewFlower = MathHelper.nextInt(this.random, 20, 60);
    @Nullable
    BlockPosition savedFlowerPos;
    @Nullable
    public BlockPosition hivePos;
    EntityBee.BeePollinateGoal beePollinateGoal;
    EntityBee.BeeGoToHiveGoal goToHiveGoal;
    private EntityBee.BeeGoToKnownFlowerGoal goToKnownFlowerGoal;
    private int underWaterTicks;

    public EntityBee(EntityTypes<? extends EntityBee> type, World world) {
        super(type, world);
        this.moveControl = new ControllerMoveFlying(this, 20, true);
        this.lookControl = new EntityBee.BeeLookControl(this);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
        this.setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
        this.entityData.register(DATA_REMAINING_ANGER_TIME, 0);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return world.getType(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new EntityBee.BeeAttackGoal(this, (double)1.4F, true));
        this.goalSelector.addGoal(1, new EntityBee.BeeEnterHiveGoal());
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalTempt(this, 1.25D, RecipeItemStack.of(TagsItem.FLOWERS), false));
        this.beePollinateGoal = new EntityBee.BeePollinateGoal();
        this.goalSelector.addGoal(4, this.beePollinateGoal);
        this.goalSelector.addGoal(5, new PathfinderGoalFollowParent(this, 1.25D));
        this.goalSelector.addGoal(5, new EntityBee.BeeLocateHiveGoal());
        this.goToHiveGoal = new EntityBee.BeeGoToHiveGoal();
        this.goalSelector.addGoal(5, this.goToHiveGoal);
        this.goToKnownFlowerGoal = new EntityBee.BeeGoToKnownFlowerGoal();
        this.goalSelector.addGoal(6, this.goToKnownFlowerGoal);
        this.goalSelector.addGoal(7, new EntityBee.BeeGrowCropGoal());
        this.goalSelector.addGoal(8, new EntityBee.BeeWanderGoal());
        this.goalSelector.addGoal(9, new PathfinderGoalFloat(this));
        this.targetSelector.addGoal(1, (new EntityBee.BeeHurtByOtherGoal(this)).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new EntityBee.BeeBecomeAngryTargetGoal(this));
        this.targetSelector.addGoal(3, new PathfinderGoalUniversalAngerReset<>(this, true));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.hasHivePos()) {
            nbt.set("HivePos", GameProfileSerializer.writeBlockPos(this.getHivePos()));
        }

        if (this.hasFlowerPos()) {
            nbt.set("FlowerPos", GameProfileSerializer.writeBlockPos(this.getFlowerPos()));
        }

        nbt.setBoolean("HasNectar", this.hasNectar());
        nbt.setBoolean("HasStung", this.hasStung());
        nbt.setInt("TicksSincePollination", this.ticksWithoutNectarSinceExitingHive);
        nbt.setInt("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
        nbt.setInt("CropsGrownSincePollination", this.numCropsGrownSincePollination);
        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.hivePos = null;
        if (nbt.hasKey("HivePos")) {
            this.hivePos = GameProfileSerializer.readBlockPos(nbt.getCompound("HivePos"));
        }

        this.savedFlowerPos = null;
        if (nbt.hasKey("FlowerPos")) {
            this.savedFlowerPos = GameProfileSerializer.readBlockPos(nbt.getCompound("FlowerPos"));
        }

        super.loadData(nbt);
        this.setHasNectar(nbt.getBoolean("HasNectar"));
        this.setHasStung(nbt.getBoolean("HasStung"));
        this.ticksWithoutNectarSinceExitingHive = nbt.getInt("TicksSincePollination");
        this.stayOutOfHiveCountdown = nbt.getInt("CannotEnterHiveTicks");
        this.numCropsGrownSincePollination = nbt.getInt("CropsGrownSincePollination");
        this.readPersistentAngerSaveData(this.level, nbt);
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = target.damageEntity(DamageSource.sting(this), (float)((int)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
            if (target instanceof EntityLiving) {
                ((EntityLiving)target).setStingerCount(((EntityLiving)target).getStingerCount() + 1);
                int i = 0;
                if (this.level.getDifficulty() == EnumDifficulty.NORMAL) {
                    i = 10;
                } else if (this.level.getDifficulty() == EnumDifficulty.HARD) {
                    i = 18;
                }

                if (i > 0) {
                    ((EntityLiving)target).addEffect(new MobEffect(MobEffectList.POISON, i * 20, 0), this);
                }
            }

            this.setHasStung(true);
            this.pacify();
            this.playSound(SoundEffects.BEE_STING, 1.0F, 1.0F);
        }

        return bl;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hasNectar() && this.getNumCropsGrownSincePollination() < 10 && this.random.nextFloat() < 0.05F) {
            for(int i = 0; i < this.random.nextInt(2) + 1; ++i) {
                this.spawnFluidParticle(this.level, this.locX() - (double)0.3F, this.locX() + (double)0.3F, this.locZ() - (double)0.3F, this.locZ() + (double)0.3F, this.getY(0.5D), Particles.FALLING_NECTAR);
            }
        }

        this.updateRollAmount();
    }

    private void spawnFluidParticle(World world, double lastX, double x, double lastZ, double z, double y, ParticleParam effect) {
        world.addParticle(effect, MathHelper.lerp(world.random.nextDouble(), lastX, x), y, MathHelper.lerp(world.random.nextDouble(), lastZ, z), 0.0D, 0.0D, 0.0D);
    }

    void pathfindRandomlyTowards(BlockPosition pos) {
        Vec3D vec3 = Vec3D.atBottomCenterOf(pos);
        int i = 0;
        BlockPosition blockPos = this.getChunkCoordinates();
        int j = (int)vec3.y - blockPos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int m = blockPos.distManhattan(pos);
        if (m < 15) {
            k = m / 2;
            l = m / 2;
        }

        Vec3D vec32 = AirRandomPos.getPosTowards(this, k, l, i, vec3, (double)((float)Math.PI / 10F));
        if (vec32 != null) {
            this.navigation.setMaxVisitedNodesMultiplier(0.5F);
            this.navigation.moveTo(vec32.x, vec32.y, vec32.z, 1.0D);
        }
    }

    @Nullable
    public BlockPosition getFlowerPos() {
        return this.savedFlowerPos;
    }

    public boolean hasFlowerPos() {
        return this.savedFlowerPos != null;
    }

    public void setFlowerPos(BlockPosition pos) {
        this.savedFlowerPos = pos;
    }

    @VisibleForDebug
    public int getTravellingTicks() {
        return Math.max(this.goToHiveGoal.travellingTicks, this.goToKnownFlowerGoal.travellingTicks);
    }

    @VisibleForDebug
    public List<BlockPosition> getBlacklistedHives() {
        return this.goToHiveGoal.blacklistedTargets;
    }

    private boolean canPollinate() {
        return this.ticksWithoutNectarSinceExitingHive > 3600;
    }

    boolean wantsToEnterHive() {
        if (this.stayOutOfHiveCountdown <= 0 && !this.beePollinateGoal.isPollinating() && !this.hasStung() && this.getGoalTarget() == null) {
            boolean bl = this.canPollinate() || this.level.isRaining() || this.level.isNight() || this.hasNectar();
            return bl && !this.isHiveNearFire();
        } else {
            return false;
        }
    }

    public void setCannotEnterHiveTicks(int ticks) {
        this.stayOutOfHiveCountdown = ticks;
    }

    public float getRollAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.rollAmountO, this.rollAmount);
    }

    private void updateRollAmount() {
        this.rollAmountO = this.rollAmount;
        if (this.isRolling()) {
            this.rollAmount = Math.min(1.0F, this.rollAmount + 0.2F);
        } else {
            this.rollAmount = Math.max(0.0F, this.rollAmount - 0.24F);
        }

    }

    @Override
    protected void mobTick() {
        boolean bl = this.hasStung();
        if (this.isInWaterOrBubble()) {
            ++this.underWaterTicks;
        } else {
            this.underWaterTicks = 0;
        }

        if (this.underWaterTicks > 20) {
            this.damageEntity(DamageSource.DROWN, 1.0F);
        }

        if (bl) {
            ++this.timeSinceSting;
            if (this.timeSinceSting % 5 == 0 && this.random.nextInt(MathHelper.clamp(1200 - this.timeSinceSting, 1, 1200)) == 0) {
                this.damageEntity(DamageSource.GENERIC, this.getHealth());
            }
        }

        if (!this.hasNectar()) {
            ++this.ticksWithoutNectarSinceExitingHive;
        }

        if (!this.level.isClientSide) {
            this.updatePersistentAnger((WorldServer)this.level, false);
        }

    }

    public void resetTicksWithoutNectarSinceExitingHive() {
        this.ticksWithoutNectarSinceExitingHive = 0;
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) {
            return false;
        } else {
            TileEntity blockEntity = this.level.getTileEntity(this.hivePos);
            return blockEntity instanceof TileEntityBeehive && ((TileEntityBeehive)blockEntity).isFireNearby();
        }
    }

    @Override
    public int getAnger() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Override
    public void setAnger(int ticks) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, ticks);
    }

    @Nullable
    @Override
    public UUID getAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    private boolean doesHiveHaveSpace(BlockPosition pos) {
        TileEntity blockEntity = this.level.getTileEntity(pos);
        if (blockEntity instanceof TileEntityBeehive) {
            return !((TileEntityBeehive)blockEntity).isFull();
        } else {
            return false;
        }
    }

    @VisibleForDebug
    public boolean hasHivePos() {
        return this.hivePos != null;
    }

    @Nullable
    @VisibleForDebug
    public BlockPosition getHivePos() {
        return this.hivePos;
    }

    @VisibleForDebug
    public PathfinderGoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendBeeInfo(this);
    }

    int getNumCropsGrownSincePollination() {
        return this.numCropsGrownSincePollination;
    }

    private void resetNumCropsGrownSincePollination() {
        this.numCropsGrownSincePollination = 0;
    }

    void incrementNumCropsGrownSincePollination() {
        ++this.numCropsGrownSincePollination;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (!this.level.isClientSide) {
            if (this.stayOutOfHiveCountdown > 0) {
                --this.stayOutOfHiveCountdown;
            }

            if (this.remainingCooldownBeforeLocatingNewHive > 0) {
                --this.remainingCooldownBeforeLocatingNewHive;
            }

            if (this.remainingCooldownBeforeLocatingNewFlower > 0) {
                --this.remainingCooldownBeforeLocatingNewFlower;
            }

            boolean bl = this.isAngry() && !this.hasStung() && this.getGoalTarget() != null && this.getGoalTarget().distanceToSqr(this) < 4.0D;
            this.setRolling(bl);
            if (this.tickCount % 20 == 0 && !this.isHiveValid()) {
                this.hivePos = null;
            }
        }

    }

    boolean isHiveValid() {
        if (!this.hasHivePos()) {
            return false;
        } else {
            TileEntity blockEntity = this.level.getTileEntity(this.hivePos);
            return blockEntity != null && blockEntity.getTileType() == TileEntityTypes.BEEHIVE;
        }
    }

    public boolean hasNectar() {
        return this.getFlag(8);
    }

    public void setHasNectar(boolean hasNectar) {
        if (hasNectar) {
            this.resetTicksWithoutNectarSinceExitingHive();
        }

        this.setFlag(8, hasNectar);
    }

    public boolean hasStung() {
        return this.getFlag(4);
    }

    public void setHasStung(boolean hasStung) {
        this.setFlag(4, hasStung);
    }

    private boolean isRolling() {
        return this.getFlag(2);
    }

    private void setRolling(boolean nearTarget) {
        this.setFlag(2, nearTarget);
    }

    boolean isTooFarAway(BlockPosition pos) {
        return !this.closerThan(pos, 32);
    }

    private void setFlag(int bit, boolean value) {
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | bit));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~bit));
        }

    }

    private boolean getFlag(int location) {
        return (this.entityData.get(DATA_FLAGS_ID) & location) != 0;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.FLYING_SPEED, (double)0.6F).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.ATTACK_DAMAGE, 2.0D).add(GenericAttributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        NavigationFlying flyingPathNavigation = new NavigationFlying(this, world) {
            @Override
            public boolean isStableDestination(BlockPosition pos) {
                return !this.level.getType(pos.below()).isAir();
            }

            @Override
            public void tick() {
                if (!EntityBee.this.beePollinateGoal.isPollinating()) {
                    super.tick();
                }
            }
        };
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(false);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return stack.is(TagsItem.FLOWERS);
    }

    boolean isFlowerValid(BlockPosition pos) {
        return this.level.isLoaded(pos) && this.level.getType(pos).is(TagsBlock.FLOWERS);
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return null;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.BEE_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.BEE_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public EntityBee getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        return EntityTypes.BEE.create(serverLevel);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? dimensions.height * 0.5F : dimensions.height * 0.5F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
    }

    @Override
    public boolean isFlapping() {
        return this.isFlying() && this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    public void dropOffNectar() {
        this.setHasNectar(false);
        this.resetNumCropsGrownSincePollination();
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            if (!this.level.isClientSide) {
                this.beePollinateGoal.stopPollinating();
            }

            return super.damageEntity(source, amount);
        }
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.ARTHROPOD;
    }

    @Override
    protected void jumpInLiquid(Tag<FluidType> fluid) {
        this.setMot(this.getMot().add(0.0D, 0.01D, 0.0D));
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.5F * this.getHeadHeight()), (double)(this.getWidth() * 0.2F));
    }

    boolean closerThan(BlockPosition pos, int distance) {
        return pos.closerThan(this.getChunkCoordinates(), (double)distance);
    }

    abstract class BaseBeeGoal extends PathfinderGoal {
        public abstract boolean canBeeUse();

        public abstract boolean canBeeContinueToUse();

        @Override
        public boolean canUse() {
            return this.canBeeUse() && !EntityBee.this.isAngry();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canBeeContinueToUse() && !EntityBee.this.isAngry();
        }
    }

    class BeeAttackGoal extends PathfinderGoalMeleeAttack {
        BeeAttackGoal(EntityCreature mob, double speed, boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && EntityBee.this.isAngry() && !EntityBee.this.hasStung();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && EntityBee.this.isAngry() && !EntityBee.this.hasStung();
        }
    }

    static class BeeBecomeAngryTargetGoal extends PathfinderGoalNearestAttackableTarget<EntityHuman> {
        BeeBecomeAngryTargetGoal(EntityBee bee) {
            super(bee, EntityHuman.class, 10, true, false, bee::isAngryAt);
        }

        @Override
        public boolean canUse() {
            return this.beeCanTarget() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            boolean bl = this.beeCanTarget();
            if (bl && this.mob.getGoalTarget() != null) {
                return super.canContinueToUse();
            } else {
                this.targetMob = null;
                return false;
            }
        }

        private boolean beeCanTarget() {
            EntityBee bee = (EntityBee)this.mob;
            return bee.isAngry() && !bee.hasStung();
        }
    }

    class BeeEnterHiveGoal extends EntityBee.BaseBeeGoal {
        @Override
        public boolean canBeeUse() {
            if (EntityBee.this.hasHivePos() && EntityBee.this.wantsToEnterHive() && EntityBee.this.hivePos.closerThan(EntityBee.this.getPositionVector(), 2.0D)) {
                TileEntity blockEntity = EntityBee.this.level.getTileEntity(EntityBee.this.hivePos);
                if (blockEntity instanceof TileEntityBeehive) {
                    TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
                    if (!beehiveBlockEntity.isFull()) {
                        return true;
                    }

                    EntityBee.this.hivePos = null;
                }
            }

            return false;
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            TileEntity blockEntity = EntityBee.this.level.getTileEntity(EntityBee.this.hivePos);
            if (blockEntity instanceof TileEntityBeehive) {
                TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
                beehiveBlockEntity.addBee(EntityBee.this, EntityBee.this.hasNectar());
            }

        }
    }

    @VisibleForDebug
    public class BeeGoToHiveGoal extends EntityBee.BaseBeeGoal {
        public static final int MAX_TRAVELLING_TICKS = 600;
        int travellingTicks = EntityBee.this.level.random.nextInt(10);
        private static final int MAX_BLACKLISTED_TARGETS = 3;
        final List<BlockPosition> blacklistedTargets = Lists.newArrayList();
        @Nullable
        private PathEntity lastPath;
        private static final int TICKS_BEFORE_HIVE_DROP = 60;
        private int ticksStuck;

        BeeGoToHiveGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            return EntityBee.this.hivePos != null && !EntityBee.this.hasRestriction() && EntityBee.this.wantsToEnterHive() && !this.hasReachedTarget(EntityBee.this.hivePos) && EntityBee.this.level.getType(EntityBee.this.hivePos).is(TagsBlock.BEEHIVES);
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void start() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            super.start();
        }

        @Override
        public void stop() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            EntityBee.this.navigation.stop();
            EntityBee.this.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            if (EntityBee.this.hivePos != null) {
                ++this.travellingTicks;
                if (this.travellingTicks > this.adjustedTickDelay(600)) {
                    this.dropAndBlacklistHive();
                } else if (!EntityBee.this.navigation.isInProgress()) {
                    if (!EntityBee.this.closerThan(EntityBee.this.hivePos, 16)) {
                        if (EntityBee.this.isTooFarAway(EntityBee.this.hivePos)) {
                            this.dropHive();
                        } else {
                            EntityBee.this.pathfindRandomlyTowards(EntityBee.this.hivePos);
                        }
                    } else {
                        boolean bl = this.pathfindDirectlyTowards(EntityBee.this.hivePos);
                        if (!bl) {
                            this.dropAndBlacklistHive();
                        } else if (this.lastPath != null && EntityBee.this.navigation.getPath().sameAs(this.lastPath)) {
                            ++this.ticksStuck;
                            if (this.ticksStuck > 60) {
                                this.dropHive();
                                this.ticksStuck = 0;
                            }
                        } else {
                            this.lastPath = EntityBee.this.navigation.getPath();
                        }

                    }
                }
            }
        }

        private boolean pathfindDirectlyTowards(BlockPosition pos) {
            EntityBee.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
            EntityBee.this.navigation.moveTo((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 1.0D);
            return EntityBee.this.navigation.getPath() != null && EntityBee.this.navigation.getPath().canReach();
        }

        boolean isTargetBlacklisted(BlockPosition pos) {
            return this.blacklistedTargets.contains(pos);
        }

        private void blacklistTarget(BlockPosition pos) {
            this.blacklistedTargets.add(pos);

            while(this.blacklistedTargets.size() > 3) {
                this.blacklistedTargets.remove(0);
            }

        }

        void clearBlacklist() {
            this.blacklistedTargets.clear();
        }

        private void dropAndBlacklistHive() {
            if (EntityBee.this.hivePos != null) {
                this.blacklistTarget(EntityBee.this.hivePos);
            }

            this.dropHive();
        }

        private void dropHive() {
            EntityBee.this.hivePos = null;
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
        }

        private boolean hasReachedTarget(BlockPosition pos) {
            if (EntityBee.this.closerThan(pos, 2)) {
                return true;
            } else {
                PathEntity path = EntityBee.this.navigation.getPath();
                return path != null && path.getTarget().equals(pos) && path.canReach() && path.isDone();
            }
        }
    }

    public class BeeGoToKnownFlowerGoal extends EntityBee.BaseBeeGoal {
        private static final int MAX_TRAVELLING_TICKS = 600;
        int travellingTicks = EntityBee.this.level.random.nextInt(10);

        BeeGoToKnownFlowerGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            return EntityBee.this.savedFlowerPos != null && !EntityBee.this.hasRestriction() && this.wantsToGoToKnownFlower() && EntityBee.this.isFlowerValid(EntityBee.this.savedFlowerPos) && !EntityBee.this.closerThan(EntityBee.this.savedFlowerPos, 2);
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void start() {
            this.travellingTicks = 0;
            super.start();
        }

        @Override
        public void stop() {
            this.travellingTicks = 0;
            EntityBee.this.navigation.stop();
            EntityBee.this.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            if (EntityBee.this.savedFlowerPos != null) {
                ++this.travellingTicks;
                if (this.travellingTicks > this.adjustedTickDelay(600)) {
                    EntityBee.this.savedFlowerPos = null;
                } else if (!EntityBee.this.navigation.isInProgress()) {
                    if (EntityBee.this.isTooFarAway(EntityBee.this.savedFlowerPos)) {
                        EntityBee.this.savedFlowerPos = null;
                    } else {
                        EntityBee.this.pathfindRandomlyTowards(EntityBee.this.savedFlowerPos);
                    }
                }
            }
        }

        private boolean wantsToGoToKnownFlower() {
            return EntityBee.this.ticksWithoutNectarSinceExitingHive > 2400;
        }
    }

    class BeeGrowCropGoal extends EntityBee.BaseBeeGoal {
        static final int GROW_CHANCE = 30;

        @Override
        public boolean canBeeUse() {
            if (EntityBee.this.getNumCropsGrownSincePollination() >= 10) {
                return false;
            } else if (EntityBee.this.random.nextFloat() < 0.3F) {
                return false;
            } else {
                return EntityBee.this.hasNectar() && EntityBee.this.isHiveValid();
            }
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void tick() {
            if (EntityBee.this.random.nextInt(this.adjustedTickDelay(30)) == 0) {
                for(int i = 1; i <= 2; ++i) {
                    BlockPosition blockPos = EntityBee.this.getChunkCoordinates().below(i);
                    IBlockData blockState = EntityBee.this.level.getType(blockPos);
                    Block block = blockState.getBlock();
                    boolean bl = false;
                    BlockStateInteger integerProperty = null;
                    if (blockState.is(TagsBlock.BEE_GROWABLES)) {
                        if (block instanceof BlockCrops) {
                            BlockCrops cropBlock = (BlockCrops)block;
                            if (!cropBlock.isRipe(blockState)) {
                                bl = true;
                                integerProperty = cropBlock.getAgeProperty();
                            }
                        } else if (block instanceof BlockStem) {
                            int j = blockState.get(BlockStem.AGE);
                            if (j < 7) {
                                bl = true;
                                integerProperty = BlockStem.AGE;
                            }
                        } else if (blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                            int k = blockState.get(BlockSweetBerryBush.AGE);
                            if (k < 3) {
                                bl = true;
                                integerProperty = BlockSweetBerryBush.AGE;
                            }
                        } else if (blockState.is(Blocks.CAVE_VINES) || blockState.is(Blocks.CAVE_VINES_PLANT)) {
                            ((IBlockFragilePlantElement)blockState.getBlock()).performBonemeal((WorldServer)EntityBee.this.level, EntityBee.this.random, blockPos, blockState);
                        }

                        if (bl) {
                            EntityBee.this.level.triggerEffect(2005, blockPos, 0);
                            EntityBee.this.level.setTypeUpdate(blockPos, blockState.set(integerProperty, Integer.valueOf(blockState.get(integerProperty) + 1)));
                            EntityBee.this.incrementNumCropsGrownSincePollination();
                        }
                    }
                }

            }
        }
    }

    class BeeHurtByOtherGoal extends PathfinderGoalHurtByTarget {
        BeeHurtByOtherGoal(EntityBee bee) {
            super(bee);
        }

        @Override
        public boolean canContinueToUse() {
            return EntityBee.this.isAngry() && super.canContinueToUse();
        }

        @Override
        protected void alertOther(EntityInsentient mob, EntityLiving target) {
            if (mob instanceof EntityBee && this.mob.hasLineOfSight(target)) {
                mob.setGoalTarget(target);
            }

        }
    }

    class BeeLocateHiveGoal extends EntityBee.BaseBeeGoal {
        @Override
        public boolean canBeeUse() {
            return EntityBee.this.remainingCooldownBeforeLocatingNewHive == 0 && !EntityBee.this.hasHivePos() && EntityBee.this.wantsToEnterHive();
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
            List<BlockPosition> list = this.findNearbyHivesWithSpace();
            if (!list.isEmpty()) {
                for(BlockPosition blockPos : list) {
                    if (!EntityBee.this.goToHiveGoal.isTargetBlacklisted(blockPos)) {
                        EntityBee.this.hivePos = blockPos;
                        return;
                    }
                }

                EntityBee.this.goToHiveGoal.clearBlacklist();
                EntityBee.this.hivePos = list.get(0);
            }
        }

        private List<BlockPosition> findNearbyHivesWithSpace() {
            BlockPosition blockPos = EntityBee.this.getChunkCoordinates();
            VillagePlace poiManager = ((WorldServer)EntityBee.this.level).getPoiManager();
            Stream<VillagePlaceRecord> stream = poiManager.getInRange((poiType) -> {
                return poiType == VillagePlaceType.BEEHIVE || poiType == VillagePlaceType.BEE_NEST;
            }, blockPos, 20, VillagePlace.Occupancy.ANY);
            return stream.map(VillagePlaceRecord::getPos).filter(EntityBee.this::doesHiveHaveSpace).sorted(Comparator.comparingDouble((blockPos2) -> {
                return blockPos2.distSqr(blockPos);
            })).collect(Collectors.toList());
        }
    }

    class BeeLookControl extends ControllerLook {
        BeeLookControl(EntityInsentient entity) {
            super(entity);
        }

        @Override
        public void tick() {
            if (!EntityBee.this.isAngry()) {
                super.tick();
            }
        }

        @Override
        protected boolean resetXRotOnTick() {
            return !EntityBee.this.beePollinateGoal.isPollinating();
        }
    }

    class BeePollinateGoal extends EntityBee.BaseBeeGoal {
        private static final int MIN_POLLINATION_TICKS = 400;
        private static final int MIN_FIND_FLOWER_RETRY_COOLDOWN = 20;
        private static final int MAX_FIND_FLOWER_RETRY_COOLDOWN = 60;
        private final Predicate<IBlockData> VALID_POLLINATION_BLOCKS = (state) -> {
            if (state.is(TagsBlock.FLOWERS)) {
                if (state.is(Blocks.SUNFLOWER)) {
                    return state.get(BlockTallPlant.HALF) == BlockPropertyDoubleBlockHalf.UPPER;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        };
        private static final double ARRIVAL_THRESHOLD = 0.1D;
        private static final int POSITION_CHANGE_CHANCE = 25;
        private static final float SPEED_MODIFIER = 0.35F;
        private static final float HOVER_HEIGHT_WITHIN_FLOWER = 0.6F;
        private static final float HOVER_POS_OFFSET = 0.33333334F;
        private int successfulPollinatingTicks;
        private int lastSoundPlayedTick;
        private boolean pollinating;
        @Nullable
        private Vec3D hoverPos;
        private int pollinatingTicks;
        private static final int MAX_POLLINATING_TICKS = 600;

        BeePollinateGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            if (EntityBee.this.remainingCooldownBeforeLocatingNewFlower > 0) {
                return false;
            } else if (EntityBee.this.hasNectar()) {
                return false;
            } else if (EntityBee.this.level.isRaining()) {
                return false;
            } else {
                Optional<BlockPosition> optional = this.findNearbyFlower();
                if (optional.isPresent()) {
                    EntityBee.this.savedFlowerPos = optional.get();
                    EntityBee.this.navigation.moveTo((double)EntityBee.this.savedFlowerPos.getX() + 0.5D, (double)EntityBee.this.savedFlowerPos.getY() + 0.5D, (double)EntityBee.this.savedFlowerPos.getZ() + 0.5D, (double)1.2F);
                    return true;
                } else {
                    EntityBee.this.remainingCooldownBeforeLocatingNewFlower = MathHelper.nextInt(EntityBee.this.random, 20, 60);
                    return false;
                }
            }
        }

        @Override
        public boolean canBeeContinueToUse() {
            if (!this.pollinating) {
                return false;
            } else if (!EntityBee.this.hasFlowerPos()) {
                return false;
            } else if (EntityBee.this.level.isRaining()) {
                return false;
            } else if (this.hasPollinatedLongEnough()) {
                return EntityBee.this.random.nextFloat() < 0.2F;
            } else if (EntityBee.this.tickCount % 20 == 0 && !EntityBee.this.isFlowerValid(EntityBee.this.savedFlowerPos)) {
                EntityBee.this.savedFlowerPos = null;
                return false;
            } else {
                return true;
            }
        }

        private boolean hasPollinatedLongEnough() {
            return this.successfulPollinatingTicks > 400;
        }

        boolean isPollinating() {
            return this.pollinating;
        }

        void stopPollinating() {
            this.pollinating = false;
        }

        @Override
        public void start() {
            this.successfulPollinatingTicks = 0;
            this.pollinatingTicks = 0;
            this.lastSoundPlayedTick = 0;
            this.pollinating = true;
            EntityBee.this.resetTicksWithoutNectarSinceExitingHive();
        }

        @Override
        public void stop() {
            if (this.hasPollinatedLongEnough()) {
                EntityBee.this.setHasNectar(true);
            }

            this.pollinating = false;
            EntityBee.this.navigation.stop();
            EntityBee.this.remainingCooldownBeforeLocatingNewFlower = 200;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.pollinatingTicks;
            if (this.pollinatingTicks > 600) {
                EntityBee.this.savedFlowerPos = null;
            } else {
                Vec3D vec3 = Vec3D.atBottomCenterOf(EntityBee.this.savedFlowerPos).add(0.0D, (double)0.6F, 0.0D);
                if (vec3.distanceTo(EntityBee.this.getPositionVector()) > 1.0D) {
                    this.hoverPos = vec3;
                    this.setWantedPos();
                } else {
                    if (this.hoverPos == null) {
                        this.hoverPos = vec3;
                    }

                    boolean bl = EntityBee.this.getPositionVector().distanceTo(this.hoverPos) <= 0.1D;
                    boolean bl2 = true;
                    if (!bl && this.pollinatingTicks > 600) {
                        EntityBee.this.savedFlowerPos = null;
                    } else {
                        if (bl) {
                            boolean bl3 = EntityBee.this.random.nextInt(25) == 0;
                            if (bl3) {
                                this.hoverPos = new Vec3D(vec3.getX() + (double)this.getOffset(), vec3.getY(), vec3.getZ() + (double)this.getOffset());
                                EntityBee.this.navigation.stop();
                            } else {
                                bl2 = false;
                            }

                            EntityBee.this.getControllerLook().setLookAt(vec3.getX(), vec3.getY(), vec3.getZ());
                        }

                        if (bl2) {
                            this.setWantedPos();
                        }

                        ++this.successfulPollinatingTicks;
                        if (EntityBee.this.random.nextFloat() < 0.05F && this.successfulPollinatingTicks > this.lastSoundPlayedTick + 60) {
                            this.lastSoundPlayedTick = this.successfulPollinatingTicks;
                            EntityBee.this.playSound(SoundEffects.BEE_POLLINATE, 1.0F, 1.0F);
                        }

                    }
                }
            }
        }

        private void setWantedPos() {
            EntityBee.this.getControllerMove().setWantedPosition(this.hoverPos.getX(), this.hoverPos.getY(), this.hoverPos.getZ(), (double)0.35F);
        }

        private float getOffset() {
            return (EntityBee.this.random.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
        }

        private Optional<BlockPosition> findNearbyFlower() {
            return this.findNearestBlock(this.VALID_POLLINATION_BLOCKS, 5.0D);
        }

        private Optional<BlockPosition> findNearestBlock(Predicate<IBlockData> predicate, double searchDistance) {
            BlockPosition blockPos = EntityBee.this.getChunkCoordinates();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int i = 0; (double)i <= searchDistance; i = i > 0 ? -i : 1 - i) {
                for(int j = 0; (double)j < searchDistance; ++j) {
                    for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            mutableBlockPos.setWithOffset(blockPos, k, i - 1, l);
                            if (blockPos.closerThan(mutableBlockPos, searchDistance) && predicate.test(EntityBee.this.level.getType(mutableBlockPos))) {
                                return Optional.of(mutableBlockPos);
                            }
                        }
                    }
                }
            }

            return Optional.empty();
        }
    }

    class BeeWanderGoal extends PathfinderGoal {
        private static final int WANDER_THRESHOLD = 22;

        BeeWanderGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            return EntityBee.this.navigation.isDone() && EntityBee.this.random.nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return EntityBee.this.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3D vec3 = this.findPos();
            if (vec3 != null) {
                EntityBee.this.navigation.moveTo(EntityBee.this.navigation.createPath(new BlockPosition(vec3), 1), 1.0D);
            }

        }

        @Nullable
        private Vec3D findPos() {
            Vec3D vec32;
            if (EntityBee.this.isHiveValid() && !EntityBee.this.closerThan(EntityBee.this.hivePos, 22)) {
                Vec3D vec3 = Vec3D.atCenterOf(EntityBee.this.hivePos);
                vec32 = vec3.subtract(EntityBee.this.getPositionVector()).normalize();
            } else {
                vec32 = EntityBee.this.getViewVector(0.0F);
            }

            int i = 8;
            Vec3D vec34 = HoverRandomPos.getPos(EntityBee.this, 8, 7, vec32.x, vec32.z, ((float)Math.PI / 2F), 3, 1);
            return vec34 != null ? vec34 : AirAndWaterRandomPos.getPos(EntityBee.this, 8, 4, -2, vec32.x, vec32.z, (double)((float)Math.PI / 2F));
        }
    }
}
