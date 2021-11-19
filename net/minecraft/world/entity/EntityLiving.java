package net.minecraft.world.entity;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutCollect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.damagesource.CombatMath;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeDefaults;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityBird;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.food.FoodInfo;
import net.minecraft.world.item.EnumAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemElytra;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.enchantment.EnchantmentFrostWalker;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.BlockHoney;
import net.minecraft.world.level.block.BlockLadder;
import net.minecraft.world.level.block.BlockPowderSnow;
import net.minecraft.world.level.block.BlockSkullAbstract;
import net.minecraft.world.level.block.BlockTrapdoor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardTeam;

public abstract class EntityLiving extends Entity {
    private static final UUID SPEED_MODIFIER_SPRINTING_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
    private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(SPEED_MODIFIER_SPRINTING_UUID, "Sprinting speed boost", (double)0.3F, AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final int HAND_SLOTS = 2;
    public static final int ARMOR_SLOTS = 4;
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int SWING_DURATION = 6;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003D;
    public static final double DEFAULT_BASE_GRAVITY = 0.08D;
    public static final int DEATH_DURATION = 20;
    private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final int USE_ITEM_INTERVAL = 4;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0D;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final DataWatcherObject<Byte> DATA_LIVING_ENTITY_FLAGS = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.BYTE);
    public static final DataWatcherObject<Float> DATA_HEALTH_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_EFFECT_COLOR_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_EFFECT_AMBIENCE_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.BOOLEAN);
    public static final DataWatcherObject<Integer> DATA_ARROW_COUNT_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_STINGER_COUNT_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Optional<BlockPosition>> SLEEPING_POS_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.OPTIONAL_BLOCK_POS);
    protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
    protected static final EntitySize SLEEPING_DIMENSIONS = EntitySize.fixed(0.2F, 0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    private final AttributeMapBase attributes;
    public CombatTracker combatTracker = new CombatTracker(this);
    public final Map<MobEffectBase, MobEffect> activeEffects = Maps.newHashMap();
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public EnumHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public float hurtDir;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    public float animationSpeedOld;
    public float animationSpeed;
    public float animationPosition;
    public int invulnerableDuration = 20;
    public final float timeOffs;
    public final float rotA;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public float flyingSpeed = 0.02F;
    @Nullable
    public EntityHuman lastHurtByPlayer;
    public int lastHurtByPlayerTime;
    protected boolean dead;
    protected int noActionTime;
    protected float oRun;
    protected float run;
    protected float animStep;
    protected float animStepO;
    protected float rotOffs;
    protected int deathScore;
    public float lastHurt;
    public boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYRot;
    protected double lerpXRot;
    protected double lyHeadRot;
    protected int lerpHeadSteps;
    public boolean effectsDirty = true;
    @Nullable
    public EntityLiving lastHurtByMob;
    public int lastHurtByMobTimestamp;
    private EntityLiving lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private BlockPosition lastPos;
    private Optional<BlockPosition> lastClimbablePos = Optional.empty();
    @Nullable
    private DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    private float swimAmount;
    private float swimAmountO;
    protected BehaviorController<?> brain;

    protected EntityLiving(EntityTypes<? extends EntityLiving> type, World world) {
        super(type, world);
        this.attributes = new AttributeMapBase(AttributeDefaults.getSupplier(type));
        this.setHealth(this.getMaxHealth());
        this.blocksBuilding = true;
        this.rotA = (float)((Math.random() + 1.0D) * (double)0.01F);
        this.reapplyPosition();
        this.timeOffs = (float)Math.random() * 12398.0F;
        this.setYRot((float)(Math.random() * (double)((float)Math.PI * 2F)));
        this.yHeadRot = this.getYRot();
        this.maxUpStep = 0.6F;
        DynamicOpsNBT nbtOps = DynamicOpsNBT.INSTANCE;
        this.brain = this.makeBrain(new Dynamic<>(nbtOps, nbtOps.createMap(ImmutableMap.of(nbtOps.createString("memories"), nbtOps.emptyMap()))));
    }

    public BehaviorController<?> getBehaviorController() {
        return this.brain;
    }

    protected BehaviorController.Provider<?> brainProvider() {
        return BehaviorController.provider(ImmutableList.of(), ImmutableList.of());
    }

    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return this.brainProvider().makeBrain(dynamic);
    }

    @Override
    public void killEntity() {
        this.damageEntity(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
    }

    public boolean canAttackType(EntityTypes<?> type) {
        return true;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        this.entityData.register(DATA_EFFECT_COLOR_ID, 0);
        this.entityData.register(DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.register(DATA_ARROW_COUNT_ID, 0);
        this.entityData.register(DATA_STINGER_COUNT_ID, 0);
        this.entityData.register(DATA_HEALTH_ID, 1.0F);
        this.entityData.register(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeProvider.Builder createLivingAttributes() {
        return AttributeProvider.builder().add(GenericAttributes.MAX_HEALTH).add(GenericAttributes.KNOCKBACK_RESISTANCE).add(GenericAttributes.MOVEMENT_SPEED).add(GenericAttributes.ARMOR).add(GenericAttributes.ARMOR_TOUGHNESS);
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
        if (!this.isInWater()) {
            this.updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (!this.level.isClientSide && onGround && this.fallDistance > 0.0F) {
            this.removeSoulSpeed();
            this.tryAddSoulSpeed();
        }

        if (!this.level.isClientSide && this.fallDistance > 3.0F && onGround) {
            float f = (float)MathHelper.ceil(this.fallDistance - 3.0F);
            if (!landedState.isAir()) {
                double d = Math.min((double)(0.2F + f / 15.0F), 2.5D);
                int i = (int)(150.0D * d);
                ((WorldServer)this.level).sendParticles(new ParticleParamBlock(Particles.BLOCK, landedState), this.locX(), this.locY(), this.locZ(), i, 0.0D, 0.0D, 0.0D, (double)0.15F);
            }
        }

        super.checkFallDamage(heightDifference, onGround, landedState, landedPosition);
    }

    public boolean canBreatheUnderwater() {
        return this.getMonsterType() == EnumMonsterType.UNDEAD;
    }

    public float getSwimAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.swimAmountO, this.swimAmount);
    }

    @Override
    public void entityBaseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getBedPosition().ifPresent(this::setPosToBed);
        }

        if (this.canSpawnSoulSpeedParticle()) {
            this.spawnSoulSpeedParticle();
        }

        super.entityBaseTick();
        this.level.getMethodProfiler().enter("livingEntityBaseTick");
        boolean bl = this instanceof EntityHuman;
        if (this.isAlive()) {
            if (this.inBlock()) {
                this.damageEntity(DamageSource.IN_WALL, 1.0F);
            } else if (bl && !this.level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double d = this.level.getWorldBorder().getDistanceToBorder(this) + this.level.getWorldBorder().getDamageBuffer();
                if (d < 0.0D) {
                    double e = this.level.getWorldBorder().getDamageAmount();
                    if (e > 0.0D) {
                        this.damageEntity(DamageSource.IN_WALL, (float)Math.max(1, MathHelper.floor(-d * e)));
                    }
                }
            }
        }

        if (this.isFireProof() || this.level.isClientSide) {
            this.extinguish();
        }

        boolean bl2 = bl && ((EntityHuman)this).getAbilities().invulnerable;
        if (this.isAlive()) {
            if (this.isEyeInFluid(TagsFluid.WATER) && !this.level.getType(new BlockPosition(this.locX(), this.getHeadY(), this.locZ())).is(Blocks.BUBBLE_COLUMN)) {
                if (!this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && !bl2) {
                    this.setAirTicks(this.decreaseAirSupply(this.getAirTicks()));
                    if (this.getAirTicks() == -20) {
                        this.setAirTicks(0);
                        Vec3D vec3 = this.getMot();

                        for(int i = 0; i < 8; ++i) {
                            double f = this.random.nextDouble() - this.random.nextDouble();
                            double g = this.random.nextDouble() - this.random.nextDouble();
                            double h = this.random.nextDouble() - this.random.nextDouble();
                            this.level.addParticle(Particles.BUBBLE, this.locX() + f, this.locY() + g, this.locZ() + h, vec3.x, vec3.y, vec3.z);
                        }

                        this.damageEntity(DamageSource.DROWN, 2.0F);
                    }
                }

                if (!this.level.isClientSide && this.isPassenger() && this.getVehicle() != null && !this.getVehicle().rideableUnderWater()) {
                    this.stopRiding();
                }
            } else if (this.getAirTicks() < this.getMaxAirSupply()) {
                this.setAirTicks(this.increaseAirSupply(this.getAirTicks()));
            }

            if (!this.level.isClientSide) {
                BlockPosition blockPos = this.getChunkCoordinates();
                if (!Objects.equal(this.lastPos, blockPos)) {
                    this.lastPos = blockPos;
                    this.onChangedBlock(blockPos);
                }
            }
        }

        if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
            if (!this.level.isClientSide && this.wasOnFire) {
                this.playEntityOnFireExtinguishedSound();
            }

            this.extinguish();
        }

        if (this.hurtTime > 0) {
            --this.hurtTime;
        }

        if (this.invulnerableTime > 0 && !(this instanceof EntityPlayer)) {
            --this.invulnerableTime;
        }

        if (this.isDeadOrDying()) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerTime > 0) {
            --this.lastHurtByPlayerTime;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        if (this.lastHurtByMob != null) {
            if (!this.lastHurtByMob.isAlive()) {
                this.setLastDamager((EntityLiving)null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastDamager((EntityLiving)null);
            }
        }

        this.tickPotionEffects();
        this.animStepO = this.animStep;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.level.getMethodProfiler().exit();
    }

    public boolean canSpawnSoulSpeedParticle() {
        return this.tickCount % 5 == 0 && this.getMot().x != 0.0D && this.getMot().z != 0.0D && !this.isSpectator() && EnchantmentManager.hasSoulSpeed(this) && this.onSoulSpeedBlock();
    }

    protected void spawnSoulSpeedParticle() {
        Vec3D vec3 = this.getMot();
        this.level.addParticle(Particles.SOUL, this.locX() + (this.random.nextDouble() - 0.5D) * (double)this.getWidth(), this.locY() + 0.1D, this.locZ() + (this.random.nextDouble() - 0.5D) * (double)this.getWidth(), vec3.x * -0.2D, 0.1D, vec3.z * -0.2D);
        float f = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;
        this.playSound(SoundEffects.SOUL_ESCAPE, f, 0.6F + this.random.nextFloat() * 0.4F);
    }

    protected boolean onSoulSpeedBlock() {
        return this.level.getType(this.getBlockPosBelowThatAffectsMyMovement()).is(TagsBlock.SOUL_SPEED_BLOCKS);
    }

    @Override
    protected float getBlockSpeedFactor() {
        return this.onSoulSpeedBlock() && EnchantmentManager.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
    }

    protected boolean shouldRemoveSoulSpeed(IBlockData landingState) {
        return !landingState.isAir() || this.isGliding();
    }

    protected void removeSoulSpeed() {
        AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
                attributeInstance.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
            }

        }
    }

    protected void tryAddSoulSpeed() {
        if (!this.getBlockStateOn().isAir()) {
            int i = EnchantmentManager.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);
            if (i > 0 && this.onSoulSpeedBlock()) {
                AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
                if (attributeInstance == null) {
                    return;
                }

                attributeInstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", (double)(0.03F * (1.0F + (float)i * 0.35F)), AttributeModifier.Operation.ADDITION));
                if (this.getRandom().nextFloat() < 0.04F) {
                    ItemStack itemStack = this.getEquipment(EnumItemSlot.FEET);
                    itemStack.damage(1, this, (player) -> {
                        player.broadcastItemBreak(EnumItemSlot.FEET);
                    });
                }
            }
        }

    }

    protected void removeFrost() {
        AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
                attributeInstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
            }

        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOn().isAir()) {
            int i = this.getTicksFrozen();
            if (i > 0) {
                AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
                if (attributeInstance == null) {
                    return;
                }

                float f = -0.05F * this.getPercentFrozen();
                attributeInstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", (double)f, AttributeModifier.Operation.ADDITION));
            }
        }

    }

    protected void onChangedBlock(BlockPosition pos) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.FROST_WALKER, this);
        if (i > 0) {
            EnchantmentFrostWalker.onEntityMoved(this, this.level, pos, i);
        }

        if (this.shouldRemoveSoulSpeed(this.getBlockStateOn())) {
            this.removeSoulSpeed();
        }

        this.tryAddSoulSpeed();
    }

    public boolean isBaby() {
        return false;
    }

    public float getScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    protected boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean rideableUnderWater() {
        return false;
    }

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20 && !this.level.isClientSide()) {
            this.level.broadcastEntityEffect(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }

    }

    protected boolean isDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot() {
        return !this.isBaby();
    }

    protected int decreaseAirSupply(int air) {
        int i = EnchantmentManager.getOxygenEnchantmentLevel(this);
        return i > 0 && this.random.nextInt(i + 1) > 0 ? air : air - 1;
    }

    protected int increaseAirSupply(int air) {
        return Math.min(air + 4, this.getMaxAirSupply());
    }

    protected int getExpValue(EntityHuman player) {
        return 0;
    }

    protected boolean alwaysGivesExp() {
        return false;
    }

    public Random getRandom() {
        return this.random;
    }

    @Nullable
    public EntityLiving getLastDamager() {
        return this.lastHurtByMob;
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(@Nullable EntityHuman attacking) {
        this.lastHurtByPlayer = attacking;
        this.lastHurtByPlayerTime = this.tickCount;
    }

    public void setLastDamager(@Nullable EntityLiving attacker) {
        this.lastHurtByMob = attacker;
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    @Nullable
    public EntityLiving getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity target) {
        if (target instanceof EntityLiving) {
            this.lastHurtMob = (EntityLiving)target;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int despawnCounter) {
        this.noActionTime = despawnCounter;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean noDrag) {
        this.discardFriction = noDrag;
    }

    protected void playEquipSound(ItemStack stack) {
        SoundEffect soundEvent = stack.getEquipSound();
        if (!stack.isEmpty() && soundEvent != null && !this.isSpectator()) {
            this.gameEvent(GameEvent.EQUIP);
            this.playSound(soundEvent, 1.0F, 1.0F);
        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        nbt.setFloat("Health", this.getHealth());
        nbt.setShort("HurtTime", (short)this.hurtTime);
        nbt.setInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        nbt.setShort("DeathTime", (short)this.deathTime);
        nbt.setFloat("AbsorptionAmount", this.getAbsorptionHearts());
        nbt.set("Attributes", this.getAttributeMap().save());
        if (!this.activeEffects.isEmpty()) {
            NBTTagList listTag = new NBTTagList();

            for(MobEffect mobEffectInstance : this.activeEffects.values()) {
                listTag.add(mobEffectInstance.save(new NBTTagCompound()));
            }

            nbt.set("ActiveEffects", listTag);
        }

        nbt.setBoolean("FallFlying", this.isGliding());
        this.getBedPosition().ifPresent((pos) -> {
            nbt.setInt("SleepingX", pos.getX());
            nbt.setInt("SleepingY", pos.getY());
            nbt.setInt("SleepingZ", pos.getZ());
        });
        DataResult<NBTBase> dataResult = this.brain.serializeStart(DynamicOpsNBT.INSTANCE);
        dataResult.resultOrPartial(LOGGER::error).ifPresent((brain) -> {
            nbt.set("Brain", brain);
        });
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.setAbsorptionHearts(nbt.getFloat("AbsorptionAmount"));
        if (nbt.hasKeyOfType("Attributes", 9) && this.level != null && !this.level.isClientSide) {
            this.getAttributeMap().load(nbt.getList("Attributes", 10));
        }

        if (nbt.hasKeyOfType("ActiveEffects", 9)) {
            NBTTagList listTag = nbt.getList("ActiveEffects", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                MobEffect mobEffectInstance = MobEffect.load(compoundTag);
                if (mobEffectInstance != null) {
                    this.activeEffects.put(mobEffectInstance.getMobEffect(), mobEffectInstance);
                }
            }
        }

        if (nbt.hasKeyOfType("Health", 99)) {
            this.setHealth(nbt.getFloat("Health"));
        }

        this.hurtTime = nbt.getShort("HurtTime");
        this.deathTime = nbt.getShort("DeathTime");
        this.lastHurtByMobTimestamp = nbt.getInt("HurtByTimestamp");
        if (nbt.hasKeyOfType("Team", 8)) {
            String string = nbt.getString("Team");
            ScoreboardTeam playerTeam = this.level.getScoreboard().getTeam(string);
            boolean bl = playerTeam != null && this.level.getScoreboard().addPlayerToTeam(this.getUniqueIDString(), playerTeam);
            if (!bl) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)string);
            }
        }

        if (nbt.getBoolean("FallFlying")) {
            this.setFlag(7, true);
        }

        if (nbt.hasKeyOfType("SleepingX", 99) && nbt.hasKeyOfType("SleepingY", 99) && nbt.hasKeyOfType("SleepingZ", 99)) {
            BlockPosition blockPos = new BlockPosition(nbt.getInt("SleepingX"), nbt.getInt("SleepingY"), nbt.getInt("SleepingZ"));
            this.setSleepingPos(blockPos);
            this.entityData.set(DATA_POSE, EntityPose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(blockPos);
            }
        }

        if (nbt.hasKeyOfType("Brain", 10)) {
            this.brain = this.makeBrain(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("Brain")));
        }

    }

    protected void tickPotionEffects() {
        Iterator<MobEffectBase> iterator = this.activeEffects.keySet().iterator();

        try {
            while(iterator.hasNext()) {
                MobEffectBase mobEffect = iterator.next();
                MobEffect mobEffectInstance = this.activeEffects.get(mobEffect);
                if (!mobEffectInstance.tick(this, () -> {
                    this.onEffectUpdated(mobEffectInstance, true, (Entity)null);
                })) {
                    if (!this.level.isClientSide) {
                        iterator.remove();
                        this.onEffectRemoved(mobEffectInstance);
                    }
                } else if (mobEffectInstance.getDuration() % 600 == 0) {
                    this.onEffectUpdated(mobEffectInstance, false, (Entity)null);
                }
            }
        } catch (ConcurrentModificationException var11) {
        }

        if (this.effectsDirty) {
            if (!this.level.isClientSide) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
            }

            this.effectsDirty = false;
        }

        int i = this.entityData.get(DATA_EFFECT_COLOR_ID);
        boolean bl = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
        if (i > 0) {
            boolean bl2;
            if (this.isInvisible()) {
                bl2 = this.random.nextInt(15) == 0;
            } else {
                bl2 = this.random.nextBoolean();
            }

            if (bl) {
                bl2 &= this.random.nextInt(5) == 0;
            }

            if (bl2 && i > 0) {
                double d = (double)(i >> 16 & 255) / 255.0D;
                double e = (double)(i >> 8 & 255) / 255.0D;
                double f = (double)(i >> 0 & 255) / 255.0D;
                this.level.addParticle(bl ? Particles.AMBIENT_ENTITY_EFFECT : Particles.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d, e, f);
            }
        }

    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            Collection<MobEffect> collection = this.activeEffects.values();
            this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(collection));
            this.entityData.set(DATA_EFFECT_COLOR_ID, PotionUtil.getColor(collection));
            this.setInvisible(this.hasEffect(MobEffectList.INVISIBILITY));
        }

    }

    private void updateGlowingStatus() {
        boolean bl = this.isCurrentlyGlowing();
        if (this.getFlag(6) != bl) {
            this.setFlag(6, bl);
        }

    }

    public double getVisibilityPercent(@Nullable Entity entity) {
        double d = 1.0D;
        if (this.isDiscrete()) {
            d *= 0.8D;
        }

        if (this.isInvisible()) {
            float f = this.getArmorCoverPercentage();
            if (f < 0.1F) {
                f = 0.1F;
            }

            d *= 0.7D * (double)f;
        }

        if (entity != null) {
            ItemStack itemStack = this.getEquipment(EnumItemSlot.HEAD);
            EntityTypes<?> entityType = entity.getEntityType();
            if (entityType == EntityTypes.SKELETON && itemStack.is(Items.SKELETON_SKULL) || entityType == EntityTypes.ZOMBIE && itemStack.is(Items.ZOMBIE_HEAD) || entityType == EntityTypes.CREEPER && itemStack.is(Items.CREEPER_HEAD)) {
                d *= 0.5D;
            }
        }

        return d;
    }

    public boolean canAttack(EntityLiving target) {
        return target instanceof EntityHuman && this.level.getDifficulty() == EnumDifficulty.PEACEFUL ? false : target.canBeSeenAsEnemy();
    }

    public boolean canAttack(EntityLiving entity, PathfinderTargetCondition predicate) {
        return predicate.test(this, entity);
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffect> effects) {
        for(MobEffect mobEffectInstance : effects) {
            if (!mobEffectInstance.isAmbient()) {
                return false;
            }
        }

        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
    }

    public boolean removeAllEffects() {
        if (this.level.isClientSide) {
            return false;
        } else {
            Iterator<MobEffect> iterator = this.activeEffects.values().iterator();

            boolean bl;
            for(bl = false; iterator.hasNext(); bl = true) {
                this.onEffectRemoved(iterator.next());
                iterator.remove();
            }

            return bl;
        }
    }

    public Collection<MobEffect> getEffects() {
        return this.activeEffects.values();
    }

    public Map<MobEffectBase, MobEffect> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(MobEffectBase effect) {
        return this.activeEffects.containsKey(effect);
    }

    @Nullable
    public MobEffect getEffect(MobEffectBase effect) {
        return this.activeEffects.get(effect);
    }

    public final boolean addEffect(MobEffect effect) {
        return this.addEffect(effect, (Entity)null);
    }

    public boolean addEffect(MobEffect effect, @Nullable Entity source) {
        if (!this.canBeAffected(effect)) {
            return false;
        } else {
            MobEffect mobEffectInstance = this.activeEffects.get(effect.getMobEffect());
            if (mobEffectInstance == null) {
                this.activeEffects.put(effect.getMobEffect(), effect);
                this.onEffectAdded(effect, source);
                return true;
            } else if (mobEffectInstance.update(effect)) {
                this.onEffectUpdated(mobEffectInstance, true, source);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean canBeAffected(MobEffect effect) {
        if (this.getMonsterType() == EnumMonsterType.UNDEAD) {
            MobEffectBase mobEffect = effect.getMobEffect();
            if (mobEffect == MobEffectList.REGENERATION || mobEffect == MobEffectList.POISON) {
                return false;
            }
        }

        return true;
    }

    public void forceAddEffect(MobEffect effect, @Nullable Entity source) {
        if (this.canBeAffected(effect)) {
            MobEffect mobEffectInstance = this.activeEffects.put(effect.getMobEffect(), effect);
            if (mobEffectInstance == null) {
                this.onEffectAdded(effect, source);
            } else {
                this.onEffectUpdated(effect, true, source);
            }

        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.getMonsterType() == EnumMonsterType.UNDEAD;
    }

    @Nullable
    public MobEffect removeEffectNoUpdate(@Nullable MobEffectBase type) {
        return this.activeEffects.remove(type);
    }

    public boolean removeEffect(MobEffectBase type) {
        MobEffect mobEffectInstance = this.removeEffectNoUpdate(type);
        if (mobEffectInstance != null) {
            this.onEffectRemoved(mobEffectInstance);
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffect effect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            effect.getMobEffect().addAttributeModifiers(this, this.getAttributeMap(), effect.getAmplifier());
        }

    }

    protected void onEffectUpdated(MobEffect effect, boolean reapplyEffect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (reapplyEffect && !this.level.isClientSide) {
            MobEffectBase mobEffect = effect.getMobEffect();
            mobEffect.removeAttributeModifiers(this, this.getAttributeMap(), effect.getAmplifier());
            mobEffect.addAttributeModifiers(this, this.getAttributeMap(), effect.getAmplifier());
        }

    }

    protected void onEffectRemoved(MobEffect effect) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            effect.getMobEffect().removeAttributeModifiers(this, this.getAttributeMap(), effect.getAmplifier());
        }

    }

    public void heal(float amount) {
        float f = this.getHealth();
        if (f > 0.0F) {
            this.setHealth(f + amount);
        }

    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH_ID, MathHelper.clamp(health, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (this.level.isClientSide) {
            return false;
        } else if (this.isDeadOrDying()) {
            return false;
        } else if (source.isFire() && this.hasEffect(MobEffectList.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping() && !this.level.isClientSide) {
                this.entityWakeup();
            }

            this.noActionTime = 0;
            float f = amount;
            boolean bl = false;
            float g = 0.0F;
            if (amount > 0.0F && this.applyBlockingModifier(source)) {
                this.damageShield(amount);
                g = amount;
                amount = 0.0F;
                if (!source.isProjectile()) {
                    Entity entity = source.getDirectEntity();
                    if (entity instanceof EntityLiving) {
                        this.shieldBlock((EntityLiving)entity);
                    }
                }

                bl = true;
            }

            this.animationSpeed = 1.5F;
            boolean bl2 = true;
            if ((float)this.invulnerableTime > 10.0F) {
                if (amount <= this.lastHurt) {
                    return false;
                }

                this.damageEntity0(source, amount - this.lastHurt);
                this.lastHurt = amount;
                bl2 = false;
            } else {
                this.lastHurt = amount;
                this.invulnerableTime = 20;
                this.damageEntity0(source, amount);
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            if (source.isDamageHelmet() && !this.getEquipment(EnumItemSlot.HEAD).isEmpty()) {
                this.damageHelmet(source, amount);
                amount *= 0.75F;
            }

            this.hurtDir = 0.0F;
            Entity entity2 = source.getEntity();
            if (entity2 != null) {
                if (entity2 instanceof EntityLiving && !source.isNoAggro()) {
                    this.setLastDamager((EntityLiving)entity2);
                }

                if (entity2 instanceof EntityHuman) {
                    this.lastHurtByPlayerTime = 100;
                    this.lastHurtByPlayer = (EntityHuman)entity2;
                } else if (entity2 instanceof EntityWolf) {
                    EntityWolf wolf = (EntityWolf)entity2;
                    if (wolf.isTamed()) {
                        this.lastHurtByPlayerTime = 100;
                        EntityLiving livingEntity = wolf.getOwner();
                        if (livingEntity != null && livingEntity.getEntityType() == EntityTypes.PLAYER) {
                            this.lastHurtByPlayer = (EntityHuman)livingEntity;
                        } else {
                            this.lastHurtByPlayer = null;
                        }
                    }
                }
            }

            if (bl2) {
                if (bl) {
                    this.level.broadcastEntityEffect(this, (byte)29);
                } else if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns()) {
                    this.level.broadcastEntityEffect(this, (byte)33);
                } else {
                    byte b;
                    if (source == DamageSource.DROWN) {
                        b = 36;
                    } else if (source.isFire()) {
                        b = 37;
                    } else if (source == DamageSource.SWEET_BERRY_BUSH) {
                        b = 44;
                    } else if (source == DamageSource.FREEZE) {
                        b = 57;
                    } else {
                        b = 2;
                    }

                    this.level.broadcastEntityEffect(this, b);
                }

                if (source != DamageSource.DROWN && (!bl || amount > 0.0F)) {
                    this.velocityChanged();
                }

                if (entity2 != null) {
                    double i = entity2.locX() - this.locX();

                    double j;
                    for(j = entity2.locZ() - this.locZ(); i * i + j * j < 1.0E-4D; j = (Math.random() - Math.random()) * 0.01D) {
                        i = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.hurtDir = (float)(MathHelper.atan2(j, i) * (double)(180F / (float)Math.PI) - (double)this.getYRot());
                    this.knockback((double)0.4F, i, j);
                } else {
                    this.hurtDir = (float)((int)(Math.random() * 2.0D) * 180);
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(source)) {
                    SoundEffect soundEvent = this.getSoundDeath();
                    if (bl2 && soundEvent != null) {
                        this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
                    }

                    this.die(source);
                }
            } else if (bl2) {
                this.playHurtSound(source);
            }

            boolean bl3 = !bl || amount > 0.0F;
            if (bl3) {
                this.lastDamageSource = source;
                this.lastDamageStamp = this.level.getTime();
            }

            if (this instanceof EntityPlayer) {
                CriterionTriggers.ENTITY_HURT_PLAYER.trigger((EntityPlayer)this, source, f, amount, bl);
                if (g > 0.0F && g < 3.4028235E37F) {
                    ((EntityPlayer)this).awardStat(StatisticList.DAMAGE_BLOCKED_BY_SHIELD, Math.round(g * 10.0F));
                }
            }

            if (entity2 instanceof EntityPlayer) {
                CriterionTriggers.PLAYER_HURT_ENTITY.trigger((EntityPlayer)entity2, this, source, f, amount, bl);
            }

            return bl3;
        }
    }

    protected void shieldBlock(EntityLiving attacker) {
        attacker.blockedByShield(this);
    }

    protected void blockedByShield(EntityLiving target) {
        target.knockback(0.5D, target.locX() - this.locX(), target.locZ() - this.locZ());
    }

    private boolean checkTotemDeathProtection(DamageSource source) {
        if (source.ignoresInvulnerability()) {
            return false;
        } else {
            ItemStack itemStack = null;

            for(EnumHand interactionHand : EnumHand.values()) {
                ItemStack itemStack2 = this.getItemInHand(interactionHand);
                if (itemStack2.is(Items.TOTEM_OF_UNDYING)) {
                    itemStack = itemStack2.cloneItemStack();
                    itemStack2.subtract(1);
                    break;
                }
            }

            if (itemStack != null) {
                if (this instanceof EntityPlayer) {
                    EntityPlayer serverPlayer = (EntityPlayer)this;
                    serverPlayer.awardStat(StatisticList.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriterionTriggers.USED_TOTEM.trigger(serverPlayer, itemStack);
                }

                this.setHealth(1.0F);
                this.removeAllEffects();
                this.addEffect(new MobEffect(MobEffectList.REGENERATION, 900, 1));
                this.addEffect(new MobEffect(MobEffectList.ABSORPTION, 100, 1));
                this.addEffect(new MobEffect(MobEffectList.FIRE_RESISTANCE, 800, 0));
                this.level.broadcastEntityEffect(this, (byte)35);
            }

            return itemStack != null;
        }
    }

    @Nullable
    public DamageSource getLastDamageSource() {
        if (this.level.getTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource source) {
        SoundEffect soundEvent = this.getSoundHurt(source);
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    public boolean applyBlockingModifier(DamageSource source) {
        Entity entity = source.getDirectEntity();
        boolean bl = false;
        if (entity instanceof EntityArrow) {
            EntityArrow abstractArrow = (EntityArrow)entity;
            if (abstractArrow.getPierceLevel() > 0) {
                bl = true;
            }
        }

        if (!source.ignoresArmor() && this.isBlocking() && !bl) {
            Vec3D vec3 = source.getSourcePosition();
            if (vec3 != null) {
                Vec3D vec32 = this.getViewVector(1.0F);
                Vec3D vec33 = vec3.vectorTo(this.getPositionVector()).normalize();
                vec33 = new Vec3D(vec33.x, 0.0D, vec33.z);
                if (vec33.dot(vec32) < 0.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    private void breakItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            if (!this.isSilent()) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.ITEM_BREAK, this.getSoundCategory(), 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F, false);
            }

            this.spawnItemParticles(stack, 5);
        }

    }

    public void die(DamageSource source) {
        if (!this.isRemoved() && !this.dead) {
            Entity entity = source.getEntity();
            EntityLiving livingEntity = this.getKillingEntity();
            if (this.deathScore >= 0 && livingEntity != null) {
                livingEntity.awardKillScore(this, this.deathScore, source);
            }

            if (this.isSleeping()) {
                this.entityWakeup();
            }

            if (!this.level.isClientSide && this.hasCustomName()) {
                LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level instanceof WorldServer) {
                if (entity != null) {
                    entity.killed((WorldServer)this.level, this);
                }

                this.dropAllDeathLoot(source);
                this.createWitherRose(livingEntity);
            }

            this.level.broadcastEntityEffect(this, (byte)3);
            this.setPose(EntityPose.DYING);
        }
    }

    protected void createWitherRose(@Nullable EntityLiving adversary) {
        if (!this.level.isClientSide) {
            boolean bl = false;
            if (adversary instanceof EntityWither) {
                if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    BlockPosition blockPos = this.getChunkCoordinates();
                    IBlockData blockState = Blocks.WITHER_ROSE.getBlockData();
                    if (this.level.getType(blockPos).isAir() && blockState.canPlace(this.level, blockPos)) {
                        this.level.setTypeAndData(blockPos, blockState, 3);
                        bl = true;
                    }
                }

                if (!bl) {
                    EntityItem itemEntity = new EntityItem(this.level, this.locX(), this.locY(), this.locZ(), new ItemStack(Items.WITHER_ROSE));
                    this.level.addEntity(itemEntity);
                }
            }

        }
    }

    protected void dropAllDeathLoot(DamageSource source) {
        Entity entity = source.getEntity();
        int i;
        if (entity instanceof EntityHuman) {
            i = EnchantmentManager.getMobLooting((EntityLiving)entity);
        } else {
            i = 0;
        }

        boolean bl = this.lastHurtByPlayerTime > 0;
        if (this.shouldDropLoot() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.dropFromLootTable(source, bl);
            this.dropDeathLoot(source, i, bl);
        }

        this.dropInventory();
        this.dropExperience();
    }

    protected void dropInventory() {
    }

    protected void dropExperience() {
        if (this.level instanceof WorldServer && (this.alwaysGivesExp() || this.lastHurtByPlayerTime > 0 && this.isDropExperience() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            EntityExperienceOrb.award((WorldServer)this.level, this.getPositionVector(), this.getExpValue(this.lastHurtByPlayer));
        }

    }

    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
    }

    public MinecraftKey getLootTable() {
        return this.getEntityType().getDefaultLootTable();
    }

    protected void dropFromLootTable(DamageSource source, boolean causedByPlayer) {
        MinecraftKey resourceLocation = this.getLootTable();
        LootTable lootTable = this.level.getMinecraftServer().getLootTableRegistry().getLootTable(resourceLocation);
        LootTableInfo.Builder builder = this.createLootContext(causedByPlayer, source);
        lootTable.populateLoot(builder.build(LootContextParameterSets.ENTITY), this::spawnAtLocation);
    }

    protected LootTableInfo.Builder createLootContext(boolean causedByPlayer, DamageSource source) {
        LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.level)).withRandom(this.random).set(LootContextParameters.THIS_ENTITY, this).set(LootContextParameters.ORIGIN, this.getPositionVector()).set(LootContextParameters.DAMAGE_SOURCE, source).setOptional(LootContextParameters.KILLER_ENTITY, source.getEntity()).setOptional(LootContextParameters.DIRECT_KILLER_ENTITY, source.getDirectEntity());
        if (causedByPlayer && this.lastHurtByPlayer != null) {
            builder = builder.set(LootContextParameters.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
        }

        return builder;
    }

    public void knockback(double strength, double x, double z) {
        strength = strength * (1.0D - this.getAttributeValue(GenericAttributes.KNOCKBACK_RESISTANCE));
        if (!(strength <= 0.0D)) {
            this.hasImpulse = true;
            Vec3D vec3 = this.getMot();
            Vec3D vec32 = (new Vec3D(x, 0.0D, z)).normalize().scale(strength);
            this.setMot(vec3.x / 2.0D - vec32.x, this.onGround ? Math.min(0.4D, vec3.y / 2.0D + strength) : vec3.y, vec3.z / 2.0D - vec32.z);
        }
    }

    @Nullable
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.GENERIC_HURT;
    }

    @Nullable
    public SoundEffect getSoundDeath() {
        return SoundEffects.GENERIC_DEATH;
    }

    protected SoundEffect getSoundFall(int distance) {
        return distance > 4 ? SoundEffects.GENERIC_BIG_FALL : SoundEffects.GENERIC_SMALL_FALL;
    }

    protected SoundEffect getDrinkingSound(ItemStack stack) {
        return stack.getDrinkingSound();
    }

    public SoundEffect getEatingSound(ItemStack stack) {
        return stack.getEatingSound();
    }

    @Override
    public void setOnGround(boolean onGround) {
        super.setOnGround(onGround);
        if (onGround) {
            this.lastClimbablePos = Optional.empty();
        }

    }

    public Optional<BlockPosition> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean isCurrentlyClimbing() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPosition blockPos = this.getChunkCoordinates();
            IBlockData blockState = this.getFeetBlockState();
            if (blockState.is(TagsBlock.CLIMBABLE)) {
                this.lastClimbablePos = Optional.of(blockPos);
                return true;
            } else if (blockState.getBlock() instanceof BlockTrapdoor && this.trapdoorUsableAsLadder(blockPos, blockState)) {
                this.lastClimbablePos = Optional.of(blockPos);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPosition pos, IBlockData state) {
        if (state.get(BlockTrapdoor.OPEN)) {
            IBlockData blockState = this.level.getType(pos.below());
            if (blockState.is(Blocks.LADDER) && blockState.get(BlockLadder.FACING) == state.get(BlockTrapdoor.FACING)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        boolean bl = super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        int i = this.calculateFallDamage(fallDistance, damageMultiplier);
        if (i > 0) {
            this.playSound(this.getSoundFall(i), 1.0F, 1.0F);
            this.playBlockStepSound();
            this.damageEntity(damageSource, (float)i);
            return true;
        } else {
            return bl;
        }
    }

    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        MobEffect mobEffectInstance = this.getEffect(MobEffectList.JUMP);
        float f = mobEffectInstance == null ? 0.0F : (float)(mobEffectInstance.getAmplifier() + 1);
        return MathHelper.ceil((fallDistance - 3.0F - f) * damageMultiplier);
    }

    protected void playBlockStepSound() {
        if (!this.isSilent()) {
            int i = MathHelper.floor(this.locX());
            int j = MathHelper.floor(this.locY() - (double)0.2F);
            int k = MathHelper.floor(this.locZ());
            IBlockData blockState = this.level.getType(new BlockPosition(i, j, k));
            if (!blockState.isAir()) {
                SoundEffectType soundType = blockState.getStepSound();
                this.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F, soundType.getPitch() * 0.75F);
            }

        }
    }

    @Override
    public void animateHurt() {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        this.hurtDir = 0.0F;
    }

    public int getArmorStrength() {
        return MathHelper.floor(this.getAttributeValue(GenericAttributes.ARMOR));
    }

    protected void damageArmor(DamageSource source, float amount) {
    }

    protected void damageHelmet(DamageSource source, float amount) {
    }

    protected void damageShield(float amount) {
    }

    protected float applyArmorModifier(DamageSource source, float amount) {
        if (!source.ignoresArmor()) {
            this.damageArmor(source, amount);
            amount = CombatMath.getDamageAfterAbsorb(amount, (float)this.getArmorStrength(), (float)this.getAttributeValue(GenericAttributes.ARMOR_TOUGHNESS));
        }

        return amount;
    }

    protected float applyMagicModifier(DamageSource source, float amount) {
        if (source.isStarvation()) {
            return amount;
        } else {
            if (this.hasEffect(MobEffectList.DAMAGE_RESISTANCE) && source != DamageSource.OUT_OF_WORLD) {
                int i = (this.getEffect(MobEffectList.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = amount * (float)j;
                float g = amount;
                amount = Math.max(f / 25.0F, 0.0F);
                float h = g - amount;
                if (h > 0.0F && h < 3.4028235E37F) {
                    if (this instanceof EntityPlayer) {
                        ((EntityPlayer)this).awardStat(StatisticList.DAMAGE_RESISTED, Math.round(h * 10.0F));
                    } else if (source.getEntity() instanceof EntityPlayer) {
                        ((EntityPlayer)source.getEntity()).awardStat(StatisticList.DAMAGE_DEALT_RESISTED, Math.round(h * 10.0F));
                    }
                }
            }

            if (amount <= 0.0F) {
                return 0.0F;
            } else {
                int k = EnchantmentManager.getDamageProtection(this.getArmorItems(), source);
                if (k > 0) {
                    amount = CombatMath.getDamageAfterMagicAbsorb(amount, (float)k);
                }

                return amount;
            }
        }
    }

    protected void damageEntity0(DamageSource source, float amount) {
        if (!this.isInvulnerable(source)) {
            amount = this.applyArmorModifier(source, amount);
            amount = this.applyMagicModifier(source, amount);
            float var8 = Math.max(amount - this.getAbsorptionHearts(), 0.0F);
            this.setAbsorptionHearts(this.getAbsorptionHearts() - (amount - var8));
            float g = amount - var8;
            if (g > 0.0F && g < 3.4028235E37F && source.getEntity() instanceof EntityPlayer) {
                ((EntityPlayer)source.getEntity()).awardStat(StatisticList.DAMAGE_DEALT_ABSORBED, Math.round(g * 10.0F));
            }

            if (var8 != 0.0F) {
                float h = this.getHealth();
                this.setHealth(h - var8);
                this.getCombatTracker().trackDamage(source, h, var8);
                this.setAbsorptionHearts(this.getAbsorptionHearts() - var8);
                this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
            }
        }
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    @Nullable
    public EntityLiving getKillingEntity() {
        if (this.combatTracker.getKiller() != null) {
            return this.combatTracker.getKiller();
        } else if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer;
        } else {
            return this.lastHurtByMob != null ? this.lastHurtByMob : null;
        }
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(GenericAttributes.MAX_HEALTH);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int stuckArrowCount) {
        this.entityData.set(DATA_ARROW_COUNT_ID, stuckArrowCount);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int stingerCount) {
        this.entityData.set(DATA_STINGER_COUNT_ID, stingerCount);
    }

    private int getCurrentSwingDuration() {
        if (MobEffectUtil.hasDigSpeed(this)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else {
            return this.hasEffect(MobEffectList.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffectList.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
        }
    }

    public void swingHand(EnumHand hand) {
        this.swingHand(hand, false);
    }

    public void swingHand(EnumHand hand, boolean fromServerPlayer) {
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = hand;
            if (this.level instanceof WorldServer) {
                PacketPlayOutAnimation clientboundAnimatePacket = new PacketPlayOutAnimation(this, hand == EnumHand.MAIN_HAND ? 0 : 3);
                ChunkProviderServer serverChunkCache = ((WorldServer)this.level).getChunkSource();
                if (fromServerPlayer) {
                    serverChunkCache.broadcastIncludingSelf(this, clientboundAnimatePacket);
                } else {
                    serverChunkCache.broadcast(this, clientboundAnimatePacket);
                }
            }
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        switch(status) {
        case 2:
        case 33:
        case 36:
        case 37:
        case 44:
        case 57:
            this.animationSpeed = 1.5F;
            this.invulnerableTime = 20;
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
            this.hurtDir = 0.0F;
            if (status == 33) {
                this.playSound(SoundEffects.THORNS_HIT, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            DamageSource damageSource;
            if (status == 37) {
                damageSource = DamageSource.ON_FIRE;
            } else if (status == 36) {
                damageSource = DamageSource.DROWN;
            } else if (status == 44) {
                damageSource = DamageSource.SWEET_BERRY_BUSH;
            } else if (status == 57) {
                damageSource = DamageSource.FREEZE;
            } else {
                damageSource = DamageSource.GENERIC;
            }

            SoundEffect soundEvent = this.getSoundHurt(damageSource);
            if (soundEvent != null) {
                this.playSound(soundEvent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            this.damageEntity(DamageSource.GENERIC, 0.0F);
            this.lastDamageSource = damageSource;
            this.lastDamageStamp = this.level.getTime();
            break;
        case 3:
            SoundEffect soundEvent2 = this.getSoundDeath();
            if (soundEvent2 != null) {
                this.playSound(soundEvent2, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof EntityHuman)) {
                this.setHealth(0.0F);
                this.die(DamageSource.GENERIC);
            }
            break;
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
        case 15:
        case 16:
        case 17:
        case 18:
        case 19:
        case 20:
        case 21:
        case 22:
        case 23:
        case 24:
        case 25:
        case 26:
        case 27:
        case 28:
        case 31:
        case 32:
        case 34:
        case 35:
        case 38:
        case 39:
        case 40:
        case 41:
        case 42:
        case 43:
        case 45:
        case 53:
        case 56:
        case 58:
        case 59:
        default:
            super.handleEntityEvent(status);
            break;
        case 29:
            this.playSound(SoundEffects.SHIELD_BLOCK, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
        case 30:
            this.playSound(SoundEffects.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
        case 46:
            int i = 128;

            for(int j = 0; j < 128; ++j) {
                double d = (double)j / 127.0D;
                float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                float g = (this.random.nextFloat() - 0.5F) * 0.2F;
                float h = (this.random.nextFloat() - 0.5F) * 0.2F;
                double e = MathHelper.lerp(d, this.xo, this.locX()) + (this.random.nextDouble() - 0.5D) * (double)this.getWidth() * 2.0D;
                double k = MathHelper.lerp(d, this.yo, this.locY()) + this.random.nextDouble() * (double)this.getHeight();
                double l = MathHelper.lerp(d, this.zo, this.locZ()) + (this.random.nextDouble() - 0.5D) * (double)this.getWidth() * 2.0D;
                this.level.addParticle(Particles.PORTAL, e, k, l, (double)f, (double)g, (double)h);
            }
            break;
        case 47:
            this.breakItem(this.getEquipment(EnumItemSlot.MAINHAND));
            break;
        case 48:
            this.breakItem(this.getEquipment(EnumItemSlot.OFFHAND));
            break;
        case 49:
            this.breakItem(this.getEquipment(EnumItemSlot.HEAD));
            break;
        case 50:
            this.breakItem(this.getEquipment(EnumItemSlot.CHEST));
            break;
        case 51:
            this.breakItem(this.getEquipment(EnumItemSlot.LEGS));
            break;
        case 52:
            this.breakItem(this.getEquipment(EnumItemSlot.FEET));
            break;
        case 54:
            BlockHoney.showJumpParticles(this);
            break;
        case 55:
            this.swapHandItems();
            break;
        case 60:
            this.makePoofParticles();
        }

    }

    private void makePoofParticles() {
        for(int i = 0; i < 20; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(Particles.POOF, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d, e, f);
        }

    }

    private void swapHandItems() {
        ItemStack itemStack = this.getEquipment(EnumItemSlot.OFFHAND);
        this.setSlot(EnumItemSlot.OFFHAND, this.getEquipment(EnumItemSlot.MAINHAND));
        this.setSlot(EnumItemSlot.MAINHAND, itemStack);
    }

    @Override
    protected void outOfWorld() {
        this.damageEntity(DamageSource.OUT_OF_WORLD, 4.0F);
    }

    protected void updateSwingTime() {
        int i = this.getCurrentSwingDuration();
        if (this.swinging) {
            ++this.swingTime;
            if (this.swingTime >= i) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float)this.swingTime / (float)i;
    }

    @Nullable
    public AttributeModifiable getAttributeInstance(AttributeBase attribute) {
        return this.getAttributeMap().getInstance(attribute);
    }

    public double getAttributeValue(AttributeBase attribute) {
        return this.getAttributeMap().getValue(attribute);
    }

    public double getAttributeBaseValue(AttributeBase attribute) {
        return this.getAttributeMap().getBaseValue(attribute);
    }

    public AttributeMapBase getAttributeMap() {
        return this.attributes;
    }

    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEFINED;
    }

    public ItemStack getItemInMainHand() {
        return this.getEquipment(EnumItemSlot.MAINHAND);
    }

    public ItemStack getItemInOffHand() {
        return this.getEquipment(EnumItemSlot.OFFHAND);
    }

    public boolean isHolding(Item item) {
        return this.isHolding((stack) -> {
            return stack.is(item);
        });
    }

    public boolean isHolding(Predicate<ItemStack> predicate) {
        return predicate.test(this.getItemInMainHand()) || predicate.test(this.getItemInOffHand());
    }

    public ItemStack getItemInHand(EnumHand hand) {
        if (hand == EnumHand.MAIN_HAND) {
            return this.getEquipment(EnumItemSlot.MAINHAND);
        } else if (hand == EnumHand.OFF_HAND) {
            return this.getEquipment(EnumItemSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + hand);
        }
    }

    public void setItemInHand(EnumHand hand, ItemStack stack) {
        if (hand == EnumHand.MAIN_HAND) {
            this.setSlot(EnumItemSlot.MAINHAND, stack);
        } else {
            if (hand != EnumHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + hand);
            }

            this.setSlot(EnumItemSlot.OFFHAND, stack);
        }

    }

    public boolean hasItemInSlot(EnumItemSlot slot) {
        return !this.getEquipment(slot).isEmpty();
    }

    @Override
    public abstract Iterable<ItemStack> getArmorItems();

    public abstract ItemStack getEquipment(EnumItemSlot slot);

    @Override
    public abstract void setSlot(EnumItemSlot slot, ItemStack stack);

    protected void verifyEquippedItem(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null) {
            stack.getItem().verifyTagAfterLoad(compoundTag);
        }

    }

    public float getArmorCoverPercentage() {
        Iterable<ItemStack> iterable = this.getArmorItems();
        int i = 0;
        int j = 0;

        for(ItemStack itemStack : iterable) {
            if (!itemStack.isEmpty()) {
                ++j;
            }

            ++i;
        }

        return i > 0 ? (float)j / (float)i : 0.0F;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
        if (attributeInstance.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) {
            attributeInstance.removeModifier(SPEED_MODIFIER_SPRINTING);
        }

        if (sprinting) {
            attributeInstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }

    }

    public float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isFrozen() {
        return this.isDeadOrDying();
    }

    @Override
    public void collide(Entity entity) {
        if (!this.isSleeping()) {
            super.collide(entity);
        }

    }

    private void dismountVehicle(Entity vehicle) {
        Vec3D vec3;
        if (this.isRemoved()) {
            vec3 = this.getPositionVector();
        } else if (!vehicle.isRemoved() && !this.level.getType(vehicle.getChunkCoordinates()).is(TagsBlock.PORTALS)) {
            vec3 = vehicle.getDismountLocationForPassenger(this);
        } else {
            double d = Math.max(this.locY(), vehicle.locY());
            vec3 = new Vec3D(this.locX(), d, this.locZ());
        }

        this.dismountTo(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.getCustomNameVisible();
    }

    protected float getJumpPower() {
        return 0.42F * this.getBlockJumpFactor();
    }

    public double getJumpBoostPower() {
        return this.hasEffect(MobEffectList.JUMP) ? (double)(0.1F * (float)(this.getEffect(MobEffectList.JUMP).getAmplifier() + 1)) : 0.0D;
    }

    protected void jump() {
        double d = (double)this.getJumpPower() + this.getJumpBoostPower();
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x, d, vec3.z);
        if (this.isSprinting()) {
            float f = this.getYRot() * ((float)Math.PI / 180F);
            this.setMot(this.getMot().add((double)(-MathHelper.sin(f) * 0.2F), 0.0D, (double)(MathHelper.cos(f) * 0.2F)));
        }

        this.hasImpulse = true;
    }

    protected void goDownInWater() {
        this.setMot(this.getMot().add(0.0D, (double)-0.04F, 0.0D));
    }

    protected void jumpInLiquid(Tag<FluidType> fluid) {
        this.setMot(this.getMot().add(0.0D, (double)0.04F, 0.0D));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidType fluid) {
        return false;
    }

    public void travel(Vec3D movementInput) {
        if (this.doAITick() || this.isControlledByLocalInstance()) {
            double d = 0.08D;
            boolean bl = this.getMot().y <= 0.0D;
            if (bl && this.hasEffect(MobEffectList.SLOW_FALLING)) {
                d = 0.01D;
                this.fallDistance = 0.0F;
            }

            Fluid fluidState = this.level.getFluid(this.getChunkCoordinates());
            if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState.getType())) {
                double e = this.locY();
                float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
                float g = 0.02F;
                float h = (float)EnchantmentManager.getDepthStrider(this);
                if (h > 3.0F) {
                    h = 3.0F;
                }

                if (!this.onGround) {
                    h *= 0.5F;
                }

                if (h > 0.0F) {
                    f += (0.54600006F - f) * h / 3.0F;
                    g += (this.getSpeed() - g) * h / 3.0F;
                }

                if (this.hasEffect(MobEffectList.DOLPHINS_GRACE)) {
                    f = 0.96F;
                }

                this.moveRelative(g, movementInput);
                this.move(EnumMoveType.SELF, this.getMot());
                Vec3D vec3 = this.getMot();
                if (this.horizontalCollision && this.isCurrentlyClimbing()) {
                    vec3 = new Vec3D(vec3.x, 0.2D, vec3.z);
                }

                this.setMot(vec3.multiply((double)f, (double)0.8F, (double)f));
                Vec3D vec32 = this.getFluidFallingAdjustedMovement(d, bl, this.getMot());
                this.setMot(vec32);
                if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + (double)0.6F - this.locY() + e, vec32.z)) {
                    this.setMot(vec32.x, (double)0.3F, vec32.z);
                }
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState.getType())) {
                double i = this.locY();
                this.moveRelative(0.02F, movementInput);
                this.move(EnumMoveType.SELF, this.getMot());
                if (this.getFluidHeight(TagsFluid.LAVA) <= this.getFluidJumpThreshold()) {
                    this.setMot(this.getMot().multiply(0.5D, (double)0.8F, 0.5D));
                    Vec3D vec33 = this.getFluidFallingAdjustedMovement(d, bl, this.getMot());
                    this.setMot(vec33);
                } else {
                    this.setMot(this.getMot().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    this.setMot(this.getMot().add(0.0D, -d / 4.0D, 0.0D));
                }

                Vec3D vec34 = this.getMot();
                if (this.horizontalCollision && this.isFree(vec34.x, vec34.y + (double)0.6F - this.locY() + i, vec34.z)) {
                    this.setMot(vec34.x, (double)0.3F, vec34.z);
                }
            } else if (this.isGliding()) {
                Vec3D vec35 = this.getMot();
                if (vec35.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                Vec3D vec36 = this.getLookDirection();
                float j = this.getXRot() * ((float)Math.PI / 180F);
                double k = Math.sqrt(vec36.x * vec36.x + vec36.z * vec36.z);
                double l = vec35.horizontalDistance();
                double m = vec36.length();
                float n = MathHelper.cos(j);
                n = (float)((double)n * (double)n * Math.min(1.0D, m / 0.4D));
                vec35 = this.getMot().add(0.0D, d * (-1.0D + (double)n * 0.75D), 0.0D);
                if (vec35.y < 0.0D && k > 0.0D) {
                    double o = vec35.y * -0.1D * (double)n;
                    vec35 = vec35.add(vec36.x * o / k, o, vec36.z * o / k);
                }

                if (j < 0.0F && k > 0.0D) {
                    double p = l * (double)(-MathHelper.sin(j)) * 0.04D;
                    vec35 = vec35.add(-vec36.x * p / k, p * 3.2D, -vec36.z * p / k);
                }

                if (k > 0.0D) {
                    vec35 = vec35.add((vec36.x / k * l - vec35.x) * 0.1D, 0.0D, (vec36.z / k * l - vec35.z) * 0.1D);
                }

                this.setMot(vec35.multiply((double)0.99F, (double)0.98F, (double)0.99F));
                this.move(EnumMoveType.SELF, this.getMot());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    double q = this.getMot().horizontalDistance();
                    double r = l - q;
                    float s = (float)(r * 10.0D - 3.0D);
                    if (s > 0.0F) {
                        this.playSound(this.getSoundFall((int)s), 1.0F, 1.0F);
                        this.damageEntity(DamageSource.FLY_INTO_WALL, s);
                    }
                }

                if (this.onGround && !this.level.isClientSide) {
                    this.setFlag(7, false);
                }
            } else {
                BlockPosition blockPos = this.getBlockPosBelowThatAffectsMyMovement();
                float t = this.level.getType(blockPos).getBlock().getFrictionFactor();
                float u = this.onGround ? t * 0.91F : 0.91F;
                Vec3D vec37 = this.handleRelativeFrictionAndCalculateMovement(movementInput, t);
                double v = vec37.y;
                if (this.hasEffect(MobEffectList.LEVITATION)) {
                    v += (0.05D * (double)(this.getEffect(MobEffectList.LEVITATION).getAmplifier() + 1) - vec37.y) * 0.2D;
                    this.fallDistance = 0.0F;
                } else if (this.level.isClientSide && !this.level.isLoaded(blockPos)) {
                    if (this.locY() > (double)this.level.getMinBuildHeight()) {
                        v = -0.1D;
                    } else {
                        v = 0.0D;
                    }
                } else if (!this.isNoGravity()) {
                    v -= d;
                }

                if (this.shouldDiscardFriction()) {
                    this.setMot(vec37.x, v, vec37.z);
                } else {
                    this.setMot(vec37.x * (double)u, v * (double)0.98F, vec37.z * (double)u);
                }
            }
        }

        this.calculateEntityAnimation(this, this instanceof EntityBird);
    }

    public void calculateEntityAnimation(EntityLiving entity, boolean flutter) {
        entity.animationSpeedOld = entity.animationSpeed;
        double d = entity.locX() - entity.xo;
        double e = flutter ? entity.locY() - entity.yo : 0.0D;
        double f = entity.locZ() - entity.zo;
        float g = (float)Math.sqrt(d * d + e * e + f * f) * 4.0F;
        if (g > 1.0F) {
            g = 1.0F;
        }

        entity.animationSpeed += (g - entity.animationSpeed) * 0.4F;
        entity.animationPosition += entity.animationSpeed;
    }

    public Vec3D handleRelativeFrictionAndCalculateMovement(Vec3D vec3, float f) {
        this.moveRelative(this.getFrictionInfluencedSpeed(f), vec3);
        this.setMot(this.handleOnClimbable(this.getMot()));
        this.move(EnumMoveType.SELF, this.getMot());
        Vec3D vec32 = this.getMot();
        if ((this.horizontalCollision || this.jumping) && (this.isCurrentlyClimbing() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && BlockPowderSnow.canEntityWalkOnPowderSnow(this))) {
            vec32 = new Vec3D(vec32.x, 0.2D, vec32.z);
        }

        return vec32;
    }

    public Vec3D getFluidFallingAdjustedMovement(double d, boolean bl, Vec3D vec3) {
        if (!this.isNoGravity() && !this.isSprinting()) {
            double e;
            if (bl && Math.abs(vec3.y - 0.005D) >= 0.003D && Math.abs(vec3.y - d / 16.0D) < 0.003D) {
                e = -0.003D;
            } else {
                e = vec3.y - d / 16.0D;
            }

            return new Vec3D(vec3.x, e, vec3.z);
        } else {
            return vec3;
        }
    }

    private Vec3D handleOnClimbable(Vec3D motion) {
        if (this.isCurrentlyClimbing()) {
            this.fallDistance = 0.0F;
            float f = 0.15F;
            double d = MathHelper.clamp(motion.x, (double)-0.15F, (double)0.15F);
            double e = MathHelper.clamp(motion.z, (double)-0.15F, (double)0.15F);
            double g = Math.max(motion.y, (double)-0.15F);
            if (g < 0.0D && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof EntityHuman) {
                g = 0.0D;
            }

            motion = new Vec3D(d, g, e);
        }

        return motion;
    }

    private float getFrictionInfluencedSpeed(float slipperiness) {
        return this.onGround ? this.getSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.flyingSpeed;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float movementSpeed) {
        this.speed = movementSpeed;
    }

    public boolean attackEntity(Entity target) {
        this.setLastHurtMob(target);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level.isClientSide) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.updateEquipment();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && !this.checkBedExists()) {
                this.entityWakeup();
            }
        }

        this.movementTick();
        double d = this.locX() - this.xo;
        double e = this.locZ() - this.zo;
        float f = (float)(d * d + e * e);
        float g = this.yBodyRot;
        float h = 0.0F;
        this.oRun = this.run;
        float k = 0.0F;
        if (f > 0.0025000002F) {
            k = 1.0F;
            h = (float)Math.sqrt((double)f) * 3.0F;
            float l = (float)MathHelper.atan2(e, d) * (180F / (float)Math.PI) - 90.0F;
            float m = MathHelper.abs(MathHelper.wrapDegrees(this.getYRot()) - l);
            if (95.0F < m && m < 265.0F) {
                g = l - 180.0F;
            } else {
                g = l;
            }
        }

        if (this.attackAnim > 0.0F) {
            g = this.getYRot();
        }

        if (!this.onGround) {
            k = 0.0F;
        }

        this.run += (k - this.run) * 0.3F;
        this.level.getMethodProfiler().enter("headTurn");
        h = this.tickHeadTurn(g, h);
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("rangeChecks");

        while(this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while(this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while(this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while(this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while(this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while(this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while(this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while(this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        this.level.getMethodProfiler().exit();
        this.animStep += h;
        if (this.isGliding()) {
            ++this.fallFlyTicks;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

    }

    public void updateEquipment() {
        Map<EnumItemSlot, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }

    }

    @Nullable
    private Map<EnumItemSlot, ItemStack> collectEquipmentChanges() {
        Map<EnumItemSlot, ItemStack> map = null;

        for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
            ItemStack itemStack;
            switch(equipmentSlot.getType()) {
            case HAND:
                itemStack = this.getLastHandItem(equipmentSlot);
                break;
            case ARMOR:
                itemStack = this.getLastArmorItem(equipmentSlot);
                break;
            default:
                continue;
            }

            ItemStack itemStack4 = this.getEquipment(equipmentSlot);
            if (!ItemStack.matches(itemStack4, itemStack)) {
                if (map == null) {
                    map = Maps.newEnumMap(EnumItemSlot.class);
                }

                map.put(equipmentSlot, itemStack4);
                if (!itemStack.isEmpty()) {
                    this.getAttributeMap().removeAttributeModifiers(itemStack.getAttributeModifiers(equipmentSlot));
                }

                if (!itemStack4.isEmpty()) {
                    this.getAttributeMap().addTransientAttributeModifiers(itemStack4.getAttributeModifiers(equipmentSlot));
                }
            }
        }

        return map;
    }

    private void handleHandSwap(Map<EnumItemSlot, ItemStack> equipment) {
        ItemStack itemStack = equipment.get(EnumItemSlot.MAINHAND);
        ItemStack itemStack2 = equipment.get(EnumItemSlot.OFFHAND);
        if (itemStack != null && itemStack2 != null && ItemStack.matches(itemStack, this.getLastHandItem(EnumItemSlot.OFFHAND)) && ItemStack.matches(itemStack2, this.getLastHandItem(EnumItemSlot.MAINHAND))) {
            ((WorldServer)this.level).getChunkSource().broadcast(this, new PacketPlayOutEntityStatus(this, (byte)55));
            equipment.remove(EnumItemSlot.MAINHAND);
            equipment.remove(EnumItemSlot.OFFHAND);
            this.setLastHandItem(EnumItemSlot.MAINHAND, itemStack.cloneItemStack());
            this.setLastHandItem(EnumItemSlot.OFFHAND, itemStack2.cloneItemStack());
        }

    }

    private void handleEquipmentChanges(Map<EnumItemSlot, ItemStack> equipment) {
        List<Pair<EnumItemSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipment.size());
        equipment.forEach((slot, stack) -> {
            ItemStack itemStack = stack.cloneItemStack();
            list.add(Pair.of(slot, itemStack));
            switch(slot.getType()) {
            case HAND:
                this.setLastHandItem(slot, itemStack);
                break;
            case ARMOR:
                this.setLastArmorItem(slot, itemStack);
            }

        });
        ((WorldServer)this.level).getChunkSource().broadcast(this, new PacketPlayOutEntityEquipment(this.getId(), list));
    }

    private ItemStack getLastArmorItem(EnumItemSlot slot) {
        return this.lastArmorItemStacks.get(slot.getIndex());
    }

    private void setLastArmorItem(EnumItemSlot slot, ItemStack armor) {
        this.lastArmorItemStacks.set(slot.getIndex(), armor);
    }

    private ItemStack getLastHandItem(EnumItemSlot slot) {
        return this.lastHandItemStacks.get(slot.getIndex());
    }

    private void setLastHandItem(EnumItemSlot slot, ItemStack stack) {
        this.lastHandItemStacks.set(slot.getIndex(), stack);
    }

    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        float f = MathHelper.wrapDegrees(bodyRotation - this.yBodyRot);
        this.yBodyRot += f * 0.3F;
        float g = MathHelper.wrapDegrees(this.getYRot() - this.yBodyRot);
        boolean bl = g < -90.0F || g >= 90.0F;
        if (g < -75.0F) {
            g = -75.0F;
        }

        if (g >= 75.0F) {
            g = 75.0F;
        }

        this.yBodyRot = this.getYRot() - g;
        if (g * g > 2500.0F) {
            this.yBodyRot += g * 0.2F;
        }

        if (bl) {
            headRotation *= -1.0F;
        }

        return headRotation;
    }

    public void movementTick() {
        if (this.noJumpDelay > 0) {
            --this.noJumpDelay;
        }

        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.setPacketCoordinates(this.locX(), this.locY(), this.locZ());
        }

        if (this.lerpSteps > 0) {
            double d = this.locX() + (this.lerpX - this.locX()) / (double)this.lerpSteps;
            double e = this.locY() + (this.lerpY - this.locY()) / (double)this.lerpSteps;
            double f = this.locZ() + (this.lerpZ - this.locZ()) / (double)this.lerpSteps;
            double g = MathHelper.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)g / (float)this.lerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
            --this.lerpSteps;
            this.setPosition(d, e, f);
            this.setYawPitch(this.getYRot(), this.getXRot());
        } else if (!this.doAITick()) {
            this.setMot(this.getMot().scale(0.98D));
        }

        if (this.lerpHeadSteps > 0) {
            this.yHeadRot = (float)((double)this.yHeadRot + MathHelper.wrapDegrees(this.lyHeadRot - (double)this.yHeadRot) / (double)this.lerpHeadSteps);
            --this.lerpHeadSteps;
        }

        Vec3D vec3 = this.getMot();
        double h = vec3.x;
        double i = vec3.y;
        double j = vec3.z;
        if (Math.abs(vec3.x) < 0.003D) {
            h = 0.0D;
        }

        if (Math.abs(vec3.y) < 0.003D) {
            i = 0.0D;
        }

        if (Math.abs(vec3.z) < 0.003D) {
            j = 0.0D;
        }

        this.setMot(h, i, j);
        this.level.getMethodProfiler().enter("ai");
        if (this.isFrozen()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.doAITick()) {
            this.level.getMethodProfiler().enter("newAi");
            this.doTick();
            this.level.getMethodProfiler().exit();
        }

        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double k;
            if (this.isInLava()) {
                k = this.getFluidHeight(TagsFluid.LAVA);
            } else {
                k = this.getFluidHeight(TagsFluid.WATER);
            }

            boolean bl = this.isInWater() && k > 0.0D;
            double m = this.getFluidJumpThreshold();
            if (!bl || this.onGround && !(k > m)) {
                if (!this.isInLava() || this.onGround && !(k > m)) {
                    if ((this.onGround || bl && k <= m) && this.noJumpDelay == 0) {
                        this.jump();
                        this.noJumpDelay = 10;
                    }
                } else {
                    this.jumpInLiquid(TagsFluid.LAVA);
                }
            } else {
                this.jumpInLiquid(TagsFluid.WATER);
            }
        } else {
            this.noJumpDelay = 0;
        }

        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("travel");
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
        this.updateFallFlying();
        AxisAlignedBB aABB = this.getBoundingBox();
        this.travel(new Vec3D((double)this.xxa, (double)this.yya, (double)this.zza));
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("freezing");
        boolean bl2 = this.getEntityType().is(TagsEntity.FREEZE_HURTS_EXTRA_TYPES);
        if (!this.level.isClientSide && !this.isDeadOrDying()) {
            int n = this.getTicksFrozen();
            if (this.isInPowderSnow && this.canFreeze()) {
                this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), n + 1));
            } else {
                this.setTicksFrozen(Math.max(0, n - 2));
            }
        }

        this.removeFrost();
        this.tryAddFrost();
        if (!this.level.isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            int o = bl2 ? 5 : 1;
            this.damageEntity(DamageSource.FREEZE, (float)o);
        }

        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("push");
        if (this.autoSpinAttackTicks > 0) {
            --this.autoSpinAttackTicks;
            this.checkAutoSpinAttack(aABB, this.getBoundingBox());
        }

        this.collideNearby();
        this.level.getMethodProfiler().exit();
        if (!this.level.isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.damageEntity(DamageSource.DROWN, 1.0F);
        }

    }

    public boolean isSensitiveToWater() {
        return false;
    }

    private void updateFallFlying() {
        boolean bl = this.getFlag(7);
        if (bl && !this.onGround && !this.isPassenger() && !this.hasEffect(MobEffectList.LEVITATION)) {
            ItemStack itemStack = this.getEquipment(EnumItemSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && ItemElytra.isFlyEnabled(itemStack)) {
                bl = true;
                int i = this.fallFlyTicks + 1;
                if (!this.level.isClientSide && i % 10 == 0) {
                    int j = i / 10;
                    if (j % 2 == 0) {
                        itemStack.damage(1, this, (player) -> {
                            player.broadcastItemBreak(EnumItemSlot.CHEST);
                        });
                    }

                    this.gameEvent(GameEvent.ELYTRA_FREE_FALL);
                }
            } else {
                bl = false;
            }
        } else {
            bl = false;
        }

        if (!this.level.isClientSide) {
            this.setFlag(7, bl);
        }

    }

    protected void doTick() {
    }

    protected void collideNearby() {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox(), IEntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            int i = this.level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                int j = 0;

                for(int k = 0; k < list.size(); ++k) {
                    if (!list.get(k).isPassenger()) {
                        ++j;
                    }
                }

                if (j > i - 1) {
                    this.damageEntity(DamageSource.CRAMMING, 6.0F);
                }
            }

            for(int l = 0; l < list.size(); ++l) {
                Entity entity = list.get(l);
                this.doPush(entity);
            }
        }

    }

    protected void checkAutoSpinAttack(AxisAlignedBB a, AxisAlignedBB b) {
        AxisAlignedBB aABB = a.minmax(b);
        List<Entity> list = this.level.getEntities(this, aABB);
        if (!list.isEmpty()) {
            for(int i = 0; i < list.size(); ++i) {
                Entity entity = list.get(i);
                if (entity instanceof EntityLiving) {
                    this.doAutoAttackOnTouch((EntityLiving)entity);
                    this.autoSpinAttackTicks = 0;
                    this.setMot(this.getMot().scale(-0.2D));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level.isClientSide && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
        }

    }

    protected void doPush(Entity entity) {
        entity.collide(this);
    }

    protected void doAutoAttackOnTouch(EntityLiving target) {
    }

    public void startAutoSpinAttack(int riptideTicks) {
        this.autoSpinAttackTicks = riptideTicks;
        if (!this.level.isClientSide) {
            this.setLivingEntityFlag(4, true);
        }

    }

    public boolean isRiptiding() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity != null && entity != this.getVehicle() && !this.level.isClientSide) {
            this.dismountVehicle(entity);
        }

    }

    @Override
    public void passengerTick() {
        super.passengerTick();
        this.oRun = this.run;
        this.run = 0.0F;
        this.fallDistance = 0.0F;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = (double)yaw;
        this.lerpXRot = (double)pitch;
        this.lerpSteps = interpolationSteps;
    }

    @Override
    public void lerpHeadTo(float yaw, int interpolationSteps) {
        this.lyHeadRot = (double)yaw;
        this.lerpHeadSteps = interpolationSteps;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    public void onItemPickup(EntityItem item) {
        EntityHuman player = item.getThrower() != null ? this.level.getPlayerByUUID(item.getThrower()) : null;
        if (player instanceof EntityPlayer) {
            CriterionTriggers.ITEM_PICKED_UP_BY_ENTITY.trigger((EntityPlayer)player, item.getItemStack(), this);
        }

    }

    public void receive(Entity item, int count) {
        if (!item.isRemoved() && !this.level.isClientSide && (item instanceof EntityItem || item instanceof EntityArrow || item instanceof EntityExperienceOrb)) {
            ((WorldServer)this.level).getChunkSource().broadcast(item, new PacketPlayOutCollect(item.getId(), this.getId(), count));
        }

    }

    public boolean hasLineOfSight(Entity entity) {
        if (entity.level != this.level) {
            return false;
        } else {
            Vec3D vec3 = new Vec3D(this.locX(), this.getHeadY(), this.locZ());
            Vec3D vec32 = new Vec3D(entity.locX(), entity.getHeadY(), entity.locZ());
            if (vec32.distanceTo(vec3) > 128.0D) {
                return false;
            } else {
                return this.level.rayTrace(new RayTrace(vec3, vec32, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this)).getType() == MovingObjectPosition.EnumMovingObjectType.MISS;
            }
        }
    }

    @Override
    public float getViewYRot(float tickDelta) {
        return tickDelta == 1.0F ? this.yHeadRot : MathHelper.lerp(tickDelta, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float tickDelta) {
        float f = this.attackAnim - this.oAttackAnim;
        if (f < 0.0F) {
            ++f;
        }

        return this.oAttackAnim + f * tickDelta;
    }

    public boolean doAITick() {
        return !this.level.isClientSide;
    }

    @Override
    public boolean isInteractable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return this.isAlive() && !this.isSpectator() && !this.isCurrentlyClimbing();
    }

    @Override
    protected void velocityChanged() {
        this.hurtMarked = this.random.nextDouble() >= this.getAttributeValue(GenericAttributes.KNOCKBACK_RESISTANCE);
    }

    @Override
    public float getHeadRotation() {
        return this.yHeadRot;
    }

    @Override
    public void setHeadRotation(float headYaw) {
        this.yHeadRot = headYaw;
    }

    @Override
    public void setYBodyRot(float bodyYaw) {
        this.yBodyRot = bodyYaw;
    }

    @Override
    protected Vec3D getRelativePortalPosition(EnumDirection.EnumAxis portalAxis, BlockUtil.Rectangle portalRect) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(portalAxis, portalRect));
    }

    public static Vec3D resetForwardDirectionOfRelativePortalPosition(Vec3D pos) {
        return new Vec3D(pos.x, pos.y, 0.0D);
    }

    public float getAbsorptionHearts() {
        return this.absorptionAmount;
    }

    public void setAbsorptionHearts(float amount) {
        if (amount < 0.0F) {
            amount = 0.0F;
        }

        this.absorptionAmount = amount;
    }

    public void enterCombat() {
    }

    public void exitCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract EnumMainHand getMainHand();

    public boolean isHandRaised() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public EnumHand getRaisedHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isHandRaised()) {
            if (ItemStack.isSameIgnoreDurability(this.getItemInHand(this.getRaisedHand()), this.useItem)) {
                this.useItem = this.getItemInHand(this.getRaisedHand());
                this.updateUsingItem(this.useItem);
            } else {
                this.clearActiveItem();
            }
        }

    }

    protected void updateUsingItem(ItemStack stack) {
        stack.onUseTick(this.level, this, this.getUseItemRemainingTicks());
        if (this.shouldTriggerItemUseEffects()) {
            this.triggerItemUseEffects(stack, 5);
        }

        if (--this.useItemRemaining == 0 && !this.level.isClientSide && !stack.useOnRelease()) {
            this.completeUsingItem();
        }

    }

    private boolean shouldTriggerItemUseEffects() {
        int i = this.getUseItemRemainingTicks();
        FoodInfo foodProperties = this.useItem.getItem().getFoodInfo();
        boolean bl = foodProperties != null && foodProperties.isFastFood();
        bl = bl | i <= this.useItem.getUseDuration() - 7;
        return bl && i % 4 == 0;
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }

    }

    protected void setLivingEntityFlag(int mask, boolean value) {
        int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
        if (value) {
            i = i | mask;
        } else {
            i = i & ~mask;
        }

        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
    }

    public void startUsingItem(EnumHand hand) {
        ItemStack itemStack = this.getItemInHand(hand);
        if (!itemStack.isEmpty() && !this.isHandRaised()) {
            this.useItem = itemStack;
            this.useItemRemaining = itemStack.getUseDuration();
            if (!this.level.isClientSide) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, hand == EnumHand.OFF_HAND);
            }

        }
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        super.onSyncedDataUpdated(data);
        if (SLEEPING_POS_ID.equals(data)) {
            if (this.level.isClientSide) {
                this.getBedPosition().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(data) && this.level.isClientSide) {
            if (this.isHandRaised() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getRaisedHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration();
                }
            } else if (!this.isHandRaised() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }

    }

    @Override
    public void lookAt(ArgumentAnchor.Anchor anchorPoint, Vec3D target) {
        super.lookAt(anchorPoint, target);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    protected void triggerItemUseEffects(ItemStack stack, int particleCount) {
        if (!stack.isEmpty() && this.isHandRaised()) {
            if (stack.getUseAnimation() == EnumAnimation.DRINK) {
                this.playSound(this.getDrinkingSound(stack), 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }

            if (stack.getUseAnimation() == EnumAnimation.EAT) {
                this.spawnItemParticles(stack, particleCount);
                this.playSound(this.getEatingSound(stack), 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

        }
    }

    private void spawnItemParticles(ItemStack stack, int count) {
        for(int i = 0; i < count; ++i) {
            Vec3D vec3 = new Vec3D(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3 = vec3.xRot(-this.getXRot() * ((float)Math.PI / 180F));
            vec3 = vec3.yRot(-this.getYRot() * ((float)Math.PI / 180F));
            double d = (double)(-this.random.nextFloat()) * 0.6D - 0.3D;
            Vec3D vec32 = new Vec3D(((double)this.random.nextFloat() - 0.5D) * 0.3D, d, 0.6D);
            vec32 = vec32.xRot(-this.getXRot() * ((float)Math.PI / 180F));
            vec32 = vec32.yRot(-this.getYRot() * ((float)Math.PI / 180F));
            vec32 = vec32.add(this.locX(), this.getHeadY(), this.locZ());
            this.level.addParticle(new ParticleParamItem(Particles.ITEM, stack), vec32.x, vec32.y, vec32.z, vec3.x, vec3.y + 0.05D, vec3.z);
        }

    }

    protected void completeUsingItem() {
        EnumHand interactionHand = this.getRaisedHand();
        if (!this.useItem.equals(this.getItemInHand(interactionHand))) {
            this.releaseActiveItem();
        } else {
            if (!this.useItem.isEmpty() && this.isHandRaised()) {
                this.triggerItemUseEffects(this.useItem, 16);
                ItemStack itemStack = this.useItem.finishUsingItem(this.level, this);
                if (itemStack != this.useItem) {
                    this.setItemInHand(interactionHand, itemStack);
                }

                this.clearActiveItem();
            }

        }
    }

    public ItemStack getActiveItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isHandRaised() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
    }

    public void releaseActiveItem() {
        if (!this.useItem.isEmpty()) {
            this.useItem.releaseUsing(this.level, this, this.getUseItemRemainingTicks());
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.clearActiveItem();
    }

    public void clearActiveItem() {
        if (!this.level.isClientSide) {
            this.setLivingEntityFlag(1, false);
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        if (this.isHandRaised() && !this.useItem.isEmpty()) {
            Item item = this.useItem.getItem();
            if (item.getUseAnimation(this.useItem) != EnumAnimation.BLOCK) {
                return false;
            } else {
                return item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
            }
        } else {
            return false;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isSneaking();
    }

    public boolean isGliding() {
        return this.getFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isGliding() && this.getPose() == EntityPose.FALL_FLYING;
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double x, double y, double z, boolean particleEffects) {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        double g = y;
        boolean bl = false;
        BlockPosition blockPos = new BlockPosition(x, y, z);
        World level = this.level;
        if (level.isLoaded(blockPos)) {
            boolean bl2 = false;

            while(!bl2 && blockPos.getY() > level.getMinBuildHeight()) {
                BlockPosition blockPos2 = blockPos.below();
                IBlockData blockState = level.getType(blockPos2);
                if (blockState.getMaterial().isSolid()) {
                    bl2 = true;
                } else {
                    --g;
                    blockPos = blockPos2;
                }
            }

            if (bl2) {
                this.enderTeleportTo(x, g, z);
                if (level.getCubes(this) && !level.containsLiquid(this.getBoundingBox())) {
                    bl = true;
                }
            }
        }

        if (!bl) {
            this.enderTeleportTo(d, e, f);
            return false;
        } else {
            if (particleEffects) {
                level.broadcastEntityEffect(this, (byte)46);
            }

            if (this instanceof EntityCreature) {
                ((EntityCreature)this).getNavigation().stop();
            }

            return true;
        }
    }

    public boolean isAffectedByPotions() {
        return true;
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPosition songPosition, boolean playing) {
    }

    public boolean canTakeItem(ItemStack stack) {
        return false;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntityLiving(this);
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return pose == EntityPose.SLEEPING ? SLEEPING_DIMENSIONS : super.getDimensions(pose).scale(this.getScale());
    }

    public ImmutableList<EntityPose> getDismountPoses() {
        return ImmutableList.of(EntityPose.STANDING);
    }

    public AxisAlignedBB getLocalBoundsForPose(EntityPose pose) {
        EntitySize entityDimensions = this.getDimensions(pose);
        return new AxisAlignedBB((double)(-entityDimensions.width / 2.0F), 0.0D, (double)(-entityDimensions.width / 2.0F), (double)(entityDimensions.width / 2.0F), (double)entityDimensions.height, (double)(entityDimensions.width / 2.0F));
    }

    public Optional<BlockPosition> getBedPosition() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPosition pos) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(pos));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getBedPosition().isPresent();
    }

    public void entitySleep(BlockPosition pos) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        IBlockData blockState = this.level.getType(pos);
        if (blockState.getBlock() instanceof BlockBed) {
            this.level.setTypeAndData(pos, blockState.set(BlockBed.OCCUPIED, Boolean.valueOf(true)), 3);
        }

        this.setPose(EntityPose.SLEEPING);
        this.setPosToBed(pos);
        this.setSleepingPos(pos);
        this.setMot(Vec3D.ZERO);
        this.hasImpulse = true;
    }

    private void setPosToBed(BlockPosition pos) {
        this.setPosition((double)pos.getX() + 0.5D, (double)pos.getY() + 0.6875D, (double)pos.getZ() + 0.5D);
    }

    private boolean checkBedExists() {
        return this.getBedPosition().map((pos) -> {
            return this.level.getType(pos).getBlock() instanceof BlockBed;
        }).orElse(false);
    }

    public void entityWakeup() {
        this.getBedPosition().filter(this.level::isLoaded).ifPresent((pos) -> {
            IBlockData blockState = this.level.getType(pos);
            if (blockState.getBlock() instanceof BlockBed) {
                this.level.setTypeAndData(pos, blockState.set(BlockBed.OCCUPIED, Boolean.valueOf(false)), 3);
                Vec3D vec3 = BlockBed.findStandUpPosition(this.getEntityType(), this.level, pos, this.getYRot()).orElseGet(() -> {
                    BlockPosition blockPos2 = pos.above();
                    return new Vec3D((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.1D, (double)blockPos2.getZ() + 0.5D);
                });
                Vec3D vec32 = Vec3D.atBottomCenterOf(pos).subtract(vec3).normalize();
                float f = (float)MathHelper.wrapDegrees(MathHelper.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI) - 90.0D);
                this.setPosition(vec3.x, vec3.y, vec3.z);
                this.setYRot(f);
                this.setXRot(0.0F);
            }

        });
        Vec3D vec3 = this.getPositionVector();
        this.setPose(EntityPose.STANDING);
        this.setPosition(vec3.x, vec3.y, vec3.z);
        this.clearSleepingPos();
    }

    @Nullable
    public EnumDirection getBedOrientation() {
        BlockPosition blockPos = this.getBedPosition().orElse((BlockPosition)null);
        return blockPos != null ? BlockBed.getBedOrientation(this.level, blockPos) : null;
    }

    @Override
    public boolean inBlock() {
        return !this.isSleeping() && super.inBlock();
    }

    @Override
    protected final float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return pose == EntityPose.SLEEPING ? 0.2F : this.getStandingEyeHeight(pose, dimensions);
    }

    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return super.getHeadHeight(pose, dimensions);
    }

    public ItemStack getProjectile(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public ItemStack eat(World world, ItemStack stack) {
        if (stack.isEdible()) {
            world.gameEvent(this, GameEvent.EAT, this.eyeBlockPosition());
            world.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), this.getEatingSound(stack), EnumSoundCategory.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            this.addEatEffect(stack, world, this);
            if (!(this instanceof EntityHuman) || !((EntityHuman)this).getAbilities().instabuild) {
                stack.subtract(1);
            }

            this.gameEvent(GameEvent.EAT);
        }

        return stack;
    }

    private void addEatEffect(ItemStack stack, World world, EntityLiving targetEntity) {
        Item item = stack.getItem();
        if (item.isFood()) {
            for(Pair<MobEffect, Float> pair : item.getFoodInfo().getEffects()) {
                if (!world.isClientSide && pair.getFirst() != null && world.random.nextFloat() < pair.getSecond()) {
                    targetEntity.addEffect(new MobEffect(pair.getFirst()));
                }
            }
        }

    }

    private static byte entityEventForEquipmentBreak(EnumItemSlot slot) {
        switch(slot) {
        case MAINHAND:
            return 47;
        case OFFHAND:
            return 48;
        case HEAD:
            return 49;
        case CHEST:
            return 50;
        case FEET:
            return 52;
        case LEGS:
            return 51;
        default:
            return 47;
        }
    }

    public void broadcastItemBreak(EnumItemSlot slot) {
        this.level.broadcastEntityEffect(this, entityEventForEquipmentBreak(slot));
    }

    public void broadcastItemBreak(EnumHand hand) {
        this.broadcastItemBreak(hand == EnumHand.MAIN_HAND ? EnumItemSlot.MAINHAND : EnumItemSlot.OFFHAND);
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        if (this.getEquipment(EnumItemSlot.HEAD).is(Items.DRAGON_HEAD)) {
            float f = 0.5F;
            return this.getBoundingBox().grow(0.5D, 0.5D, 0.5D);
        } else {
            return super.getBoundingBoxForCulling();
        }
    }

    public static EnumItemSlot getEquipmentSlotForItem(ItemStack stack) {
        Item item = stack.getItem();
        if (!stack.is(Items.CARVED_PUMPKIN) && (!(item instanceof ItemBlock) || !(((ItemBlock)item).getBlock() instanceof BlockSkullAbstract))) {
            if (item instanceof ItemArmor) {
                return ((ItemArmor)item).getSlot();
            } else if (stack.is(Items.ELYTRA)) {
                return EnumItemSlot.CHEST;
            } else {
                return stack.is(Items.SHIELD) ? EnumItemSlot.OFFHAND : EnumItemSlot.MAINHAND;
            }
        } else {
            return EnumItemSlot.HEAD;
        }
    }

    private static SlotAccess createEquipmentSlotAccess(EntityLiving entity, EnumItemSlot slot) {
        return slot != EnumItemSlot.HEAD && slot != EnumItemSlot.MAINHAND && slot != EnumItemSlot.OFFHAND ? SlotAccess.forEquipmentSlot(entity, slot, (stack) -> {
            return stack.isEmpty() || EntityInsentient.getEquipmentSlotForItem(stack) == slot;
        }) : SlotAccess.forEquipmentSlot(entity, slot);
    }

    @Nullable
    private static EnumItemSlot getEquipmentSlot(int slotId) {
        if (slotId == 100 + EnumItemSlot.HEAD.getIndex()) {
            return EnumItemSlot.HEAD;
        } else if (slotId == 100 + EnumItemSlot.CHEST.getIndex()) {
            return EnumItemSlot.CHEST;
        } else if (slotId == 100 + EnumItemSlot.LEGS.getIndex()) {
            return EnumItemSlot.LEGS;
        } else if (slotId == 100 + EnumItemSlot.FEET.getIndex()) {
            return EnumItemSlot.FEET;
        } else if (slotId == 98) {
            return EnumItemSlot.MAINHAND;
        } else {
            return slotId == 99 ? EnumItemSlot.OFFHAND : null;
        }
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        EnumItemSlot equipmentSlot = getEquipmentSlot(mappedIndex);
        return equipmentSlot != null ? createEquipmentSlotAccess(this, equipmentSlot) : super.getSlot(mappedIndex);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            boolean bl = !this.getEquipment(EnumItemSlot.HEAD).is(TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getEquipment(EnumItemSlot.CHEST).is(TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getEquipment(EnumItemSlot.LEGS).is(TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getEquipment(EnumItemSlot.FEET).is(TagsItem.FREEZE_IMMUNE_WEARABLES);
            return bl && super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level.isClientSide() && this.hasEffect(MobEffectList.GLOWING) || super.isCurrentlyGlowing();
    }

    public void recreateFromPacket(PacketPlayOutSpawnEntityLiving packet) {
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        float g = (float)(packet.getyRot() * 360) / 256.0F;
        float h = (float)(packet.getxRot() * 360) / 256.0F;
        this.setPacketCoordinates(d, e, f);
        this.yBodyRot = (float)(packet.getyHeadRot() * 360) / 256.0F;
        this.yHeadRot = (float)(packet.getyHeadRot() * 360) / 256.0F;
        this.setId(packet.getId());
        this.setUUID(packet.getUUID());
        this.setLocation(d, e, f, g, h);
        this.setMot((double)((float)packet.getXd() / 8000.0F), (double)((float)packet.getYd() / 8000.0F), (double)((float)packet.getZd() / 8000.0F));
    }
}
