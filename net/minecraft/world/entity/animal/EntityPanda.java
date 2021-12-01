package net.minecraft.world.entity.animal;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntityPanda extends EntityAnimal {
    private static final DataWatcherObject<Integer> UNHAPPY_COUNTER = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> SNEEZE_COUNTER = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> EAT_COUNTER = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Byte> MAIN_GENE_ID = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Byte> HIDDEN_GENE_ID = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Byte> DATA_ID_FLAGS = DataWatcher.defineId(EntityPanda.class, DataWatcherRegistry.BYTE);
    static final PathfinderTargetCondition BREED_TARGETING = PathfinderTargetCondition.forNonCombat().range(8.0D);
    private static final int FLAG_SNEEZE = 2;
    private static final int FLAG_ROLL = 4;
    private static final int FLAG_SIT = 8;
    private static final int FLAG_ON_BACK = 16;
    private static final int EAT_TICK_INTERVAL = 5;
    public static final int TOTAL_ROLL_STEPS = 32;
    private static final int TOTAL_UNHAPPY_TIME = 32;
    boolean gotBamboo;
    boolean didBite;
    public int rollCounter;
    private Vec3D rollDelta;
    private float sitAmount;
    private float sitAmountO;
    private float onBackAmount;
    private float onBackAmountO;
    private float rollAmount;
    private float rollAmountO;
    EntityPanda.PandaLookAtPlayerGoal lookAtPlayerGoal;
    static final Predicate<EntityItem> PANDA_ITEMS = (item) -> {
        ItemStack itemStack = item.getItemStack();
        return (itemStack.is(Blocks.BAMBOO.getItem()) || itemStack.is(Blocks.CAKE.getItem())) && item.isAlive() && !item.hasPickUpDelay();
    };

    public EntityPanda(EntityTypes<? extends EntityPanda> type, World world) {
        super(type, world);
        this.moveControl = new EntityPanda.PandaMoveControl(this);
        if (!this.isBaby()) {
            this.setCanPickupLoot(true);
        }

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

    public int getUnhappyCounter() {
        return this.entityData.get(UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int askForBambooTicks) {
        this.entityData.set(UNHAPPY_COUNTER, askForBambooTicks);
    }

    public boolean isSneezing() {
        return this.getFlag(2);
    }

    public boolean isSitting() {
        return this.getFlag(8);
    }

    public void sit(boolean scared) {
        this.setFlag(8, scared);
    }

    public boolean isOnBack() {
        return this.getFlag(16);
    }

    public void setOnBack(boolean lyingOnBack) {
        this.setFlag(16, lyingOnBack);
    }

    public boolean isEating() {
        return this.entityData.get(EAT_COUNTER) > 0;
    }

    public void eat(boolean eating) {
        this.entityData.set(EAT_COUNTER, eating ? 1 : 0);
    }

    private int getEatCounter() {
        return this.entityData.get(EAT_COUNTER);
    }

    private void setEatCounter(int eatingTicks) {
        this.entityData.set(EAT_COUNTER, eatingTicks);
    }

    public void sneeze(boolean sneezing) {
        this.setFlag(2, sneezing);
        if (!sneezing) {
            this.setSneezeCounter(0);
        }

    }

    public int getSneezeCounter() {
        return this.entityData.get(SNEEZE_COUNTER);
    }

    public void setSneezeCounter(int sneezeProgress) {
        this.entityData.set(SNEEZE_COUNTER, sneezeProgress);
    }

    public EntityPanda.Gene getMainGene() {
        return EntityPanda.Gene.byId(this.entityData.get(MAIN_GENE_ID));
    }

    public void setMainGene(EntityPanda.Gene gene) {
        if (gene.getId() > 6) {
            gene = EntityPanda.Gene.getRandom(this.random);
        }

        this.entityData.set(MAIN_GENE_ID, (byte)gene.getId());
    }

    public EntityPanda.Gene getHiddenGene() {
        return EntityPanda.Gene.byId(this.entityData.get(HIDDEN_GENE_ID));
    }

    public void setHiddenGene(EntityPanda.Gene gene) {
        if (gene.getId() > 6) {
            gene = EntityPanda.Gene.getRandom(this.random);
        }

        this.entityData.set(HIDDEN_GENE_ID, (byte)gene.getId());
    }

    public boolean isRolling() {
        return this.getFlag(4);
    }

    public void roll(boolean playing) {
        this.setFlag(4, playing);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(UNHAPPY_COUNTER, 0);
        this.entityData.register(SNEEZE_COUNTER, 0);
        this.entityData.register(MAIN_GENE_ID, (byte)0);
        this.entityData.register(HIDDEN_GENE_ID, (byte)0);
        this.entityData.register(DATA_ID_FLAGS, (byte)0);
        this.entityData.register(EAT_COUNTER, 0);
    }

    private boolean getFlag(int bitmask) {
        return (this.entityData.get(DATA_ID_FLAGS) & bitmask) != 0;
    }

    private void setFlag(int mask, boolean value) {
        byte b = this.entityData.get(DATA_ID_FLAGS);
        if (value) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b | mask));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b & ~mask));
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setString("MainGene", this.getMainGene().getName());
        nbt.setString("HiddenGene", this.getHiddenGene().getName());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setMainGene(EntityPanda.Gene.byName(nbt.getString("MainGene")));
        this.setHiddenGene(EntityPanda.Gene.byName(nbt.getString("HiddenGene")));
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        EntityPanda panda = EntityTypes.PANDA.create(world);
        if (entity instanceof EntityPanda) {
            panda.setGeneFromParents(this, (EntityPanda)entity);
        }

        panda.setAttributes();
        return panda;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new EntityPanda.PandaPanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new EntityPanda.PandaBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new EntityPanda.PandaAttackGoal(this, (double)1.2F, true));
        this.goalSelector.addGoal(4, new PathfinderGoalTempt(this, 1.0D, RecipeItemStack.of(Blocks.BAMBOO.getItem()), false));
        this.goalSelector.addGoal(6, new EntityPanda.PandaAvoidGoal<>(this, EntityHuman.class, 8.0F, 2.0D, 2.0D));
        this.goalSelector.addGoal(6, new EntityPanda.PandaAvoidGoal<>(this, EntityMonster.class, 4.0F, 2.0D, 2.0D));
        this.goalSelector.addGoal(7, new EntityPanda.PandaSitGoal());
        this.goalSelector.addGoal(8, new EntityPanda.PandaLieOnBackGoal(this));
        this.goalSelector.addGoal(8, new EntityPanda.PandaSneezeGoal(this));
        this.lookAtPlayerGoal = new EntityPanda.PandaLookAtPlayerGoal(this, EntityHuman.class, 6.0F);
        this.goalSelector.addGoal(9, this.lookAtPlayerGoal);
        this.goalSelector.addGoal(10, new PathfinderGoalRandomLookaround(this));
        this.goalSelector.addGoal(12, new EntityPanda.PandaRollGoal(this));
        this.goalSelector.addGoal(13, new PathfinderGoalFollowParent(this, 1.25D));
        this.goalSelector.addGoal(14, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.targetSelector.addGoal(1, (new EntityPanda.PandaHurtByTargetGoal(this)).setAlertOthers(new Class[0]));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.15F).add(GenericAttributes.ATTACK_DAMAGE, 6.0D);
    }

    public EntityPanda.Gene getActiveGene() {
        return EntityPanda.Gene.getVariantFromGenes(this.getMainGene(), this.getHiddenGene());
    }

    public boolean isLazy() {
        return this.getActiveGene() == EntityPanda.Gene.LAZY;
    }

    public boolean isWorried() {
        return this.getActiveGene() == EntityPanda.Gene.WORRIED;
    }

    public boolean isPlayful() {
        return this.getActiveGene() == EntityPanda.Gene.PLAYFUL;
    }

    public boolean isBrown() {
        return this.getActiveGene() == EntityPanda.Gene.BROWN;
    }

    public boolean isWeak() {
        return this.getActiveGene() == EntityPanda.Gene.WEAK;
    }

    @Override
    public boolean isAggressive() {
        return this.getActiveGene() == EntityPanda.Gene.AGGRESSIVE;
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return false;
    }

    @Override
    public boolean attackEntity(Entity target) {
        this.playSound(SoundEffects.PANDA_BITE, 1.0F, 1.0F);
        if (!this.isAggressive()) {
            this.didBite = true;
        }

        return super.attackEntity(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isWorried()) {
            if (this.level.isThundering() && !this.isInWater()) {
                this.sit(true);
                this.eat(false);
            } else if (!this.isEating()) {
                this.sit(false);
            }
        }

        EntityLiving livingEntity = this.getGoalTarget();
        if (livingEntity == null) {
            this.gotBamboo = false;
            this.didBite = false;
        }

        if (this.getUnhappyCounter() > 0) {
            if (livingEntity != null) {
                this.lookAt(livingEntity, 90.0F, 90.0F);
            }

            if (this.getUnhappyCounter() == 29 || this.getUnhappyCounter() == 14) {
                this.playSound(SoundEffects.PANDA_CANT_BREED, 1.0F, 1.0F);
            }

            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }

        if (this.isSneezing()) {
            this.setSneezeCounter(this.getSneezeCounter() + 1);
            if (this.getSneezeCounter() > 20) {
                this.sneeze(false);
                this.afterSneeze();
            } else if (this.getSneezeCounter() == 1) {
                this.playSound(SoundEffects.PANDA_PRE_SNEEZE, 1.0F, 1.0F);
            }
        }

        if (this.isRolling()) {
            this.handleRoll();
        } else {
            this.rollCounter = 0;
        }

        if (this.isSitting()) {
            this.setXRot(0.0F);
        }

        this.updateSitAmount();
        this.handleEating();
        this.updateOnBackAnimation();
        this.updateRollAmount();
    }

    public boolean isScared() {
        return this.isWorried() && this.level.isThundering();
    }

    private void handleEating() {
        if (!this.isEating() && this.isSitting() && !this.isScared() && !this.getEquipment(EnumItemSlot.MAINHAND).isEmpty() && this.random.nextInt(80) == 1) {
            this.eat(true);
        } else if (this.getEquipment(EnumItemSlot.MAINHAND).isEmpty() || !this.isSitting()) {
            this.eat(false);
        }

        if (this.isEating()) {
            this.addEatingParticles();
            if (!this.level.isClientSide && this.getEatCounter() > 80 && this.random.nextInt(20) == 1) {
                if (this.getEatCounter() > 100 && this.isFoodOrCake(this.getEquipment(EnumItemSlot.MAINHAND))) {
                    if (!this.level.isClientSide) {
                        this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                        this.gameEvent(GameEvent.EAT, this.eyeBlockPosition());
                    }

                    this.sit(false);
                }

                this.eat(false);
                return;
            }

            this.setEatCounter(this.getEatCounter() + 1);
        }

    }

    private void addEatingParticles() {
        if (this.getEatCounter() % 5 == 0) {
            this.playSound(SoundEffects.PANDA_EAT, 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

            for(int i = 0; i < 6; ++i) {
                Vec3D vec3 = new Vec3D(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, ((double)this.random.nextFloat() - 0.5D) * 0.1D);
                vec3 = vec3.xRot(-this.getXRot() * ((float)Math.PI / 180F));
                vec3 = vec3.yRot(-this.getYRot() * ((float)Math.PI / 180F));
                double d = (double)(-this.random.nextFloat()) * 0.6D - 0.3D;
                Vec3D vec32 = new Vec3D(((double)this.random.nextFloat() - 0.5D) * 0.8D, d, 1.0D + ((double)this.random.nextFloat() - 0.5D) * 0.4D);
                vec32 = vec32.yRot(-this.yBodyRot * ((float)Math.PI / 180F));
                vec32 = vec32.add(this.locX(), this.getHeadY() + 1.0D, this.locZ());
                this.level.addParticle(new ParticleParamItem(Particles.ITEM, this.getEquipment(EnumItemSlot.MAINHAND)), vec32.x, vec32.y, vec32.z, vec3.x, vec3.y + 0.05D, vec3.z);
            }
        }

    }

    private void updateSitAmount() {
        this.sitAmountO = this.sitAmount;
        if (this.isSitting()) {
            this.sitAmount = Math.min(1.0F, this.sitAmount + 0.15F);
        } else {
            this.sitAmount = Math.max(0.0F, this.sitAmount - 0.19F);
        }

    }

    private void updateOnBackAnimation() {
        this.onBackAmountO = this.onBackAmount;
        if (this.isOnBack()) {
            this.onBackAmount = Math.min(1.0F, this.onBackAmount + 0.15F);
        } else {
            this.onBackAmount = Math.max(0.0F, this.onBackAmount - 0.19F);
        }

    }

    private void updateRollAmount() {
        this.rollAmountO = this.rollAmount;
        if (this.isRolling()) {
            this.rollAmount = Math.min(1.0F, this.rollAmount + 0.15F);
        } else {
            this.rollAmount = Math.max(0.0F, this.rollAmount - 0.19F);
        }

    }

    public float getSitAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.sitAmountO, this.sitAmount);
    }

    public float getLieOnBackAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.onBackAmountO, this.onBackAmount);
    }

    public float getRollAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.rollAmountO, this.rollAmount);
    }

    private void handleRoll() {
        ++this.rollCounter;
        if (this.rollCounter > 32) {
            this.roll(false);
        } else {
            if (!this.level.isClientSide) {
                Vec3D vec3 = this.getMot();
                if (this.rollCounter == 1) {
                    float f = this.getYRot() * ((float)Math.PI / 180F);
                    float g = this.isBaby() ? 0.1F : 0.2F;
                    this.rollDelta = new Vec3D(vec3.x + (double)(-MathHelper.sin(f) * g), 0.0D, vec3.z + (double)(MathHelper.cos(f) * g));
                    this.setMot(this.rollDelta.add(0.0D, 0.27D, 0.0D));
                } else if ((float)this.rollCounter != 7.0F && (float)this.rollCounter != 15.0F && (float)this.rollCounter != 23.0F) {
                    this.setMot(this.rollDelta.x, vec3.y, this.rollDelta.z);
                } else {
                    this.setMot(0.0D, this.onGround ? 0.27D : vec3.y, 0.0D);
                }
            }

        }
    }

    private void afterSneeze() {
        Vec3D vec3 = this.getMot();
        this.level.addParticle(Particles.SNEEZE, this.locX() - (double)(this.getWidth() + 1.0F) * 0.5D * (double)MathHelper.sin(this.yBodyRot * ((float)Math.PI / 180F)), this.getHeadY() - (double)0.1F, this.locZ() + (double)(this.getWidth() + 1.0F) * 0.5D * (double)MathHelper.cos(this.yBodyRot * ((float)Math.PI / 180F)), vec3.x, 0.0D, vec3.z);
        this.playSound(SoundEffects.PANDA_SNEEZE, 1.0F, 1.0F);

        for(EntityPanda panda : this.level.getEntitiesOfClass(EntityPanda.class, this.getBoundingBox().inflate(10.0D))) {
            if (!panda.isBaby() && panda.onGround && !panda.isInWater() && panda.canPerformAction()) {
                panda.jump();
            }
        }

        if (!this.level.isClientSide() && this.random.nextInt(700) == 0 && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.spawnAtLocation(Items.SLIME_BALL);
        }

    }

    @Override
    protected void pickUpItem(EntityItem item) {
        if (this.getEquipment(EnumItemSlot.MAINHAND).isEmpty() && PANDA_ITEMS.test(item)) {
            this.onItemPickup(item);
            ItemStack itemStack = item.getItemStack();
            this.setSlot(EnumItemSlot.MAINHAND, itemStack);
            this.handDropChances[EnumItemSlot.MAINHAND.getIndex()] = 2.0F;
            this.receive(item, itemStack.getCount());
            item.die();
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        this.sit(false);
        return super.damageEntity(source, amount);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setMainGene(EntityPanda.Gene.getRandom(this.random));
        this.setHiddenGene(EntityPanda.Gene.getRandom(this.random));
        this.setAttributes();
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(0.2F);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public void setGeneFromParents(EntityPanda mother, @Nullable EntityPanda father) {
        if (father == null) {
            if (this.random.nextBoolean()) {
                this.setMainGene(mother.getOneOfGenesRandomly());
                this.setHiddenGene(EntityPanda.Gene.getRandom(this.random));
            } else {
                this.setMainGene(EntityPanda.Gene.getRandom(this.random));
                this.setHiddenGene(mother.getOneOfGenesRandomly());
            }
        } else if (this.random.nextBoolean()) {
            this.setMainGene(mother.getOneOfGenesRandomly());
            this.setHiddenGene(father.getOneOfGenesRandomly());
        } else {
            this.setMainGene(father.getOneOfGenesRandomly());
            this.setHiddenGene(mother.getOneOfGenesRandomly());
        }

        if (this.random.nextInt(32) == 0) {
            this.setMainGene(EntityPanda.Gene.getRandom(this.random));
        }

        if (this.random.nextInt(32) == 0) {
            this.setHiddenGene(EntityPanda.Gene.getRandom(this.random));
        }

    }

    private EntityPanda.Gene getOneOfGenesRandomly() {
        return this.random.nextBoolean() ? this.getMainGene() : this.getHiddenGene();
    }

    public void setAttributes() {
        if (this.isWeak()) {
            this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(10.0D);
        }

        if (this.isLazy()) {
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((double)0.07F);
        }

    }

    void tryToSit() {
        if (!this.isInWater()) {
            this.setZza(0.0F);
            this.getNavigation().stop();
            this.sit(true);
        }

    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isScared()) {
            return EnumInteractionResult.PASS;
        } else if (this.isOnBack()) {
            this.setOnBack(false);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (this.isBreedItem(itemStack)) {
            if (this.getGoalTarget() != null) {
                this.gotBamboo = true;
            }

            if (this.isBaby()) {
                this.usePlayerItem(player, hand, itemStack);
                this.setAge((int)((float)(-this.getAge() / 20) * 0.1F), true);
                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
            } else if (!this.level.isClientSide && this.getAge() == 0 && this.canFallInLove()) {
                this.usePlayerItem(player, hand, itemStack);
                this.setInLove(player);
                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
            } else {
                if (this.level.isClientSide || this.isSitting() || this.isInWater()) {
                    return EnumInteractionResult.PASS;
                }

                this.tryToSit();
                this.eat(true);
                ItemStack itemStack2 = this.getEquipment(EnumItemSlot.MAINHAND);
                if (!itemStack2.isEmpty() && !player.getAbilities().instabuild) {
                    this.spawnAtLocation(itemStack2);
                }

                this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(itemStack.getItem(), 1));
                this.usePlayerItem(player, hand, itemStack);
            }

            return EnumInteractionResult.SUCCESS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.isAggressive()) {
            return SoundEffects.PANDA_AGGRESSIVE_AMBIENT;
        } else {
            return this.isWorried() ? SoundEffects.PANDA_WORRIED_AMBIENT : SoundEffects.PANDA_AMBIENT;
        }
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.PANDA_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return stack.is(Blocks.BAMBOO.getItem());
    }

    private boolean isFoodOrCake(ItemStack stack) {
        return this.isBreedItem(stack) || stack.is(Blocks.CAKE.getItem());
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PANDA_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PANDA_HURT;
    }

    public boolean canPerformAction() {
        return !this.isOnBack() && !this.isScared() && !this.isEating() && !this.isRolling() && !this.isSitting();
    }

    public static enum Gene {
        NORMAL(0, "normal", false),
        LAZY(1, "lazy", false),
        WORRIED(2, "worried", false),
        PLAYFUL(3, "playful", false),
        BROWN(4, "brown", true),
        WEAK(5, "weak", true),
        AGGRESSIVE(6, "aggressive", false);

        private static final EntityPanda.Gene[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(EntityPanda.Gene::getId)).toArray((i) -> {
            return new EntityPanda.Gene[i];
        });
        private static final int MAX_GENE = 6;
        private final int id;
        private final String name;
        private final boolean isRecessive;

        private Gene(int id, String name, boolean recessive) {
            this.id = id;
            this.name = name;
            this.isRecessive = recessive;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean isRecessive() {
            return this.isRecessive;
        }

        static EntityPanda.Gene getVariantFromGenes(EntityPanda.Gene mainGene, EntityPanda.Gene hiddenGene) {
            if (mainGene.isRecessive()) {
                return mainGene == hiddenGene ? mainGene : NORMAL;
            } else {
                return mainGene;
            }
        }

        public static EntityPanda.Gene byId(int id) {
            if (id < 0 || id >= BY_ID.length) {
                id = 0;
            }

            return BY_ID[id];
        }

        public static EntityPanda.Gene byName(String name) {
            for(EntityPanda.Gene gene : values()) {
                if (gene.name.equals(name)) {
                    return gene;
                }
            }

            return NORMAL;
        }

        public static EntityPanda.Gene getRandom(Random random) {
            int i = random.nextInt(16);
            if (i == 0) {
                return LAZY;
            } else if (i == 1) {
                return WORRIED;
            } else if (i == 2) {
                return PLAYFUL;
            } else if (i == 4) {
                return AGGRESSIVE;
            } else if (i < 9) {
                return WEAK;
            } else {
                return i < 11 ? BROWN : NORMAL;
            }
        }
    }

    static class PandaAttackGoal extends PathfinderGoalMeleeAttack {
        private final EntityPanda panda;

        public PandaAttackGoal(EntityPanda panda, double speed, boolean pauseWhenMobIdle) {
            super(panda, speed, pauseWhenMobIdle);
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            return this.panda.canPerformAction() && super.canUse();
        }
    }

    static class PandaAvoidGoal<T extends EntityLiving> extends PathfinderGoalAvoidTarget<T> {
        private final EntityPanda panda;

        public PandaAvoidGoal(EntityPanda panda, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
            super(panda, fleeFromType, distance, slowSpeed, fastSpeed, IEntitySelector.NO_SPECTATORS::test);
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            return this.panda.isWorried() && this.panda.canPerformAction() && super.canUse();
        }
    }

    static class PandaBreedGoal extends PathfinderGoalBreed {
        private final EntityPanda panda;
        private int unhappyCooldown;

        public PandaBreedGoal(EntityPanda panda, double chance) {
            super(panda, chance);
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            if (super.canUse() && this.panda.getUnhappyCounter() == 0) {
                if (!this.canFindBamboo()) {
                    if (this.unhappyCooldown <= this.panda.tickCount) {
                        this.panda.setUnhappyCounter(32);
                        this.unhappyCooldown = this.panda.tickCount + 600;
                        if (this.panda.doAITick()) {
                            EntityHuman player = this.level.getNearestPlayer(EntityPanda.BREED_TARGETING, this.panda);
                            this.panda.lookAtPlayerGoal.setTarget(player);
                        }
                    }

                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        private boolean canFindBamboo() {
            BlockPosition blockPos = this.panda.getChunkCoordinates();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int i = 0; i < 3; ++i) {
                for(int j = 0; j < 8; ++j) {
                    for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            mutableBlockPos.setWithOffset(blockPos, k, i, l);
                            if (this.level.getType(mutableBlockPos).is(Blocks.BAMBOO)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    static class PandaHurtByTargetGoal extends PathfinderGoalHurtByTarget {
        private final EntityPanda panda;

        public PandaHurtByTargetGoal(EntityPanda panda, Class<?>... noRevengeTypes) {
            super(panda, noRevengeTypes);
            this.panda = panda;
        }

        @Override
        public boolean canContinueToUse() {
            if (!this.panda.gotBamboo && !this.panda.didBite) {
                return super.canContinueToUse();
            } else {
                this.panda.setGoalTarget((EntityLiving)null);
                return false;
            }
        }

        @Override
        protected void alertOther(EntityInsentient mob, EntityLiving target) {
            if (mob instanceof EntityPanda && ((EntityPanda)mob).isAggressive()) {
                mob.setGoalTarget(target);
            }

        }
    }

    static class PandaLieOnBackGoal extends PathfinderGoal {
        private final EntityPanda panda;
        private int cooldown;

        public PandaLieOnBackGoal(EntityPanda panda) {
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            return this.cooldown < this.panda.tickCount && this.panda.isLazy() && this.panda.canPerformAction() && this.panda.random.nextInt(reducedTickDelay(400)) == 1;
        }

        @Override
        public boolean canContinueToUse() {
            if (!this.panda.isInWater() && (this.panda.isLazy() || this.panda.random.nextInt(reducedTickDelay(600)) != 1)) {
                return this.panda.random.nextInt(reducedTickDelay(2000)) != 1;
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            this.panda.setOnBack(true);
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            this.panda.setOnBack(false);
            this.cooldown = this.panda.tickCount + 200;
        }
    }

    static class PandaLookAtPlayerGoal extends PathfinderGoalLookAtPlayer {
        private final EntityPanda panda;

        public PandaLookAtPlayerGoal(EntityPanda panda, Class<? extends EntityLiving> targetType, float range) {
            super(panda, targetType, range);
            this.panda = panda;
        }

        public void setTarget(EntityLiving target) {
            this.lookAt = target;
        }

        @Override
        public boolean canContinueToUse() {
            return this.lookAt != null && super.canContinueToUse();
        }

        @Override
        public boolean canUse() {
            if (this.mob.getRandom().nextFloat() >= this.probability) {
                return false;
            } else {
                if (this.lookAt == null) {
                    if (this.lookAtType == EntityHuman.class) {
                        this.lookAt = this.mob.level.getNearestPlayer(this.lookAtContext, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
                    } else {
                        this.lookAt = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().grow((double)this.lookDistance, 3.0D, (double)this.lookDistance), (livingEntity) -> {
                            return true;
                        }), this.lookAtContext, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
                    }
                }

                return this.panda.canPerformAction() && this.lookAt != null;
            }
        }

        @Override
        public void tick() {
            if (this.lookAt != null) {
                super.tick();
            }

        }
    }

    static class PandaMoveControl extends ControllerMove {
        private final EntityPanda panda;

        public PandaMoveControl(EntityPanda panda) {
            super(panda);
            this.panda = panda;
        }

        @Override
        public void tick() {
            if (this.panda.canPerformAction()) {
                super.tick();
            }
        }
    }

    static class PandaPanicGoal extends PathfinderGoalPanic {
        private final EntityPanda panda;

        public PandaPanicGoal(EntityPanda panda, double speed) {
            super(panda, speed);
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            if (!this.panda.isBurning()) {
                return false;
            } else {
                BlockPosition blockPos = this.lookForWater(this.mob.level, this.mob, 5);
                if (blockPos != null) {
                    this.posX = (double)blockPos.getX();
                    this.posY = (double)blockPos.getY();
                    this.posZ = (double)blockPos.getZ();
                    return true;
                } else {
                    return this.findRandomPosition();
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (this.panda.isSitting()) {
                this.panda.getNavigation().stop();
                return false;
            } else {
                return super.canContinueToUse();
            }
        }
    }

    static class PandaRollGoal extends PathfinderGoal {
        private final EntityPanda panda;

        public PandaRollGoal(EntityPanda panda) {
            this.panda = panda;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK, PathfinderGoal.Type.JUMP));
        }

        @Override
        public boolean canUse() {
            if ((this.panda.isBaby() || this.panda.isPlayful()) && this.panda.onGround) {
                if (!this.panda.canPerformAction()) {
                    return false;
                } else {
                    float f = this.panda.getYRot() * ((float)Math.PI / 180F);
                    int i = 0;
                    int j = 0;
                    float g = -MathHelper.sin(f);
                    float h = MathHelper.cos(f);
                    if ((double)Math.abs(g) > 0.5D) {
                        i = (int)((float)i + g / Math.abs(g));
                    }

                    if ((double)Math.abs(h) > 0.5D) {
                        j = (int)((float)j + h / Math.abs(h));
                    }

                    if (this.panda.level.getType(this.panda.getChunkCoordinates().offset(i, -1, j)).isAir()) {
                        return true;
                    } else if (this.panda.isPlayful() && this.panda.random.nextInt(reducedTickDelay(60)) == 1) {
                        return true;
                    } else {
                        return this.panda.random.nextInt(reducedTickDelay(500)) == 1;
                    }
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.panda.roll(true);
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }
    }

    class PandaSitGoal extends PathfinderGoal {
        private int cooldown;

        public PandaSitGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown <= EntityPanda.this.tickCount && !EntityPanda.this.isBaby() && !EntityPanda.this.isInWater() && EntityPanda.this.canPerformAction() && EntityPanda.this.getUnhappyCounter() <= 0) {
                List<EntityItem> list = EntityPanda.this.level.getEntitiesOfClass(EntityItem.class, EntityPanda.this.getBoundingBox().grow(6.0D, 6.0D, 6.0D), EntityPanda.PANDA_ITEMS);
                return !list.isEmpty() || !EntityPanda.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty();
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (!EntityPanda.this.isInWater() && (EntityPanda.this.isLazy() || EntityPanda.this.random.nextInt(reducedTickDelay(600)) != 1)) {
                return EntityPanda.this.random.nextInt(reducedTickDelay(2000)) != 1;
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            if (!EntityPanda.this.isSitting() && !EntityPanda.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty()) {
                EntityPanda.this.tryToSit();
            }

        }

        @Override
        public void start() {
            List<EntityItem> list = EntityPanda.this.level.getEntitiesOfClass(EntityItem.class, EntityPanda.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityPanda.PANDA_ITEMS);
            if (!list.isEmpty() && EntityPanda.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty()) {
                EntityPanda.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            } else if (!EntityPanda.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty()) {
                EntityPanda.this.tryToSit();
            }

            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemStack = EntityPanda.this.getEquipment(EnumItemSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                EntityPanda.this.spawnAtLocation(itemStack);
                EntityPanda.this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                int i = EntityPanda.this.isLazy() ? EntityPanda.this.random.nextInt(50) + 10 : EntityPanda.this.random.nextInt(150) + 10;
                this.cooldown = EntityPanda.this.tickCount + i * 20;
            }

            EntityPanda.this.sit(false);
        }
    }

    static class PandaSneezeGoal extends PathfinderGoal {
        private final EntityPanda panda;

        public PandaSneezeGoal(EntityPanda panda) {
            this.panda = panda;
        }

        @Override
        public boolean canUse() {
            if (this.panda.isBaby() && this.panda.canPerformAction()) {
                if (this.panda.isWeak() && this.panda.random.nextInt(reducedTickDelay(500)) == 1) {
                    return true;
                } else {
                    return this.panda.random.nextInt(reducedTickDelay(6000)) == 1;
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.panda.sneeze(true);
        }
    }
}
