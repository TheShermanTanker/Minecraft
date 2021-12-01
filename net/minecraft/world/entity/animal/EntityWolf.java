package net.minecraft.world.entity.animal;

import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBeg;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowOwner;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLeapAtTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSit;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalOwnerHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalOwnerHurtTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalRandomTargetNonTamed;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.monster.EntityGhast;
import net.minecraft.world.entity.monster.EntitySkeletonAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntityWolf extends EntityTameableAnimal implements IEntityAngerable {
    private static final DataWatcherObject<Boolean> DATA_INTERESTED_ID = DataWatcher.defineId(EntityWolf.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_COLLAR_COLOR = DataWatcher.defineId(EntityWolf.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_REMAINING_ANGER_TIME = DataWatcher.defineId(EntityWolf.class, DataWatcherRegistry.INT);
    public static final Predicate<EntityLiving> PREY_SELECTOR = (entity) -> {
        EntityTypes<?> entityType = entity.getEntityType();
        return entityType == EntityTypes.SHEEP || entityType == EntityTypes.RABBIT || entityType == EntityTypes.FOX;
    };
    private static final float START_HEALTH = 8.0F;
    private static final float TAME_HEALTH = 20.0F;
    private float interestedAngle;
    private float interestedAngleO;
    private boolean isWet;
    private boolean isShaking;
    private float shakeAnim;
    private float shakeAnimO;
    private static final IntProviderUniform PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    @Nullable
    private UUID persistentAngerTarget;

    public EntityWolf(EntityTypes<? extends EntityWolf> type, World world) {
        super(type, world);
        this.setTamed(false);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new PathfinderGoalSit(this));
        this.goalSelector.addGoal(3, new EntityWolf.WolfAvoidEntityGoal<>(this, EntityLlama.class, 24.0F, 1.5D, 1.5D));
        this.goalSelector.addGoal(4, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.addGoal(5, new PathfinderGoalMeleeAttack(this, 1.0D, true));
        this.goalSelector.addGoal(6, new PathfinderGoalFollowOwner(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(7, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(9, new PathfinderGoalBeg(this, 8.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalOwnerHurtByTarget(this));
        this.targetSelector.addGoal(2, new PathfinderGoalOwnerHurtTarget(this));
        this.targetSelector.addGoal(3, (new PathfinderGoalHurtByTarget(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new PathfinderGoalRandomTargetNonTamed<>(this, EntityAnimal.class, false, PREY_SELECTOR));
        this.targetSelector.addGoal(6, new PathfinderGoalRandomTargetNonTamed<>(this, EntityTurtle.class, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
        this.targetSelector.addGoal(7, new PathfinderGoalNearestAttackableTarget<>(this, EntitySkeletonAbstract.class, false));
        this.targetSelector.addGoal(8, new PathfinderGoalUniversalAngerReset<>(this, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.MAX_HEALTH, 8.0D).add(GenericAttributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_INTERESTED_ID, false);
        this.entityData.register(DATA_COLLAR_COLOR, EnumColor.RED.getColorIndex());
        this.entityData.register(DATA_REMAINING_ANGER_TIME, 0);
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setByte("CollarColor", (byte)this.getCollarColor().getColorIndex());
        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("CollarColor", 99)) {
            this.setCollarColor(EnumColor.fromColorIndex(nbt.getInt("CollarColor")));
        }

        this.readPersistentAngerSaveData(this.level, nbt);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.isAngry()) {
            return SoundEffects.WOLF_GROWL;
        } else if (this.random.nextInt(3) == 0) {
            return this.isTamed() && this.getHealth() < 10.0F ? SoundEffects.WOLF_WHINE : SoundEffects.WOLF_PANT;
        } else {
            return SoundEffects.WOLF_AMBIENT;
        }
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.WOLF_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.WOLF_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (!this.level.isClientSide && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
            this.level.broadcastEntityEffect(this, (byte)8);
        }

        if (!this.level.isClientSide) {
            this.updatePersistentAnger((WorldServer)this.level, true);
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.interestedAngleO = this.interestedAngle;
            if (this.isInterested()) {
                this.interestedAngle += (1.0F - this.interestedAngle) * 0.4F;
            } else {
                this.interestedAngle += (0.0F - this.interestedAngle) * 0.4F;
            }

            if (this.isInWaterRainOrBubble()) {
                this.isWet = true;
                if (this.isShaking && !this.level.isClientSide) {
                    this.level.broadcastEntityEffect(this, (byte)56);
                    this.cancelShake();
                }
            } else if ((this.isWet || this.isShaking) && this.isShaking) {
                if (this.shakeAnim == 0.0F) {
                    this.playSound(SoundEffects.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.gameEvent(GameEvent.WOLF_SHAKING);
                }

                this.shakeAnimO = this.shakeAnim;
                this.shakeAnim += 0.05F;
                if (this.shakeAnimO >= 2.0F) {
                    this.isWet = false;
                    this.isShaking = false;
                    this.shakeAnimO = 0.0F;
                    this.shakeAnim = 0.0F;
                }

                if (this.shakeAnim > 0.4F) {
                    float f = (float)this.locY();
                    int i = (int)(MathHelper.sin((this.shakeAnim - 0.4F) * (float)Math.PI) * 7.0F);
                    Vec3D vec3 = this.getMot();

                    for(int j = 0; j < i; ++j) {
                        float g = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                        float h = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                        this.level.addParticle(Particles.SPLASH, this.locX() + (double)g, (double)(f + 0.8F), this.locZ() + (double)h, vec3.x, vec3.y, vec3.z);
                    }
                }
            }

        }
    }

    private void cancelShake() {
        this.isShaking = false;
        this.shakeAnim = 0.0F;
        this.shakeAnimO = 0.0F;
    }

    @Override
    public void die(DamageSource source) {
        this.isWet = false;
        this.isShaking = false;
        this.shakeAnimO = 0.0F;
        this.shakeAnim = 0.0F;
        super.die(source);
    }

    public boolean isWet() {
        return this.isWet;
    }

    public float getWetShade(float tickDelta) {
        return Math.min(0.5F + MathHelper.lerp(tickDelta, this.shakeAnimO, this.shakeAnim) / 2.0F * 0.5F, 1.0F);
    }

    public float getBodyRollAngle(float tickDelta, float f) {
        float g = (MathHelper.lerp(tickDelta, this.shakeAnimO, this.shakeAnim) + f) / 1.8F;
        if (g < 0.0F) {
            g = 0.0F;
        } else if (g > 1.0F) {
            g = 1.0F;
        }

        return MathHelper.sin(g * (float)Math.PI) * MathHelper.sin(g * (float)Math.PI * 11.0F) * 0.15F * (float)Math.PI;
    }

    public float getHeadRollAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.interestedAngleO, this.interestedAngle) * 0.15F * (float)Math.PI;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.8F;
    }

    @Override
    public int getMaxHeadXRot() {
        return this.isSitting() ? 20 : super.getMaxHeadXRot();
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            this.setWillSit(false);
            if (entity != null && !(entity instanceof EntityHuman) && !(entity instanceof EntityArrow)) {
                amount = (amount + 1.0F) / 2.0F;
            }

            return super.damageEntity(source, amount);
        }
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = target.damageEntity(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
        }

        return bl;
    }

    @Override
    public void setTamed(boolean tamed) {
        super.setTamed(tamed);
        if (tamed) {
            this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(20.0D);
            this.setHealth(20.0F);
        } else {
            this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(8.0D);
        }

        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(4.0D);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        if (this.level.isClientSide) {
            boolean bl = this.isOwnedBy(player) || this.isTamed() || itemStack.is(Items.BONE) && !this.isTamed() && !this.isAngry();
            return bl ? EnumInteractionResult.CONSUME : EnumInteractionResult.PASS;
        } else {
            if (this.isTamed()) {
                if (this.isBreedItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    if (!player.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }

                    this.heal((float)item.getFoodInfo().getNutrition());
                    this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                    return EnumInteractionResult.SUCCESS;
                }

                if (!(item instanceof ItemDye)) {
                    EnumInteractionResult interactionResult = super.mobInteract(player, hand);
                    if ((!interactionResult.consumesAction() || this.isBaby()) && this.isOwnedBy(player)) {
                        this.setWillSit(!this.isWillSit());
                        this.jumping = false;
                        this.navigation.stop();
                        this.setGoalTarget((EntityLiving)null);
                        return EnumInteractionResult.SUCCESS;
                    }

                    return interactionResult;
                }

                EnumColor dyeColor = ((ItemDye)item).getDyeColor();
                if (dyeColor != this.getCollarColor()) {
                    this.setCollarColor(dyeColor);
                    if (!player.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }

                    return EnumInteractionResult.SUCCESS;
                }
            } else if (itemStack.is(Items.BONE) && !this.isAngry()) {
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setGoalTarget((EntityLiving)null);
                    this.setWillSit(true);
                    this.level.broadcastEntityEffect(this, (byte)7);
                } else {
                    this.level.broadcastEntityEffect(this, (byte)6);
                }

                return EnumInteractionResult.SUCCESS;
            }

            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 8) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
        } else if (status == 56) {
            this.cancelShake();
        } else {
            super.handleEntityEvent(status);
        }

    }

    public float getTailAngle() {
        if (this.isAngry()) {
            return 1.5393804F;
        } else {
            return this.isTamed() ? (0.55F - (this.getMaxHealth() - this.getHealth()) * 0.02F) * (float)Math.PI : ((float)Math.PI / 5F);
        }
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.isFood() && item.getFoodInfo().isMeat();
    }

    @Override
    public int getMaxSpawnGroup() {
        return 8;
    }

    @Override
    public int getAnger() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Override
    public void setAnger(int ticks) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, ticks);
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
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

    public EnumColor getCollarColor() {
        return EnumColor.fromColorIndex(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public void setCollarColor(EnumColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getColorIndex());
    }

    @Override
    public EntityWolf getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityWolf wolf = EntityTypes.WOLF.create(serverLevel);
        UUID uUID = this.getOwnerUUID();
        if (uUID != null) {
            wolf.setOwnerUUID(uUID);
            wolf.setTamed(true);
        }

        return wolf;
    }

    public void setIsInterested(boolean begging) {
        this.entityData.set(DATA_INTERESTED_ID, begging);
    }

    @Override
    public boolean mate(EntityAnimal other) {
        if (other == this) {
            return false;
        } else if (!this.isTamed()) {
            return false;
        } else if (!(other instanceof EntityWolf)) {
            return false;
        } else {
            EntityWolf wolf = (EntityWolf)other;
            if (!wolf.isTamed()) {
                return false;
            } else if (wolf.isSitting()) {
                return false;
            } else {
                return this.isInLove() && wolf.isInLove();
            }
        }
    }

    public boolean isInterested() {
        return this.entityData.get(DATA_INTERESTED_ID);
    }

    @Override
    public boolean wantsToAttack(EntityLiving target, EntityLiving owner) {
        if (!(target instanceof EntityCreeper) && !(target instanceof EntityGhast)) {
            if (target instanceof EntityWolf) {
                EntityWolf wolf = (EntityWolf)target;
                return !wolf.isTamed() || wolf.getOwner() != owner;
            } else if (target instanceof EntityHuman && owner instanceof EntityHuman && !((EntityHuman)owner).canHarmPlayer((EntityHuman)target)) {
                return false;
            } else if (target instanceof EntityHorseAbstract && ((EntityHorseAbstract)target).isTamed()) {
                return false;
            } else {
                return !(target instanceof EntityTameableAnimal) || !((EntityTameableAnimal)target).isTamed();
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return !this.isAngry() && super.canBeLeashed(player);
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.6F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    public static boolean checkWolfSpawnRules(EntityTypes<EntityWolf> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getType(pos.below()).is(TagsBlock.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(world, pos);
    }

    class WolfAvoidEntityGoal<T extends EntityLiving> extends PathfinderGoalAvoidTarget<T> {
        private final EntityWolf wolf;

        public WolfAvoidEntityGoal(EntityWolf wolf, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
            super(wolf, fleeFromType, distance, slowSpeed, fastSpeed);
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            if (super.canUse() && this.toAvoid instanceof EntityLlama) {
                return !this.wolf.isTamed() && this.avoidLlama((EntityLlama)this.toAvoid);
            } else {
                return false;
            }
        }

        private boolean avoidLlama(EntityLlama llama) {
            return llama.getStrength() >= EntityWolf.this.random.nextInt(5);
        }

        @Override
        public void start() {
            EntityWolf.this.setGoalTarget((EntityLiving)null);
            super.start();
        }

        @Override
        public void tick() {
            EntityWolf.this.setGoalTarget((EntityLiving)null);
            super.tick();
        }
    }
}
