package net.minecraft.world.entity.animal.horse;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.players.NameReferencingFileConverter;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.IInventoryListener;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IJumpable;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTame;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.DismountUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityHorseAbstract extends EntityAnimal implements IInventoryListener, IJumpable, ISaddleable {
    public static final int EQUIPMENT_SLOT_OFFSET = 400;
    public static final int CHEST_SLOT_OFFSET = 499;
    public static final int INVENTORY_SLOT_OFFSET = 500;
    private static final Predicate<EntityLiving> PARENT_HORSE_SELECTOR = (entity) -> {
        return entity instanceof EntityHorseAbstract && ((EntityHorseAbstract)entity).hasReproduced();
    };
    private static final PathfinderTargetCondition MOMMY_TARGETING = PathfinderTargetCondition.forNonCombat().range(16.0D).ignoreLineOfSight().selector(PARENT_HORSE_SELECTOR);
    private static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Items.WHEAT, Items.SUGAR, Blocks.HAY_BLOCK.getItem(), Items.APPLE, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
    private static final DataWatcherObject<Byte> DATA_ID_FLAGS = DataWatcher.defineId(EntityHorseAbstract.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Optional<UUID>> DATA_ID_OWNER_UUID = DataWatcher.defineId(EntityHorseAbstract.class, DataWatcherRegistry.OPTIONAL_UUID);
    private static final int FLAG_TAME = 2;
    private static final int FLAG_SADDLE = 4;
    private static final int FLAG_BRED = 8;
    private static final int FLAG_EATING = 16;
    private static final int FLAG_STANDING = 32;
    private static final int FLAG_OPEN_MOUTH = 64;
    public static final int INV_SLOT_SADDLE = 0;
    public static final int INV_SLOT_ARMOR = 1;
    public static final int INV_BASE_COUNT = 2;
    private int eatingCounter;
    private int mouthCounter;
    private int standCounter;
    public int tailCounter;
    public int sprintCounter;
    protected boolean isJumping;
    public InventorySubcontainer inventory;
    protected int temper;
    protected float playerJumpPendingScale;
    private boolean allowStandSliding;
    private float eatAnim;
    private float eatAnimO;
    private float standAnim;
    private float standAnimO;
    private float mouthAnim;
    private float mouthAnimO;
    protected boolean canGallop = true;
    protected int gallopSoundCounter;

    protected EntityHorseAbstract(EntityTypes<? extends EntityHorseAbstract> type, World world) {
        super(type, world);
        this.maxUpStep = 1.0F;
        this.loadChest();
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 1.2D));
        this.goalSelector.addGoal(1, new PathfinderGoalTame(this, 1.2D));
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D, EntityHorseAbstract.class));
        this.goalSelector.addGoal(4, new PathfinderGoalFollowParent(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomStrollLand(this, 0.7D));
        this.goalSelector.addGoal(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_FLAGS, (byte)0);
        this.entityData.register(DATA_ID_OWNER_UUID, Optional.empty());
    }

    protected boolean getFlag(int bitmask) {
        return (this.entityData.get(DATA_ID_FLAGS) & bitmask) != 0;
    }

    protected void setFlag(int bitmask, boolean flag) {
        byte b = this.entityData.get(DATA_ID_FLAGS);
        if (flag) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b | bitmask));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b & ~bitmask));
        }

    }

    public boolean isTamed() {
        return this.getFlag(2);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_ID_OWNER_UUID).orElse((UUID)null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public void setTamed(boolean tame) {
        this.setFlag(2, tame);
    }

    public void setIsJumping(boolean inAir) {
        this.isJumping = inAir;
    }

    @Override
    protected void onLeashDistance(float leashLength) {
        if (leashLength > 6.0F && this.isEating()) {
            this.setEating(false);
        }

    }

    public boolean isEating() {
        return this.getFlag(16);
    }

    public boolean isStanding() {
        return this.getFlag(32);
    }

    public boolean hasReproduced() {
        return this.getFlag(8);
    }

    public void setBred(boolean bred) {
        this.setFlag(8, bred);
    }

    @Override
    public boolean canSaddle() {
        return this.isAlive() && !this.isBaby() && this.isTamed();
    }

    @Override
    public void saddle(@Nullable EnumSoundCategory sound) {
        this.inventory.setItem(0, new ItemStack(Items.SADDLE));
        if (sound != null) {
            this.level.playSound((EntityHuman)null, this, SoundEffects.HORSE_SADDLE, sound, 0.5F, 1.0F);
        }

    }

    @Override
    public boolean hasSaddle() {
        return this.getFlag(4);
    }

    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int temper) {
        this.temper = temper;
    }

    public int modifyTemper(int difference) {
        int i = MathHelper.clamp(this.getTemper() + difference, 0, this.getMaxDomestication());
        this.setTemper(i);
        return i;
    }

    @Override
    public boolean isCollidable() {
        return !this.isVehicle();
    }

    private void eating() {
        this.openMouth();
        if (!this.isSilent()) {
            SoundEffect soundEvent = this.getEatingSound();
            if (soundEvent != null) {
                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), soundEvent, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance > 1.0F) {
            this.playSound(SoundEffects.HORSE_LAND, 0.4F, 1.0F);
        }

        int i = this.calculateFallDamage(fallDistance, damageMultiplier);
        if (i <= 0) {
            return false;
        } else {
            this.damageEntity(damageSource, (float)i);
            if (this.isVehicle()) {
                for(Entity entity : this.getAllPassengers()) {
                    entity.damageEntity(damageSource, (float)i);
                }
            }

            this.playBlockStepSound();
            return true;
        }
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return MathHelper.ceil((fallDistance * 0.5F - 3.0F) * damageMultiplier);
    }

    protected int getChestSlots() {
        return 2;
    }

    public void loadChest() {
        InventorySubcontainer simpleContainer = this.inventory;
        this.inventory = new InventorySubcontainer(this.getChestSlots());
        if (simpleContainer != null) {
            simpleContainer.removeListener(this);
            int i = Math.min(simpleContainer.getSize(), this.inventory.getSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemStack = simpleContainer.getItem(j);
                if (!itemStack.isEmpty()) {
                    this.inventory.setItem(j, itemStack.cloneItemStack());
                }
            }
        }

        this.inventory.addListener(this);
        this.updateContainerEquipment();
    }

    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }

    @Override
    public void containerChanged(IInventory sender) {
        boolean bl = this.hasSaddle();
        this.updateContainerEquipment();
        if (this.tickCount > 20 && !bl && this.hasSaddle()) {
            this.playSound(SoundEffects.HORSE_SADDLE, 0.5F, 1.0F);
        }

    }

    public double getJumpStrength() {
        return this.getAttributeValue(GenericAttributes.JUMP_STRENGTH);
    }

    @Nullable
    protected SoundEffect getEatingSound() {
        return null;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        if (this.random.nextInt(3) == 0) {
            this.stand();
        }

        return null;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.random.nextInt(10) == 0 && !this.isFrozen()) {
            this.stand();
        }

        return null;
    }

    @Nullable
    protected SoundEffect getSoundAngry() {
        this.stand();
        return null;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        if (!state.getMaterial().isLiquid()) {
            IBlockData blockState = this.level.getType(pos.above());
            SoundEffectType soundType = state.getStepSound();
            if (blockState.is(Blocks.SNOW)) {
                soundType = blockState.getStepSound();
            }

            if (this.isVehicle() && this.canGallop) {
                ++this.gallopSoundCounter;
                if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                    this.playGallopSound(soundType);
                } else if (this.gallopSoundCounter <= 5) {
                    this.playSound(SoundEffects.HORSE_STEP_WOOD, soundType.getVolume() * 0.15F, soundType.getPitch());
                }
            } else if (soundType == SoundEffectType.WOOD) {
                this.playSound(SoundEffects.HORSE_STEP_WOOD, soundType.getVolume() * 0.15F, soundType.getPitch());
            } else {
                this.playSound(SoundEffects.HORSE_STEP, soundType.getVolume() * 0.15F, soundType.getPitch());
            }

        }
    }

    protected void playGallopSound(SoundEffectType group) {
        this.playSound(SoundEffects.HORSE_GALLOP, group.getVolume() * 0.15F, group.getPitch());
    }

    public static AttributeProvider.Builder createBaseHorseAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.JUMP_STRENGTH).add(GenericAttributes.MAX_HEALTH, 53.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.225F);
    }

    @Override
    public int getMaxSpawnGroup() {
        return 6;
    }

    public int getMaxDomestication() {
        return 100;
    }

    @Override
    public float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    public void openInventory(EntityHuman player) {
        if (!this.level.isClientSide && (!this.isVehicle() || this.hasPassenger(player)) && this.isTamed()) {
            player.openHorseInventory(this, this.inventory);
        }

    }

    public EnumInteractionResult fedFood(EntityHuman player, ItemStack stack) {
        boolean bl = this.handleEating(player, stack);
        if (!player.getAbilities().instabuild) {
            stack.subtract(1);
        }

        if (this.level.isClientSide) {
            return EnumInteractionResult.CONSUME;
        } else {
            return bl ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
        }
    }

    protected boolean handleEating(EntityHuman player, ItemStack item) {
        boolean bl = false;
        float f = 0.0F;
        int i = 0;
        int j = 0;
        if (item.is(Items.WHEAT)) {
            f = 2.0F;
            i = 20;
            j = 3;
        } else if (item.is(Items.SUGAR)) {
            f = 1.0F;
            i = 30;
            j = 3;
        } else if (item.is(Blocks.HAY_BLOCK.getItem())) {
            f = 20.0F;
            i = 180;
        } else if (item.is(Items.APPLE)) {
            f = 3.0F;
            i = 60;
            j = 3;
        } else if (item.is(Items.GOLDEN_CARROT)) {
            f = 4.0F;
            i = 60;
            j = 5;
            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                bl = true;
                this.setInLove(player);
            }
        } else if (item.is(Items.GOLDEN_APPLE) || item.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            f = 10.0F;
            i = 240;
            j = 10;
            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                bl = true;
                this.setInLove(player);
            }
        }

        if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
            this.heal(f);
            bl = true;
        }

        if (this.isBaby() && i > 0) {
            this.level.addParticle(Particles.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
            if (!this.level.isClientSide) {
                this.setAge(i);
            }

            bl = true;
        }

        if (j > 0 && (bl || !this.isTamed()) && this.getTemper() < this.getMaxDomestication()) {
            bl = true;
            if (!this.level.isClientSide) {
                this.modifyTemper(j);
            }
        }

        if (bl) {
            this.eating();
            this.gameEvent(GameEvent.EAT, this.eyeBlockPosition());
        }

        return bl;
    }

    protected void doPlayerRide(EntityHuman player) {
        this.setEating(false);
        this.setStanding(false);
        if (!this.level.isClientSide) {
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
        }

    }

    @Override
    protected boolean isFrozen() {
        return super.isFrozen() && this.isVehicle() && this.hasSaddle() || this.isEating() || this.isStanding();
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    private void moveTail() {
        this.tailCounter = 1;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.inventory != null) {
            for(int i = 0; i < this.inventory.getSize(); ++i) {
                ItemStack itemStack = this.inventory.getItem(i);
                if (!itemStack.isEmpty() && !EnchantmentManager.shouldNotDrop(itemStack)) {
                    this.spawnAtLocation(itemStack);
                }
            }

        }
    }

    @Override
    public void movementTick() {
        if (this.random.nextInt(200) == 0) {
            this.moveTail();
        }

        super.movementTick();
        if (!this.level.isClientSide && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }

            if (this.canEatGrass()) {
                if (!this.isEating() && !this.isVehicle() && this.random.nextInt(300) == 0 && this.level.getType(this.getChunkCoordinates().below()).is(Blocks.GRASS_BLOCK)) {
                    this.setEating(true);
                }

                if (this.isEating() && ++this.eatingCounter > 50) {
                    this.eatingCounter = 0;
                    this.setEating(false);
                }
            }

            this.followMommy();
        }
    }

    protected void followMommy() {
        if (this.hasReproduced() && this.isBaby() && !this.isEating()) {
            EntityLiving livingEntity = this.level.getNearestEntity(EntityHorseAbstract.class, MOMMY_TARGETING, this, this.locX(), this.locY(), this.locZ(), this.getBoundingBox().inflate(16.0D));
            if (livingEntity != null && this.distanceToSqr(livingEntity) > 4.0D) {
                this.navigation.createPath(livingEntity, 0);
            }
        }

    }

    public boolean canEatGrass() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
            this.mouthCounter = 0;
            this.setFlag(64, false);
        }

        if ((this.isControlledByLocalInstance() || this.doAITick()) && this.standCounter > 0 && ++this.standCounter > 20) {
            this.standCounter = 0;
            this.setStanding(false);
        }

        if (this.tailCounter > 0 && ++this.tailCounter > 8) {
            this.tailCounter = 0;
        }

        if (this.sprintCounter > 0) {
            ++this.sprintCounter;
            if (this.sprintCounter > 300) {
                this.sprintCounter = 0;
            }
        }

        this.eatAnimO = this.eatAnim;
        if (this.isEating()) {
            this.eatAnim += (1.0F - this.eatAnim) * 0.4F + 0.05F;
            if (this.eatAnim > 1.0F) {
                this.eatAnim = 1.0F;
            }
        } else {
            this.eatAnim += (0.0F - this.eatAnim) * 0.4F - 0.05F;
            if (this.eatAnim < 0.0F) {
                this.eatAnim = 0.0F;
            }
        }

        this.standAnimO = this.standAnim;
        if (this.isStanding()) {
            this.eatAnim = 0.0F;
            this.eatAnimO = this.eatAnim;
            this.standAnim += (1.0F - this.standAnim) * 0.4F + 0.05F;
            if (this.standAnim > 1.0F) {
                this.standAnim = 1.0F;
            }
        } else {
            this.allowStandSliding = false;
            this.standAnim += (0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F;
            if (this.standAnim < 0.0F) {
                this.standAnim = 0.0F;
            }
        }

        this.mouthAnimO = this.mouthAnim;
        if (this.getFlag(64)) {
            this.mouthAnim += (1.0F - this.mouthAnim) * 0.7F + 0.05F;
            if (this.mouthAnim > 1.0F) {
                this.mouthAnim = 1.0F;
            }
        } else {
            this.mouthAnim += (0.0F - this.mouthAnim) * 0.7F - 0.05F;
            if (this.mouthAnim < 0.0F) {
                this.mouthAnim = 0.0F;
            }
        }

    }

    private void openMouth() {
        if (!this.level.isClientSide) {
            this.mouthCounter = 1;
            this.setFlag(64, true);
        }

    }

    public void setEating(boolean eatingGrass) {
        this.setFlag(16, eatingGrass);
    }

    public void setStanding(boolean angry) {
        if (angry) {
            this.setEating(false);
        }

        this.setFlag(32, angry);
    }

    private void stand() {
        if (this.isControlledByLocalInstance() || this.doAITick()) {
            this.standCounter = 1;
            this.setStanding(true);
        }

    }

    public void makeMad() {
        if (!this.isStanding()) {
            this.stand();
            SoundEffect soundEvent = this.getSoundAngry();
            if (soundEvent != null) {
                this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
            }
        }

    }

    public boolean tameWithName(EntityHuman player) {
        this.setOwnerUUID(player.getUniqueID());
        this.setTamed(true);
        if (player instanceof EntityPlayer) {
            CriterionTriggers.TAME_ANIMAL.trigger((EntityPlayer)player, this);
        }

        this.level.broadcastEntityEffect(this, (byte)7);
        return true;
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.isAlive()) {
            if (this.isVehicle() && this.canBeControlledByRider() && this.hasSaddle()) {
                EntityLiving livingEntity = (EntityLiving)this.getRidingPassenger();
                this.setYRot(livingEntity.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(livingEntity.getXRot() * 0.5F);
                this.setYawPitch(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;
                float f = livingEntity.xxa * 0.5F;
                float g = livingEntity.zza;
                if (g <= 0.0F) {
                    g *= 0.25F;
                    this.gallopSoundCounter = 0;
                }

                if (this.onGround && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
                    f = 0.0F;
                    g = 0.0F;
                }

                if (this.playerJumpPendingScale > 0.0F && !this.isJumping() && this.onGround) {
                    double d = this.getJumpStrength() * (double)this.playerJumpPendingScale * (double)this.getBlockJumpFactor();
                    double e = d + this.getJumpBoostPower();
                    Vec3D vec3 = this.getMot();
                    this.setMot(vec3.x, e, vec3.z);
                    this.setIsJumping(true);
                    this.hasImpulse = true;
                    if (g > 0.0F) {
                        float h = MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F));
                        float i = MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F));
                        this.setMot(this.getMot().add((double)(-0.4F * h * this.playerJumpPendingScale), 0.0D, (double)(0.4F * i * this.playerJumpPendingScale)));
                    }

                    this.playerJumpPendingScale = 0.0F;
                }

                this.flyingSpeed = this.getSpeed() * 0.1F;
                if (this.isControlledByLocalInstance()) {
                    this.setSpeed((float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                    super.travel(new Vec3D((double)f, movementInput.y, (double)g));
                } else if (livingEntity instanceof EntityHuman) {
                    this.setMot(Vec3D.ZERO);
                }

                if (this.onGround) {
                    this.playerJumpPendingScale = 0.0F;
                    this.setIsJumping(false);
                }

                this.calculateEntityAnimation(this, false);
                this.tryCheckInsideBlocks();
            } else {
                this.flyingSpeed = 0.02F;
                super.travel(movementInput);
            }
        }
    }

    protected void playJumpSound() {
        this.playSound(SoundEffects.HORSE_JUMP, 0.4F, 1.0F);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("EatingHaystack", this.isEating());
        nbt.setBoolean("Bred", this.hasReproduced());
        nbt.setInt("Temper", this.getTemper());
        nbt.setBoolean("Tame", this.isTamed());
        if (this.getOwnerUUID() != null) {
            nbt.putUUID("Owner", this.getOwnerUUID());
        }

        if (!this.inventory.getItem(0).isEmpty()) {
            nbt.set("SaddleItem", this.inventory.getItem(0).save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setEating(nbt.getBoolean("EatingHaystack"));
        this.setBred(nbt.getBoolean("Bred"));
        this.setTemper(nbt.getInt("Temper"));
        this.setTamed(nbt.getBoolean("Tame"));
        UUID uUID;
        if (nbt.hasUUID("Owner")) {
            uUID = nbt.getUUID("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = NameReferencingFileConverter.convertMobOwnerIfNecessary(this.getMinecraftServer(), string);
        }

        if (uUID != null) {
            this.setOwnerUUID(uUID);
        }

        if (nbt.hasKeyOfType("SaddleItem", 10)) {
            ItemStack itemStack = ItemStack.of(nbt.getCompound("SaddleItem"));
            if (itemStack.is(Items.SADDLE)) {
                this.inventory.setItem(0, itemStack);
            }
        }

        this.updateContainerEquipment();
    }

    @Override
    public boolean mate(EntityAnimal other) {
        return false;
    }

    protected boolean canParent() {
        return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return null;
    }

    protected void setOffspringAttributes(EntityAgeable mate, EntityHorseAbstract child) {
        double d = this.getAttributeBaseValue(GenericAttributes.MAX_HEALTH) + mate.getAttributeBaseValue(GenericAttributes.MAX_HEALTH) + (double)this.generateRandomMaxHealth();
        child.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(d / 3.0D);
        double e = this.getAttributeBaseValue(GenericAttributes.JUMP_STRENGTH) + mate.getAttributeBaseValue(GenericAttributes.JUMP_STRENGTH) + this.generateRandomJumpStrength();
        child.getAttributeInstance(GenericAttributes.JUMP_STRENGTH).setValue(e / 3.0D);
        double f = this.getAttributeBaseValue(GenericAttributes.MOVEMENT_SPEED) + mate.getAttributeBaseValue(GenericAttributes.MOVEMENT_SPEED) + this.generateRandomSpeed();
        child.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(f / 3.0D);
    }

    @Override
    public boolean canBeControlledByRider() {
        return this.getRidingPassenger() instanceof EntityLiving;
    }

    public float getEatAnim(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.eatAnimO, this.eatAnim);
    }

    public float getStandAnim(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.standAnimO, this.standAnim);
    }

    public float getMouthAnim(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.mouthAnimO, this.mouthAnim);
    }

    @Override
    public void onPlayerJump(int strength) {
        if (this.hasSaddle()) {
            if (strength < 0) {
                strength = 0;
            } else {
                this.allowStandSliding = true;
                this.stand();
            }

            if (strength >= 90) {
                this.playerJumpPendingScale = 1.0F;
            } else {
                this.playerJumpPendingScale = 0.4F + 0.4F * (float)strength / 90.0F;
            }

        }
    }

    @Override
    public boolean canJump() {
        return this.hasSaddle();
    }

    @Override
    public void handleStartJump(int height) {
        this.allowStandSliding = true;
        this.stand();
        this.playJumpSound();
    }

    @Override
    public void handleStopJump() {
    }

    protected void spawnTamingParticles(boolean positive) {
        ParticleParam particleOptions = positive ? Particles.HEART : Particles.SMOKE;

        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleOptions, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 7) {
            this.spawnTamingParticles(true);
        } else if (status == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public void positionRider(Entity passenger) {
        super.positionRider(passenger);
        if (passenger instanceof EntityInsentient) {
            EntityInsentient mob = (EntityInsentient)passenger;
            this.yBodyRot = mob.yBodyRot;
        }

        if (this.standAnimO > 0.0F) {
            float f = MathHelper.sin(this.yBodyRot * ((float)Math.PI / 180F));
            float g = MathHelper.cos(this.yBodyRot * ((float)Math.PI / 180F));
            float h = 0.7F * this.standAnimO;
            float i = 0.15F * this.standAnimO;
            passenger.setPosition(this.locX() + (double)(h * f), this.locY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset() + (double)i, this.locZ() - (double)(h * g));
            if (passenger instanceof EntityLiving) {
                ((EntityLiving)passenger).yBodyRot = this.yBodyRot;
            }
        }

    }

    protected float generateRandomMaxHealth() {
        return 15.0F + (float)this.random.nextInt(8) + (float)this.random.nextInt(9);
    }

    protected double generateRandomJumpStrength() {
        return (double)0.4F + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D;
    }

    protected double generateRandomSpeed() {
        return ((double)0.45F + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D) * 0.25D;
    }

    @Override
    public boolean isCurrentlyClimbing() {
        return false;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.95F;
    }

    public boolean canWearArmor() {
        return false;
    }

    public boolean isWearingArmor() {
        return !this.getEquipment(EnumItemSlot.CHEST).isEmpty();
    }

    public boolean isArmor(ItemStack item) {
        return false;
    }

    private SlotAccess createEquipmentSlotAccess(int slot, Predicate<ItemStack> predicate) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return EntityHorseAbstract.this.inventory.getItem(slot);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!predicate.test(stack)) {
                    return false;
                } else {
                    EntityHorseAbstract.this.inventory.setItem(slot, stack);
                    EntityHorseAbstract.this.updateContainerEquipment();
                    return true;
                }
            }
        };
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        int i = mappedIndex - 400;
        if (i >= 0 && i < 2 && i < this.inventory.getSize()) {
            if (i == 0) {
                return this.createEquipmentSlotAccess(i, (stack) -> {
                    return stack.isEmpty() || stack.is(Items.SADDLE);
                });
            }

            if (i == 1) {
                if (!this.canWearArmor()) {
                    return SlotAccess.NULL;
                }

                return this.createEquipmentSlotAccess(i, (stack) -> {
                    return stack.isEmpty() || this.isArmor(stack);
                });
            }
        }

        int j = mappedIndex - 500 + 2;
        return j >= 2 && j < this.inventory.getSize() ? SlotAccess.forContainer(this.inventory, j) : super.getSlot(mappedIndex);
    }

    @Nullable
    @Override
    public Entity getRidingPassenger() {
        return this.getFirstPassenger();
    }

    @Nullable
    private Vec3D getDismountLocationInDirection(Vec3D vec3, EntityLiving livingEntity) {
        double d = this.locX() + vec3.x;
        double e = this.getBoundingBox().minY;
        double f = this.locZ() + vec3.z;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EntityPose pose : livingEntity.getDismountPoses()) {
            mutableBlockPos.set(d, e, f);
            double g = this.getBoundingBox().maxY + 0.75D;

            while(true) {
                double h = this.level.getBlockFloorHeight(mutableBlockPos);
                if ((double)mutableBlockPos.getY() + h > g) {
                    break;
                }

                if (DismountUtil.isBlockFloorValid(h)) {
                    AxisAlignedBB aABB = livingEntity.getLocalBoundsForPose(pose);
                    Vec3D vec32 = new Vec3D(d, (double)mutableBlockPos.getY() + h, f);
                    if (DismountUtil.canDismountTo(this.level, livingEntity, aABB.move(vec32))) {
                        livingEntity.setPose(pose);
                        return vec32;
                    }
                }

                mutableBlockPos.move(EnumDirection.UP);
                if (!((double)mutableBlockPos.getY() < g)) {
                    break;
                }
            }
        }

        return null;
    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        Vec3D vec3 = getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), this.getYRot() + (passenger.getMainHand() == EnumMainHand.RIGHT ? 90.0F : -90.0F));
        Vec3D vec32 = this.getDismountLocationInDirection(vec3, passenger);
        if (vec32 != null) {
            return vec32;
        } else {
            Vec3D vec33 = getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), this.getYRot() + (passenger.getMainHand() == EnumMainHand.LEFT ? 90.0F : -90.0F));
            Vec3D vec34 = this.getDismountLocationInDirection(vec33, passenger);
            return vec34 != null ? vec34 : this.getPositionVector();
        }
    }

    protected void randomizeAttributes() {
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(0.2F);
        }

        this.randomizeAttributes();
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public boolean hasInventoryChanged(IInventory container) {
        return this.inventory != container;
    }
}
