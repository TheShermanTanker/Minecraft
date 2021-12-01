package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.LivingEntity$Fallsounds;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.entity.animal.EntityPig;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.entity.vehicle.EntityMinecartAbstract;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerPlayer;
import net.minecraft.world.inventory.InventoryEnderChest;
import net.minecraft.world.item.ItemAxe;
import net.minecraft.world.item.ItemCooldown;
import net.minecraft.world.item.ItemElytra;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSword;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.BlockRespawnAnchor;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;

public abstract class EntityHuman extends EntityLiving {
    public static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";
    public static final int MAX_NAME_LENGTH = 16;
    public static final int MAX_HEALTH = 20;
    public static final int SLEEP_DURATION = 100;
    public static final int WAKE_UP_DURATION = 10;
    public static final int ENDER_SLOT_OFFSET = 200;
    public static final float CROUCH_BB_HEIGHT = 1.5F;
    public static final float SWIMMING_BB_WIDTH = 0.6F;
    public static final float SWIMMING_BB_HEIGHT = 0.6F;
    public static final float DEFAULT_EYE_HEIGHT = 1.62F;
    public static final EntitySize STANDING_DIMENSIONS = EntitySize.scalable(0.6F, 1.8F);
    private static final Map<EntityPose, EntitySize> POSES = ImmutableMap.<EntityPose, EntitySize>builder().put(EntityPose.STANDING, STANDING_DIMENSIONS).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS).put(EntityPose.FALL_FLYING, EntitySize.scalable(0.6F, 0.6F)).put(EntityPose.SWIMMING, EntitySize.scalable(0.6F, 0.6F)).put(EntityPose.SPIN_ATTACK, EntitySize.scalable(0.6F, 0.6F)).put(EntityPose.CROUCHING, EntitySize.scalable(0.6F, 1.5F)).put(EntityPose.DYING, EntitySize.fixed(0.2F, 0.2F)).build();
    private static final int FLY_ACHIEVEMENT_SPEED = 25;
    private static final DataWatcherObject<Float> DATA_PLAYER_ABSORPTION_ID = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_SCORE_ID = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.INT);
    public static final DataWatcherObject<Byte> DATA_PLAYER_MODE_CUSTOMISATION = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.BYTE);
    protected static final DataWatcherObject<Byte> DATA_PLAYER_MAIN_HAND = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.BYTE);
    protected static final DataWatcherObject<NBTTagCompound> DATA_SHOULDER_LEFT = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.COMPOUND_TAG);
    protected static final DataWatcherObject<NBTTagCompound> DATA_SHOULDER_RIGHT = DataWatcher.defineId(EntityHuman.class, DataWatcherRegistry.COMPOUND_TAG);
    private long timeEntitySatOnShoulder;
    private final PlayerInventory inventory = new PlayerInventory(this);
    protected InventoryEnderChest enderChestInventory = new InventoryEnderChest();
    public final ContainerPlayer inventoryMenu;
    public Container containerMenu;
    protected FoodMetaData foodData = new FoodMetaData();
    protected int jumpTriggerTime;
    public float oBob;
    public float bob;
    public int takeXpDelay;
    public double xCloakO;
    public double yCloakO;
    public double zCloakO;
    public double xCloak;
    public double yCloak;
    public double zCloak;
    public int sleepCounter;
    protected boolean wasUnderwater;
    private final PlayerAbilities abilities = new PlayerAbilities();
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    protected int enchantmentSeed;
    protected final float defaultFlySpeed = 0.02F;
    private int lastLevelUpTime;
    public GameProfile gameProfile;
    private boolean reducedDebugInfo;
    private ItemStack lastItemInMainHand = ItemStack.EMPTY;
    private final ItemCooldown cooldowns = this.createItemCooldowns();
    @Nullable
    public EntityFishingHook fishing;

    public EntityHuman(World world, BlockPosition pos, float yaw, GameProfile profile) {
        super(EntityTypes.PLAYER, world);
        this.setUUID(createPlayerUUID(profile));
        this.gameProfile = profile;
        this.inventoryMenu = new ContainerPlayer(this.inventory, !world.isClientSide, this);
        this.containerMenu = this.inventoryMenu;
        this.setPositionRotation((double)pos.getX() + 0.5D, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D, yaw, 0.0F);
        this.rotOffs = 180.0F;
    }

    public boolean blockActionRestricted(World world, BlockPosition pos, EnumGamemode gameMode) {
        if (!gameMode.isBlockPlacingRestricted()) {
            return false;
        } else if (gameMode == EnumGamemode.SPECTATOR) {
            return true;
        } else if (this.mayBuild()) {
            return false;
        } else {
            ItemStack itemStack = this.getItemInMainHand();
            return itemStack.isEmpty() || !itemStack.hasAdventureModeBreakTagForBlock(world.getTagManager(), new ShapeDetectorBlock(world, pos, false));
        }
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityLiving.createLivingAttributes().add(GenericAttributes.ATTACK_DAMAGE, 1.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.1F).add(GenericAttributes.ATTACK_SPEED).add(GenericAttributes.LUCK);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_PLAYER_ABSORPTION_ID, 0.0F);
        this.entityData.register(DATA_SCORE_ID, 0);
        this.entityData.register(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
        this.entityData.register(DATA_PLAYER_MAIN_HAND, (byte)1);
        this.entityData.register(DATA_SHOULDER_LEFT, new NBTTagCompound());
        this.entityData.register(DATA_SHOULDER_RIGHT, new NBTTagCompound());
    }

    @Override
    public void tick() {
        this.noPhysics = this.isSpectator();
        if (this.isSpectator()) {
            this.onGround = false;
        }

        if (this.takeXpDelay > 0) {
            --this.takeXpDelay;
        }

        if (this.isSleeping()) {
            ++this.sleepCounter;
            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level.isClientSide && this.level.isDay()) {
                this.wakeup(false, true);
            }
        } else if (this.sleepCounter > 0) {
            ++this.sleepCounter;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        super.tick();
        if (!this.level.isClientSide && this.containerMenu != null && !this.containerMenu.canUse(this)) {
            this.closeInventory();
            this.containerMenu = this.inventoryMenu;
        }

        this.moveCloak();
        if (!this.level.isClientSide) {
            this.foodData.tick(this);
            this.awardStat(StatisticList.PLAY_TIME);
            this.awardStat(StatisticList.TOTAL_WORLD_TIME);
            if (this.isAlive()) {
                this.awardStat(StatisticList.TIME_SINCE_DEATH);
            }

            if (this.isDiscrete()) {
                this.awardStat(StatisticList.CROUCH_TIME);
            }

            if (!this.isSleeping()) {
                this.awardStat(StatisticList.TIME_SINCE_REST);
            }
        }

        int i = 29999999;
        double d = MathHelper.clamp(this.locX(), -2.9999999E7D, 2.9999999E7D);
        double e = MathHelper.clamp(this.locZ(), -2.9999999E7D, 2.9999999E7D);
        if (d != this.locX() || e != this.locZ()) {
            this.setPosition(d, this.locY(), e);
        }

        ++this.attackStrengthTicker;
        ItemStack itemStack = this.getItemInMainHand();
        if (!ItemStack.matches(this.lastItemInMainHand, itemStack)) {
            if (!ItemStack.isSameIgnoreDurability(this.lastItemInMainHand, itemStack)) {
                this.resetAttackCooldown();
            }

            this.lastItemInMainHand = itemStack.cloneItemStack();
        }

        this.turtleHelmetTick();
        this.cooldowns.tick();
        this.updatePlayerPose();
    }

    public boolean isSecondaryUseActive() {
        return this.isSneaking();
    }

    protected boolean wantsToStopRiding() {
        return this.isSneaking();
    }

    protected boolean isStayingOnGroundSurface() {
        return this.isSneaking();
    }

    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(TagsFluid.WATER);
        return this.wasUnderwater;
    }

    private void turtleHelmetTick() {
        ItemStack itemStack = this.getEquipment(EnumItemSlot.HEAD);
        if (itemStack.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(TagsFluid.WATER)) {
            this.addEffect(new MobEffect(MobEffectList.WATER_BREATHING, 200, 0, false, false, true));
        }

    }

    protected ItemCooldown createItemCooldowns() {
        return new ItemCooldown();
    }

    private void moveCloak() {
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
        double d = this.locX() - this.xCloak;
        double e = this.locY() - this.yCloak;
        double f = this.locZ() - this.zCloak;
        double g = 10.0D;
        if (d > 10.0D) {
            this.xCloak = this.locX();
            this.xCloakO = this.xCloak;
        }

        if (f > 10.0D) {
            this.zCloak = this.locZ();
            this.zCloakO = this.zCloak;
        }

        if (e > 10.0D) {
            this.yCloak = this.locY();
            this.yCloakO = this.yCloak;
        }

        if (d < -10.0D) {
            this.xCloak = this.locX();
            this.xCloakO = this.xCloak;
        }

        if (f < -10.0D) {
            this.zCloak = this.locZ();
            this.zCloakO = this.zCloak;
        }

        if (e < -10.0D) {
            this.yCloak = this.locY();
            this.yCloakO = this.yCloak;
        }

        this.xCloak += d * 0.25D;
        this.zCloak += f * 0.25D;
        this.yCloak += e * 0.25D;
    }

    protected void updatePlayerPose() {
        if (this.canEnterPose(EntityPose.SWIMMING)) {
            EntityPose pose;
            if (this.isGliding()) {
                pose = EntityPose.FALL_FLYING;
            } else if (this.isSleeping()) {
                pose = EntityPose.SLEEPING;
            } else if (this.isSwimming()) {
                pose = EntityPose.SWIMMING;
            } else if (this.isRiptiding()) {
                pose = EntityPose.SPIN_ATTACK;
            } else if (this.isSneaking() && !this.abilities.flying) {
                pose = EntityPose.CROUCHING;
            } else {
                pose = EntityPose.STANDING;
            }

            EntityPose pose8;
            if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
                if (this.canEnterPose(EntityPose.CROUCHING)) {
                    pose8 = EntityPose.CROUCHING;
                } else {
                    pose8 = EntityPose.SWIMMING;
                }
            } else {
                pose8 = pose;
            }

            this.setPose(pose8);
        }
    }

    @Override
    public int getPortalWaitTime() {
        return this.abilities.invulnerable ? 1 : 80;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.PLAYER_SWIM;
    }

    @Override
    protected SoundEffect getSoundSplash() {
        return SoundEffects.PLAYER_SPLASH;
    }

    @Override
    protected SoundEffect getSoundSplashHighSpeed() {
        return SoundEffects.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    public int getDefaultPortalCooldown() {
        return 10;
    }

    @Override
    public void playSound(SoundEffect sound, float volume, float pitch) {
        this.level.playSound(this, this.locX(), this.locY(), this.locZ(), sound, this.getSoundCategory(), volume, pitch);
    }

    public void playNotifySound(SoundEffect event, EnumSoundCategory category, float volume, float pitch) {
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.PLAYERS;
    }

    @Override
    public int getMaxFireTicks() {
        return 20;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 9) {
            this.completeUsingItem();
        } else if (status == 23) {
            this.reducedDebugInfo = false;
        } else if (status == 22) {
            this.reducedDebugInfo = true;
        } else if (status == 43) {
            this.addParticlesAroundSelf(Particles.CLOUD);
        } else {
            super.handleEntityEvent(status);
        }

    }

    private void addParticlesAroundSelf(ParticleParam parameters) {
        for(int i = 0; i < 5; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(parameters, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    public void closeInventory() {
        this.containerMenu = this.inventoryMenu;
    }

    @Override
    public void passengerTick() {
        if (!this.level.isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            this.setSneaking(false);
        } else {
            double d = this.locX();
            double e = this.locY();
            double f = this.locZ();
            super.passengerTick();
            this.oBob = this.bob;
            this.bob = 0.0F;
            this.checkRidingStatistics(this.locX() - d, this.locY() - e, this.locZ() - f);
        }
    }

    @Override
    protected void doTick() {
        super.doTick();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
    }

    @Override
    public void movementTick() {
        if (this.jumpTriggerTime > 0) {
            --this.jumpTriggerTime;
        }

        if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL && this.level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }

            if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
                this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
            }
        }

        this.inventory.tick();
        this.oBob = this.bob;
        super.movementTick();
        this.flyingSpeed = 0.02F;
        if (this.isSprinting()) {
            this.flyingSpeed = (float)((double)this.flyingSpeed + 0.005999999865889549D);
        }

        this.setSpeed((float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
        float g;
        if (this.onGround && !this.isDeadOrDying() && !this.isSwimming()) {
            g = Math.min(0.1F, (float)this.getMot().horizontalDistance());
        } else {
            g = 0.0F;
        }

        this.bob += (g - this.bob) * 0.4F;
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AxisAlignedBB aABB;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                aABB = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).grow(1.0D, 0.0D, 1.0D);
            } else {
                aABB = this.getBoundingBox().grow(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.level.getEntities(this, aABB);
            List<Entity> list2 = Lists.newArrayList();

            for(int i = 0; i < list.size(); ++i) {
                Entity entity = list.get(i);
                if (entity.getEntityType() == EntityTypes.EXPERIENCE_ORB) {
                    list2.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list2.isEmpty()) {
                this.touch(SystemUtils.getRandom(list2, this.random));
            }
        }

        this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
        this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
        if (!this.level.isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
            this.releaseShoulderEntities();
        }

    }

    private void playShoulderEntityAmbientSound(@Nullable NBTTagCompound entityNbt) {
        if (entityNbt != null && (!entityNbt.hasKey("Silent") || !entityNbt.getBoolean("Silent")) && this.level.random.nextInt(200) == 0) {
            String string = entityNbt.getString("id");
            EntityTypes.byString(string).filter((entityType) -> {
                return entityType == EntityTypes.PARROT;
            }).ifPresent((entityType) -> {
                if (!EntityParrot.imitateNearbyMobs(this.level, this)) {
                    this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), EntityParrot.getAmbient(this.level, this.level.random), this.getSoundCategory(), 1.0F, EntityParrot.getPitch(this.level.random));
                }

            });
        }

    }

    private void touch(Entity entity) {
        entity.pickup(this);
    }

    public int getScore() {
        return this.entityData.get(DATA_SCORE_ID);
    }

    public void setScore(int score) {
        this.entityData.set(DATA_SCORE_ID, score);
    }

    public void addScore(int score) {
        int i = this.getScore();
        this.entityData.set(DATA_SCORE_ID, i + score);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.reapplyPosition();
        if (!this.isSpectator()) {
            this.dropAllDeathLoot(source);
        }

        if (source != null) {
            this.setMot((double)(-MathHelper.cos((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F), (double)0.1F, (double)(-MathHelper.sin((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F));
        } else {
            this.setMot(0.0D, 0.1D, 0.0D);
        }

        this.awardStat(StatisticList.DEATHS);
        this.resetStat(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_DEATH));
        this.resetStat(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_REST));
        this.extinguish();
        this.setSharedFlagOnFire(false);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.removeCursedItems();
            this.inventory.dropContents();
        }

    }

    protected void removeCursedItems() {
        for(int i = 0; i < this.inventory.getSize(); ++i) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty() && EnchantmentManager.shouldNotDrop(itemStack)) {
                this.inventory.splitWithoutUpdate(i);
            }
        }

    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        if (source == DamageSource.ON_FIRE) {
            return SoundEffects.PLAYER_HURT_ON_FIRE;
        } else if (source == DamageSource.DROWN) {
            return SoundEffects.PLAYER_HURT_DROWN;
        } else if (source == DamageSource.SWEET_BERRY_BUSH) {
            return SoundEffects.PLAYER_HURT_SWEET_BERRY_BUSH;
        } else {
            return source == DamageSource.FREEZE ? SoundEffects.PLAYER_HURT_FREEZE : SoundEffects.PLAYER_HURT;
        }
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PLAYER_DEATH;
    }

    @Nullable
    public EntityItem drop(ItemStack stack, boolean retainOwnership) {
        return this.drop(stack, false, retainOwnership);
    }

    @Nullable
    public EntityItem drop(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (stack.isEmpty()) {
            return null;
        } else {
            if (this.level.isClientSide) {
                this.swingHand(EnumHand.MAIN_HAND);
            }

            double d = this.getHeadY() - (double)0.3F;
            EntityItem itemEntity = new EntityItem(this.level, this.locX(), d, this.locZ(), stack);
            itemEntity.setPickupDelay(40);
            if (retainOwnership) {
                itemEntity.setThrower(this.getUniqueID());
            }

            if (throwRandomly) {
                float f = this.random.nextFloat() * 0.5F;
                float g = this.random.nextFloat() * ((float)Math.PI * 2F);
                itemEntity.setMot((double)(-MathHelper.sin(g) * f), (double)0.2F, (double)(MathHelper.cos(g) * f));
            } else {
                float h = 0.3F;
                float i = MathHelper.sin(this.getXRot() * ((float)Math.PI / 180F));
                float j = MathHelper.cos(this.getXRot() * ((float)Math.PI / 180F));
                float k = MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F));
                float l = MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F));
                float m = this.random.nextFloat() * ((float)Math.PI * 2F);
                float n = 0.02F * this.random.nextFloat();
                itemEntity.setMot((double)(-k * j * 0.3F) + Math.cos((double)m) * (double)n, (double)(-i * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(l * j * 0.3F) + Math.sin((double)m) * (double)n);
            }

            return itemEntity;
        }
    }

    public float getDestroySpeed(IBlockData block) {
        float f = this.inventory.getDestroySpeed(block);
        if (f > 1.0F) {
            int i = EnchantmentManager.getDigSpeedEnchantmentLevel(this);
            ItemStack itemStack = this.getItemInMainHand();
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float)(i * i + 1);
            }
        }

        if (MobEffectUtil.hasDigSpeed(this)) {
            f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffectList.DIG_SLOWDOWN)) {
            float g;
            switch(this.getEffect(MobEffectList.DIG_SLOWDOWN).getAmplifier()) {
            case 0:
                g = 0.3F;
                break;
            case 1:
                g = 0.09F;
                break;
            case 2:
                g = 0.0027F;
                break;
            case 3:
            default:
                g = 8.1E-4F;
            }

            f *= g;
        }

        if (this.isEyeInFluid(TagsFluid.WATER) && !EnchantmentManager.hasAquaAffinity(this)) {
            f /= 5.0F;
        }

        if (!this.onGround) {
            f /= 5.0F;
        }

        return f;
    }

    public boolean hasBlock(IBlockData state) {
        return !state.isRequiresSpecialTool() || this.inventory.getItemInHand().canDestroySpecialBlock(state);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setUUID(createPlayerUUID(this.gameProfile));
        NBTTagList listTag = nbt.getList("Inventory", 10);
        this.inventory.load(listTag);
        this.inventory.selected = nbt.getInt("SelectedItemSlot");
        this.sleepCounter = nbt.getShort("SleepTimer");
        this.experienceProgress = nbt.getFloat("XpP");
        this.experienceLevel = nbt.getInt("XpLevel");
        this.totalExperience = nbt.getInt("XpTotal");
        this.enchantmentSeed = nbt.getInt("XpSeed");
        if (this.enchantmentSeed == 0) {
            this.enchantmentSeed = this.random.nextInt();
        }

        this.setScore(nbt.getInt("Score"));
        this.foodData.readAdditionalSaveData(nbt);
        this.abilities.loadSaveData(nbt);
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((double)this.abilities.getWalkingSpeed());
        if (nbt.hasKeyOfType("EnderItems", 9)) {
            this.enderChestInventory.fromTag(nbt.getList("EnderItems", 10));
        }

        if (nbt.hasKeyOfType("ShoulderEntityLeft", 10)) {
            this.setShoulderEntityLeft(nbt.getCompound("ShoulderEntityLeft"));
        }

        if (nbt.hasKeyOfType("ShoulderEntityRight", 10)) {
            this.setShoulderEntityRight(nbt.getCompound("ShoulderEntityRight"));
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        nbt.set("Inventory", this.inventory.save(new NBTTagList()));
        nbt.setInt("SelectedItemSlot", this.inventory.selected);
        nbt.setShort("SleepTimer", (short)this.sleepCounter);
        nbt.setFloat("XpP", this.experienceProgress);
        nbt.setInt("XpLevel", this.experienceLevel);
        nbt.setInt("XpTotal", this.totalExperience);
        nbt.setInt("XpSeed", this.enchantmentSeed);
        nbt.setInt("Score", this.getScore());
        this.foodData.addAdditionalSaveData(nbt);
        this.abilities.addSaveData(nbt);
        nbt.set("EnderItems", this.enderChestInventory.createTag());
        if (!this.getShoulderEntityLeft().isEmpty()) {
            nbt.set("ShoulderEntityLeft", this.getShoulderEntityLeft());
        }

        if (!this.getShoulderEntityRight().isEmpty()) {
            nbt.set("ShoulderEntityRight", this.getShoulderEntityRight());
        }

    }

    @Override
    public boolean isInvulnerable(DamageSource damageSource) {
        if (super.isInvulnerable(damageSource)) {
            return true;
        } else if (damageSource == DamageSource.DROWN) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
        } else if (damageSource.isFall()) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
        } else if (damageSource.isFire()) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
        } else if (damageSource == DamageSource.FREEZE) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE);
        } else {
            return false;
        }
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (this.abilities.invulnerable && !source.ignoresInvulnerability()) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                this.releaseShoulderEntities();
                if (source.scalesWithDifficulty()) {
                    if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL) {
                        amount = 0.0F;
                    }

                    if (this.level.getDifficulty() == EnumDifficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.level.getDifficulty() == EnumDifficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                return amount == 0.0F ? false : super.damageEntity(source, amount);
            }
        }
    }

    @Override
    protected void shieldBlock(EntityLiving attacker) {
        super.shieldBlock(attacker);
        if (attacker.getItemInMainHand().getItem() instanceof ItemAxe) {
            this.disableShield(true);
        }

    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
    }

    public boolean canHarmPlayer(EntityHuman player) {
        ScoreboardTeamBase team = this.getScoreboardTeam();
        ScoreboardTeamBase team2 = player.getScoreboardTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlly(team2) ? true : team.allowFriendlyFire();
        }
    }

    @Override
    protected void damageArmor(DamageSource source, float amount) {
        this.inventory.hurtArmor(source, amount, PlayerInventory.ALL_ARMOR_SLOTS);
    }

    @Override
    protected void damageHelmet(DamageSource source, float amount) {
        this.inventory.hurtArmor(source, amount, PlayerInventory.HELMET_SLOT_ONLY);
    }

    @Override
    protected void damageShield(float amount) {
        if (this.useItem.is(Items.SHIELD)) {
            if (!this.level.isClientSide) {
                this.awardStat(StatisticList.ITEM_USED.get(this.useItem.getItem()));
            }

            if (amount >= 3.0F) {
                int i = 1 + MathHelper.floor(amount);
                EnumHand interactionHand = this.getRaisedHand();
                this.useItem.damage(i, this, (player) -> {
                    player.broadcastItemBreak(interactionHand);
                });
                if (this.useItem.isEmpty()) {
                    if (interactionHand == EnumHand.MAIN_HAND) {
                        this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setSlot(EnumItemSlot.OFFHAND, ItemStack.EMPTY);
                    }

                    this.useItem = ItemStack.EMPTY;
                    this.playSound(SoundEffects.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
                }
            }

        }
    }

    @Override
    protected void damageEntity0(DamageSource source, float amount) {
        if (!this.isInvulnerable(source)) {
            amount = this.applyArmorModifier(source, amount);
            amount = this.applyMagicModifier(source, amount);
            float var8 = Math.max(amount - this.getAbsorptionHearts(), 0.0F);
            this.setAbsorptionHearts(this.getAbsorptionHearts() - (amount - var8));
            float g = amount - var8;
            if (g > 0.0F && g < 3.4028235E37F) {
                this.awardStat(StatisticList.DAMAGE_ABSORBED, Math.round(g * 10.0F));
            }

            if (var8 != 0.0F) {
                this.applyExhaustion(source.getExhaustionCost());
                float h = this.getHealth();
                this.setHealth(this.getHealth() - var8);
                this.getCombatTracker().trackDamage(source, h, var8);
                if (var8 < 3.4028235E37F) {
                    this.awardStat(StatisticList.DAMAGE_TAKEN, Math.round(var8 * 10.0F));
                }

            }
        }
    }

    @Override
    protected boolean onSoulSpeedBlock() {
        return !this.abilities.flying && super.onSoulSpeedBlock();
    }

    public void openSign(TileEntitySign sign) {
    }

    public void openMinecartCommandBlock(CommandBlockListenerAbstract commandBlockExecutor) {
    }

    public void openCommandBlock(TileEntityCommand commandBlock) {
    }

    public void openStructureBlock(TileEntityStructure structureBlock) {
    }

    public void openJigsawBlock(TileEntityJigsaw jigsaw) {
    }

    public void openHorseInventory(EntityHorseAbstract horse, IInventory inventory) {
    }

    public OptionalInt openContainer(@Nullable ITileInventory factory) {
        return OptionalInt.empty();
    }

    public void openTrade(int syncId, MerchantRecipeList offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
    }

    public void openBook(ItemStack book, EnumHand hand) {
    }

    public EnumInteractionResult interactOn(Entity entity, EnumHand hand) {
        if (this.isSpectator()) {
            if (entity instanceof ITileInventory) {
                this.openContainer((ITileInventory)entity);
            }

            return EnumInteractionResult.PASS;
        } else {
            ItemStack itemStack = this.getItemInHand(hand);
            ItemStack itemStack2 = itemStack.cloneItemStack();
            EnumInteractionResult interactionResult = entity.interact(this, hand);
            if (interactionResult.consumesAction()) {
                if (this.abilities.instabuild && itemStack == this.getItemInHand(hand) && itemStack.getCount() < itemStack2.getCount()) {
                    itemStack.setCount(itemStack2.getCount());
                }

                return interactionResult;
            } else {
                if (!itemStack.isEmpty() && entity instanceof EntityLiving) {
                    if (this.abilities.instabuild) {
                        itemStack = itemStack2;
                    }

                    EnumInteractionResult interactionResult2 = itemStack.interactLivingEntity(this, (EntityLiving)entity, hand);
                    if (interactionResult2.consumesAction()) {
                        if (itemStack.isEmpty() && !this.abilities.instabuild) {
                            this.setItemInHand(hand, ItemStack.EMPTY);
                        }

                        return interactionResult2;
                    }
                }

                return EnumInteractionResult.PASS;
            }
        }
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void removeVehicle() {
        super.removeVehicle();
        this.boardingCooldown = 0;
    }

    @Override
    protected boolean isFrozen() {
        return super.isFrozen() || this.isSleeping();
    }

    @Override
    public boolean isAffectedByFluids() {
        return !this.abilities.flying;
    }

    @Override
    protected Vec3D maybeBackOffFromEdge(Vec3D movement, EnumMoveType type) {
        if (!this.abilities.flying && (type == EnumMoveType.SELF || type == EnumMoveType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
            double d = movement.x;
            double e = movement.z;
            double f = 0.05D;

            while(d != 0.0D && this.level.getCubes(this, this.getBoundingBox().move(d, (double)(-this.maxUpStep), 0.0D))) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }
            }

            while(e != 0.0D && this.level.getCubes(this, this.getBoundingBox().move(0.0D, (double)(-this.maxUpStep), e))) {
                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            while(d != 0.0D && e != 0.0D && this.level.getCubes(this, this.getBoundingBox().move(d, (double)(-this.maxUpStep), e))) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }

                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            movement = new Vec3D(d, movement.y, e);
        }

        return movement;
    }

    private boolean isAboveGround() {
        return this.onGround || this.fallDistance < this.maxUpStep && !this.level.getCubes(this, this.getBoundingBox().move(0.0D, (double)(this.fallDistance - this.maxUpStep), 0.0D));
    }

    public void attack(Entity target) {
        if (target.isAttackable()) {
            if (!target.skipAttackInteraction(this)) {
                float f = (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
                float g;
                if (target instanceof EntityLiving) {
                    g = EnchantmentManager.getDamageBonus(this.getItemInMainHand(), ((EntityLiving)target).getMonsterType());
                } else {
                    g = EnchantmentManager.getDamageBonus(this.getItemInMainHand(), EnumMonsterType.UNDEFINED);
                }

                float i = this.getAttackCooldown(0.5F);
                f = f * (0.2F + i * i * 0.8F);
                g = g * i;
                this.resetAttackCooldown();
                if (f > 0.0F || g > 0.0F) {
                    boolean bl = i > 0.9F;
                    boolean bl2 = false;
                    int j = 0;
                    j = j + EnchantmentManager.getKnockbackBonus(this);
                    if (this.isSprinting() && bl) {
                        this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_KNOCKBACK, this.getSoundCategory(), 1.0F, 1.0F);
                        ++j;
                        bl2 = true;
                    }

                    boolean bl3 = bl && this.fallDistance > 0.0F && !this.onGround && !this.isCurrentlyClimbing() && !this.isInWater() && !this.hasEffect(MobEffectList.BLINDNESS) && !this.isPassenger() && target instanceof EntityLiving;
                    bl3 = bl3 && !this.isSprinting();
                    if (bl3) {
                        f *= 1.5F;
                    }

                    f = f + g;
                    boolean bl4 = false;
                    double d = (double)(this.walkDist - this.walkDistO);
                    if (bl && !bl3 && !bl2 && this.onGround && d < (double)this.getSpeed()) {
                        ItemStack itemStack = this.getItemInHand(EnumHand.MAIN_HAND);
                        if (itemStack.getItem() instanceof ItemSword) {
                            bl4 = true;
                        }
                    }

                    float k = 0.0F;
                    boolean bl5 = false;
                    int l = EnchantmentManager.getFireAspectEnchantmentLevel(this);
                    if (target instanceof EntityLiving) {
                        k = ((EntityLiving)target).getHealth();
                        if (l > 0 && !target.isBurning()) {
                            bl5 = true;
                            target.setOnFire(1);
                        }
                    }

                    Vec3D vec3 = target.getMot();
                    boolean bl6 = target.damageEntity(DamageSource.playerAttack(this), f);
                    if (bl6) {
                        if (j > 0) {
                            if (target instanceof EntityLiving) {
                                ((EntityLiving)target).knockback((double)((float)j * 0.5F), (double)MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F))));
                            } else {
                                target.push((double)(-MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)) * (float)j * 0.5F), 0.1D, (double)(MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F)) * (float)j * 0.5F));
                            }

                            this.setMot(this.getMot().multiply(0.6D, 1.0D, 0.6D));
                            this.setSprinting(false);
                        }

                        if (bl4) {
                            float m = 1.0F + EnchantmentManager.getSweepingDamageRatio(this) * f;

                            for(EntityLiving livingEntity : this.level.getEntitiesOfClass(EntityLiving.class, target.getBoundingBox().grow(1.0D, 0.25D, 1.0D))) {
                                if (livingEntity != this && livingEntity != target && !this.isAlliedTo(livingEntity) && (!(livingEntity instanceof EntityArmorStand) || !((EntityArmorStand)livingEntity).isMarker()) && this.distanceToSqr(livingEntity) < 9.0D) {
                                    livingEntity.knockback((double)0.4F, (double)MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F))));
                                    livingEntity.damageEntity(DamageSource.playerAttack(this), m);
                                }
                            }

                            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_SWEEP, this.getSoundCategory(), 1.0F, 1.0F);
                            this.sweepAttack();
                        }

                        if (target instanceof EntityPlayer && target.hurtMarked) {
                            ((EntityPlayer)target).connection.sendPacket(new PacketPlayOutEntityVelocity(target));
                            target.hurtMarked = false;
                            target.setMot(vec3);
                        }

                        if (bl3) {
                            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_CRIT, this.getSoundCategory(), 1.0F, 1.0F);
                            this.crit(target);
                        }

                        if (!bl3 && !bl4) {
                            if (bl) {
                                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_STRONG, this.getSoundCategory(), 1.0F, 1.0F);
                            } else {
                                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_WEAK, this.getSoundCategory(), 1.0F, 1.0F);
                            }
                        }

                        if (g > 0.0F) {
                            this.magicCrit(target);
                        }

                        this.setLastHurtMob(target);
                        if (target instanceof EntityLiving) {
                            EnchantmentManager.doPostHurtEffects((EntityLiving)target, this);
                        }

                        EnchantmentManager.doPostDamageEffects(this, target);
                        ItemStack itemStack2 = this.getItemInMainHand();
                        Entity entity = target;
                        if (target instanceof EntityComplexPart) {
                            entity = ((EntityComplexPart)target).parentMob;
                        }

                        if (!this.level.isClientSide && !itemStack2.isEmpty() && entity instanceof EntityLiving) {
                            itemStack2.hurtEnemy((EntityLiving)entity, this);
                            if (itemStack2.isEmpty()) {
                                this.setItemInHand(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                            }
                        }

                        if (target instanceof EntityLiving) {
                            float n = k - ((EntityLiving)target).getHealth();
                            this.awardStat(StatisticList.DAMAGE_DEALT, Math.round(n * 10.0F));
                            if (l > 0) {
                                target.setOnFire(l * 4);
                            }

                            if (this.level instanceof WorldServer && n > 2.0F) {
                                int o = (int)((double)n * 0.5D);
                                ((WorldServer)this.level).sendParticles(Particles.DAMAGE_INDICATOR, target.locX(), target.getY(0.5D), target.locZ(), o, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        this.applyExhaustion(0.1F);
                    } else {
                        this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0F, 1.0F);
                        if (bl5) {
                            target.extinguish();
                        }
                    }
                }

            }
        }
    }

    @Override
    protected void doAutoAttackOnTouch(EntityLiving target) {
        this.attack(target);
    }

    public void disableShield(boolean sprinting) {
        float f = 0.25F + (float)EnchantmentManager.getDigSpeedEnchantmentLevel(this) * 0.05F;
        if (sprinting) {
            f += 0.75F;
        }

        if (this.random.nextFloat() < f) {
            this.getCooldownTracker().setCooldown(Items.SHIELD, 100);
            this.clearActiveItem();
            this.level.broadcastEntityEffect(this, (byte)30);
        }

    }

    public void crit(Entity target) {
    }

    public void magicCrit(Entity target) {
    }

    public void sweepAttack() {
        double d = (double)(-MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)));
        double e = (double)MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F));
        if (this.level instanceof WorldServer) {
            ((WorldServer)this.level).sendParticles(Particles.SWEEP_ATTACK, this.locX() + d, this.getY(0.5D), this.locZ() + e, 0, d, 0.0D, e, 0.0D);
        }

    }

    public void respawn() {
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.inventoryMenu.removed(this);
        if (this.containerMenu != null) {
            this.containerMenu.removed(this);
        }

    }

    public boolean isLocalPlayer() {
        return false;
    }

    public GameProfile getProfile() {
        return this.gameProfile;
    }

    public PlayerInventory getInventory() {
        return this.inventory;
    }

    public PlayerAbilities getAbilities() {
        return this.abilities;
    }

    public void updateTutorialInventoryAction(ItemStack cursorStack, ItemStack slotStack, ClickAction clickType) {
    }

    public Either<EntityHuman.EnumBedResult, Unit> sleep(BlockPosition pos) {
        this.entitySleep(pos);
        this.sleepCounter = 0;
        return Either.right(Unit.INSTANCE);
    }

    public void wakeup(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        super.entityWakeup();
        if (this.level instanceof WorldServer && updateSleepingPlayers) {
            ((WorldServer)this.level).everyoneSleeping();
        }

        this.sleepCounter = skipSleepTimer ? 0 : 100;
    }

    @Override
    public void entityWakeup() {
        this.wakeup(true, true);
    }

    public static Optional<Vec3D> getBed(WorldServer world, BlockPosition pos, float angle, boolean forced, boolean alive) {
        IBlockData blockState = world.getType(pos);
        Block block = blockState.getBlock();
        if (block instanceof BlockRespawnAnchor && blockState.get(BlockRespawnAnchor.CHARGE) > 0 && BlockRespawnAnchor.canSetSpawn(world)) {
            Optional<Vec3D> optional = BlockRespawnAnchor.findStandUpPosition(EntityTypes.PLAYER, world, pos);
            if (!alive && optional.isPresent()) {
                world.setTypeAndData(pos, blockState.set(BlockRespawnAnchor.CHARGE, Integer.valueOf(blockState.get(BlockRespawnAnchor.CHARGE) - 1)), 3);
            }

            return optional;
        } else if (block instanceof BlockBed && BlockBed.canSetSpawn(world)) {
            return BlockBed.findStandUpPosition(EntityTypes.PLAYER, world, pos, angle);
        } else if (!forced) {
            return Optional.empty();
        } else {
            boolean bl = block.isPossibleToRespawnInThis();
            boolean bl2 = world.getType(pos.above()).getBlock().isPossibleToRespawnInThis();
            return bl && bl2 ? Optional.of(new Vec3D((double)pos.getX() + 0.5D, (double)pos.getY() + 0.1D, (double)pos.getZ() + 0.5D)) : Optional.empty();
        }
    }

    public boolean isDeeplySleeping() {
        return this.isSleeping() && this.sleepCounter >= 100;
    }

    public int getSleepTimer() {
        return this.sleepCounter;
    }

    public void displayClientMessage(IChatBaseComponent message, boolean actionBar) {
    }

    public void awardStat(MinecraftKey stat) {
        this.awardStat(StatisticList.CUSTOM.get(stat));
    }

    public void awardStat(MinecraftKey stat, int amount) {
        this.awardStat(StatisticList.CUSTOM.get(stat), amount);
    }

    public void awardStat(Statistic<?> stat) {
        this.awardStat(stat, 1);
    }

    public void awardStat(Statistic<?> stat, int amount) {
    }

    public void resetStat(Statistic<?> stat) {
    }

    public int discoverRecipes(Collection<IRecipe<?>> recipes) {
        return 0;
    }

    public void awardRecipesByKey(MinecraftKey[] ids) {
    }

    public int undiscoverRecipes(Collection<IRecipe<?>> recipes) {
        return 0;
    }

    @Override
    public void jump() {
        super.jump();
        this.awardStat(StatisticList.JUMP);
        if (this.isSprinting()) {
            this.applyExhaustion(0.2F);
        } else {
            this.applyExhaustion(0.05F);
        }

    }

    @Override
    public void travel(Vec3D movementInput) {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        if (this.isSwimming() && !this.isPassenger()) {
            double g = this.getLookDirection().y;
            double h = g < -0.2D ? 0.085D : 0.06D;
            if (g <= 0.0D || this.jumping || !this.level.getType(new BlockPosition(this.locX(), this.locY() + 1.0D - 0.1D, this.locZ())).getFluid().isEmpty()) {
                Vec3D vec3 = this.getMot();
                this.setMot(vec3.add(0.0D, (g - vec3.y) * h, 0.0D));
            }
        }

        if (this.abilities.flying && !this.isPassenger()) {
            double i = this.getMot().y;
            float j = this.flyingSpeed;
            this.flyingSpeed = this.abilities.getFlyingSpeed() * (float)(this.isSprinting() ? 2 : 1);
            super.travel(movementInput);
            Vec3D vec32 = this.getMot();
            this.setMot(vec32.x, i * 0.6D, vec32.z);
            this.flyingSpeed = j;
            this.resetFallDistance();
            this.setFlag(7, false);
        } else {
            super.travel(movementInput);
        }

        this.checkMovement(this.locX() - d, this.locY() - e, this.locZ() - f);
    }

    @Override
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }

    }

    protected boolean freeAt(BlockPosition pos) {
        return !this.level.getType(pos).isSuffocating(this.level, pos);
    }

    @Override
    public float getSpeed() {
        return (float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED);
    }

    public void checkMovement(double dx, double dy, double dz) {
        if (!this.isPassenger()) {
            if (this.isSwimming()) {
                int i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (i > 0) {
                    this.awardStat(StatisticList.SWIM_ONE_CM, i);
                    this.applyExhaustion(0.01F * (float)i * 0.01F);
                }
            } else if (this.isEyeInFluid(TagsFluid.WATER)) {
                int j = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (j > 0) {
                    this.awardStat(StatisticList.WALK_UNDER_WATER_ONE_CM, j);
                    this.applyExhaustion(0.01F * (float)j * 0.01F);
                }
            } else if (this.isInWater()) {
                int k = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (k > 0) {
                    this.awardStat(StatisticList.WALK_ON_WATER_ONE_CM, k);
                    this.applyExhaustion(0.01F * (float)k * 0.01F);
                }
            } else if (this.isCurrentlyClimbing()) {
                if (dy > 0.0D) {
                    this.awardStat(StatisticList.CLIMB_ONE_CM, (int)Math.round(dy * 100.0D));
                }
            } else if (this.onGround) {
                int l = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (l > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(StatisticList.SPRINT_ONE_CM, l);
                        this.applyExhaustion(0.1F * (float)l * 0.01F);
                    } else if (this.isCrouching()) {
                        this.awardStat(StatisticList.CROUCH_ONE_CM, l);
                        this.applyExhaustion(0.0F * (float)l * 0.01F);
                    } else {
                        this.awardStat(StatisticList.WALK_ONE_CM, l);
                        this.applyExhaustion(0.0F * (float)l * 0.01F);
                    }
                }
            } else if (this.isGliding()) {
                int m = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                this.awardStat(StatisticList.AVIATE_ONE_CM, m);
            } else {
                int n = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (n > 25) {
                    this.awardStat(StatisticList.FLY_ONE_CM, n);
                }
            }

        }
    }

    private void checkRidingStatistics(double dx, double dy, double dz) {
        if (this.isPassenger()) {
            int i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
            if (i > 0) {
                Entity entity = this.getVehicle();
                if (entity instanceof EntityMinecartAbstract) {
                    this.awardStat(StatisticList.MINECART_ONE_CM, i);
                } else if (entity instanceof EntityBoat) {
                    this.awardStat(StatisticList.BOAT_ONE_CM, i);
                } else if (entity instanceof EntityPig) {
                    this.awardStat(StatisticList.PIG_ONE_CM, i);
                } else if (entity instanceof EntityHorseAbstract) {
                    this.awardStat(StatisticList.HORSE_ONE_CM, i);
                } else if (entity instanceof EntityStrider) {
                    this.awardStat(StatisticList.STRIDER_ONE_CM, i);
                }
            }
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.abilities.mayfly) {
            return false;
        } else {
            if (fallDistance >= 2.0F) {
                this.awardStat(StatisticList.FALL_ONE_CM, (int)Math.round((double)fallDistance * 100.0D));
            }

            return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        }
    }

    public boolean tryToStartFallFlying() {
        if (!this.onGround && !this.isGliding() && !this.isInWater() && !this.hasEffect(MobEffectList.LEVITATION)) {
            ItemStack itemStack = this.getEquipment(EnumItemSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && ItemElytra.isFlyEnabled(itemStack)) {
                this.startGliding();
                return true;
            }
        }

        return false;
    }

    public void startGliding() {
        this.setFlag(7, true);
    }

    public void stopGliding() {
        this.setFlag(7, true);
        this.setFlag(7, false);
    }

    @Override
    protected void doWaterSplashEffect() {
        if (!this.isSpectator()) {
            super.doWaterSplashEffect();
        }

    }

    @Override
    public LivingEntity$Fallsounds getFallSounds() {
        return new LivingEntity$Fallsounds(SoundEffects.PLAYER_SMALL_FALL, SoundEffects.PLAYER_BIG_FALL);
    }

    @Override
    public void killed(WorldServer world, EntityLiving other) {
        this.awardStat(StatisticList.ENTITY_KILLED.get(other.getEntityType()));
    }

    @Override
    public void makeStuckInBlock(IBlockData state, Vec3D multiplier) {
        if (!this.abilities.flying) {
            super.makeStuckInBlock(state, multiplier);
        }

    }

    public void giveExp(int experience) {
        this.addScore(experience);
        this.experienceProgress += (float)experience / (float)this.getExpToLevel();
        this.totalExperience = MathHelper.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);

        while(this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * (float)this.getExpToLevel();
            if (this.experienceLevel > 0) {
                this.levelDown(-1);
                this.experienceProgress = 1.0F + f / (float)this.getExpToLevel();
            } else {
                this.levelDown(-1);
                this.experienceProgress = 0.0F;
            }
        }

        while(this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getExpToLevel();
            this.levelDown(1);
            this.experienceProgress /= (float)this.getExpToLevel();
        }

    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void enchantDone(ItemStack enchantedItem, int experienceLevels) {
        this.experienceLevel -= experienceLevels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        this.enchantmentSeed = this.random.nextInt();
    }

    public void levelDown(int levels) {
        this.experienceLevel += levels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        if (levels > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
            float f = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_LEVELUP, this.getSoundCategory(), f * 0.75F, 1.0F);
            this.lastLevelUpTime = this.tickCount;
        }

    }

    public int getExpToLevel() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    public void applyExhaustion(float exhaustion) {
        if (!this.abilities.invulnerable) {
            if (!this.level.isClientSide) {
                this.foodData.addExhaustion(exhaustion);
            }

        }
    }

    public FoodMetaData getFoodData() {
        return this.foodData;
    }

    public boolean canEat(boolean ignoreHunger) {
        return this.abilities.invulnerable || ignoreHunger || this.foodData.needsFood();
    }

    public boolean isHurt() {
        return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
    }

    public boolean mayBuild() {
        return this.abilities.mayBuild;
    }

    public boolean mayUseItemAt(BlockPosition pos, EnumDirection facing, ItemStack stack) {
        if (this.abilities.mayBuild) {
            return true;
        } else {
            BlockPosition blockPos = pos.relative(facing.opposite());
            ShapeDetectorBlock blockInWorld = new ShapeDetectorBlock(this.level, blockPos, false);
            return stack.hasAdventureModePlaceTagForBlock(this.level.getTagManager(), blockInWorld);
        }
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
            int i = this.experienceLevel * 7;
            return i > 100 ? 100 : i;
        } else {
            return 0;
        }
    }

    @Override
    protected boolean alwaysGivesExp() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return this.abilities.flying || this.onGround && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
    }

    public void updateAbilities() {
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return new ChatComponentText(this.gameProfile.getName());
    }

    public InventoryEnderChest getEnderChest() {
        return this.enderChestInventory;
    }

    @Override
    public ItemStack getEquipment(EnumItemSlot slot) {
        if (slot == EnumItemSlot.MAINHAND) {
            return this.inventory.getItemInHand();
        } else if (slot == EnumItemSlot.OFFHAND) {
            return this.inventory.offhand.get(0);
        } else {
            return slot.getType() == EnumItemSlot.Function.ARMOR ? this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
        }
    }

    @Override
    public void setSlot(EnumItemSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        if (slot == EnumItemSlot.MAINHAND) {
            this.playEquipSound(stack);
            this.inventory.items.set(this.inventory.selected, stack);
        } else if (slot == EnumItemSlot.OFFHAND) {
            this.playEquipSound(stack);
            this.inventory.offhand.set(0, stack);
        } else if (slot.getType() == EnumItemSlot.Function.ARMOR) {
            this.playEquipSound(stack);
            this.inventory.armor.set(slot.getIndex(), stack);
        }

    }

    public boolean addItem(ItemStack stack) {
        this.playEquipSound(stack);
        return this.inventory.pickup(stack);
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Lists.newArrayList(this.getItemInMainHand(), this.getItemInOffHand());
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return this.inventory.armor;
    }

    public boolean setEntityOnShoulder(NBTTagCompound entityNbt) {
        if (!this.isPassenger() && this.onGround && !this.isInWater() && !this.isInPowderSnow) {
            if (this.getShoulderEntityLeft().isEmpty()) {
                this.setShoulderEntityLeft(entityNbt);
                this.timeEntitySatOnShoulder = this.level.getTime();
                return true;
            } else if (this.getShoulderEntityRight().isEmpty()) {
                this.setShoulderEntityRight(entityNbt);
                this.timeEntitySatOnShoulder = this.level.getTime();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void releaseShoulderEntities() {
        if (this.timeEntitySatOnShoulder + 20L < this.level.getTime()) {
            this.spawnEntityFromShoulder(this.getShoulderEntityLeft());
            this.setShoulderEntityLeft(new NBTTagCompound());
            this.spawnEntityFromShoulder(this.getShoulderEntityRight());
            this.setShoulderEntityRight(new NBTTagCompound());
        }

    }

    private void spawnEntityFromShoulder(NBTTagCompound entityNbt) {
        if (!this.level.isClientSide && !entityNbt.isEmpty()) {
            EntityTypes.create(entityNbt, this.level).ifPresent((entity) -> {
                if (entity instanceof EntityTameableAnimal) {
                    ((EntityTameableAnimal)entity).setOwnerUUID(this.uuid);
                }

                entity.setPosition(this.locX(), this.locY() + (double)0.7F, this.locZ());
                ((WorldServer)this.level).addEntitySerialized(entity);
            });
        }

    }

    @Override
    public abstract boolean isSpectator();

    @Override
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    public abstract boolean isCreative();

    @Override
    public boolean isPushedByFluid() {
        return !this.abilities.flying;
    }

    public Scoreboard getScoreboard() {
        return this.level.getScoreboard();
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        IChatMutableComponent mutableComponent = ScoreboardTeam.formatNameForTeam(this.getScoreboardTeam(), this.getDisplayName());
        return this.decorateDisplayNameComponent(mutableComponent);
    }

    private IChatMutableComponent decorateDisplayNameComponent(IChatMutableComponent component) {
        String string = this.getProfile().getName();
        return component.format((style) -> {
            return style.setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.SUGGEST_COMMAND, "/tell " + string + " ")).setChatHoverable(this.createHoverEvent()).setInsertion(string);
        });
    }

    @Override
    public String getName() {
        return this.getProfile().getName();
    }

    @Override
    public float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        switch(pose) {
        case SWIMMING:
        case FALL_FLYING:
        case SPIN_ATTACK:
            return 0.4F;
        case CROUCHING:
            return 1.27F;
        default:
            return 1.62F;
        }
    }

    @Override
    public void setAbsorptionHearts(float amount) {
        if (amount < 0.0F) {
            amount = 0.0F;
        }

        this.getDataWatcher().set(DATA_PLAYER_ABSORPTION_ID, amount);
    }

    @Override
    public float getAbsorptionHearts() {
        return this.getDataWatcher().get(DATA_PLAYER_ABSORPTION_ID);
    }

    public static UUID createPlayerUUID(GameProfile profile) {
        UUID uUID = profile.getId();
        if (uUID == null) {
            uUID = getOfflineUUID(profile.getName());
        }

        return uUID;
    }

    public static UUID getOfflineUUID(String nickname) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
    }

    public boolean isModelPartShown(PlayerModelPart modelPart) {
        return (this.getDataWatcher().get(DATA_PLAYER_MODE_CUSTOMISATION) & modelPart.getMask()) == modelPart.getMask();
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        if (mappedIndex >= 0 && mappedIndex < this.inventory.items.size()) {
            return SlotAccess.forContainer(this.inventory, mappedIndex);
        } else {
            int i = mappedIndex - 200;
            return i >= 0 && i < this.enderChestInventory.getSize() ? SlotAccess.forContainer(this.enderChestInventory, i) : super.getSlot(mappedIndex);
        }
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Override
    public void setFireTicks(int ticks) {
        super.setFireTicks(this.abilities.invulnerable ? Math.min(ticks, 1) : ticks);
    }

    @Override
    public EnumMainHand getMainHand() {
        return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? EnumMainHand.LEFT : EnumMainHand.RIGHT;
    }

    public void setMainArm(EnumMainHand arm) {
        this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(arm == EnumMainHand.LEFT ? 0 : 1));
    }

    public NBTTagCompound getShoulderEntityLeft() {
        return this.entityData.get(DATA_SHOULDER_LEFT);
    }

    public void setShoulderEntityLeft(NBTTagCompound entityNbt) {
        this.entityData.set(DATA_SHOULDER_LEFT, entityNbt);
    }

    public NBTTagCompound getShoulderEntityRight() {
        return this.entityData.get(DATA_SHOULDER_RIGHT);
    }

    public void setShoulderEntityRight(NBTTagCompound entityNbt) {
        this.entityData.set(DATA_SHOULDER_RIGHT, entityNbt);
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float)(1.0D / this.getAttributeValue(GenericAttributes.ATTACK_SPEED) * 20.0D);
    }

    public float getAttackCooldown(float baseTime) {
        return MathHelper.clamp(((float)this.attackStrengthTicker + baseTime) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public void resetAttackCooldown() {
        this.attackStrengthTicker = 0;
    }

    public ItemCooldown getCooldownTracker() {
        return this.cooldowns;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return !this.abilities.flying && !this.isGliding() ? super.getBlockSpeedFactor() : 1.0F;
    }

    public float getLuck() {
        return (float)this.getAttributeValue(GenericAttributes.LUCK);
    }

    public boolean isCreativeAndOp() {
        return this.abilities.instabuild && this.getPermissionLevel() >= 2;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(stack);
        return this.getEquipment(equipmentSlot).isEmpty();
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return POSES.getOrDefault(pose, STANDING_DIMENSIONS);
    }

    @Override
    public ImmutableList<EntityPose> getDismountPoses() {
        return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
    }

    @Override
    public ItemStack getProjectile(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemProjectileWeapon)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((ItemProjectileWeapon)stack.getItem()).getSupportedHeldProjectiles();
            ItemStack itemStack = ItemProjectileWeapon.getHeldProjectile(this, predicate);
            if (!itemStack.isEmpty()) {
                return itemStack;
            } else {
                predicate = ((ItemProjectileWeapon)stack.getItem()).getAllSupportedProjectiles();

                for(int i = 0; i < this.inventory.getSize(); ++i) {
                    ItemStack itemStack2 = this.inventory.getItem(i);
                    if (predicate.test(itemStack2)) {
                        return itemStack2;
                    }
                }

                return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
            }
        }
    }

    @Override
    public ItemStack eat(World world, ItemStack stack) {
        this.getFoodData().eat(stack.getItem(), stack);
        this.awardStat(StatisticList.ITEM_USED.get(stack.getItem()));
        world.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.PLAYER_BURP, EnumSoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (this instanceof EntityPlayer) {
            CriterionTriggers.CONSUME_ITEM.trigger((EntityPlayer)this, stack);
        }

        return super.eat(world, stack);
    }

    @Override
    protected boolean shouldRemoveSoulSpeed(IBlockData landingState) {
        return this.abilities.flying || super.shouldRemoveSoulSpeed(landingState);
    }

    @Override
    public Vec3D getRopeHoldPosition(float delta) {
        double d = 0.22D * (this.getMainHand() == EnumMainHand.RIGHT ? -1.0D : 1.0D);
        float f = MathHelper.lerp(delta * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
        float g = MathHelper.lerp(delta, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
        if (!this.isGliding() && !this.isRiptiding()) {
            if (this.isVisuallySwimming()) {
                return this.getPosition(delta).add((new Vec3D(d, 0.2D, -0.15D)).xRot(-f).yRot(-g));
            } else {
                double m = this.getBoundingBox().getYsize() - 1.0D;
                double n = this.isCrouching() ? -0.2D : 0.07D;
                return this.getPosition(delta).add((new Vec3D(d, m, n)).yRot(-g));
            }
        } else {
            Vec3D vec3 = this.getViewVector(delta);
            Vec3D vec32 = this.getMot();
            double e = vec32.horizontalDistanceSqr();
            double h = vec3.horizontalDistanceSqr();
            float k;
            if (e > 0.0D && h > 0.0D) {
                double i = (vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(e * h);
                double j = vec32.x * vec3.z - vec32.z * vec3.x;
                k = (float)(Math.signum(j) * Math.acos(i));
            } else {
                k = 0.0F;
            }

            return this.getPosition(delta).add((new Vec3D(d, -0.11D, 0.85D)).zRot(-k).xRot(-f).yRot(-g));
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isScoping() {
        return this.isHandRaised() && this.getActiveItem().is(Items.SPYGLASS);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public static enum EnumBedResult {
        NOT_POSSIBLE_HERE,
        NOT_POSSIBLE_NOW(new ChatMessage("block.minecraft.bed.no_sleep")),
        TOO_FAR_AWAY(new ChatMessage("block.minecraft.bed.too_far_away")),
        OBSTRUCTED(new ChatMessage("block.minecraft.bed.obstructed")),
        OTHER_PROBLEM,
        NOT_SAFE(new ChatMessage("block.minecraft.bed.not_safe"));

        @Nullable
        private final IChatBaseComponent message;

        private EnumBedResult() {
            this.message = null;
        }

        private EnumBedResult(IChatBaseComponent message) {
            this.message = message;
        }

        @Nullable
        public IChatBaseComponent getMessage() {
            return this.message;
        }
    }
}
