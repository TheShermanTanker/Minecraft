package net.minecraft.world.entity.animal.horse;

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
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.IInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLlamaFollow;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTame;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.monster.IRangedEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityLlamaSpit;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCarpet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntityLlama extends EntityHorseChestedAbstract implements IRangedEntity {
    private static final int MAX_STRENGTH = 5;
    private static final int VARIANTS = 4;
    private static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Items.WHEAT, Blocks.HAY_BLOCK.getItem());
    private static final DataWatcherObject<Integer> DATA_STRENGTH_ID = DataWatcher.defineId(EntityLlama.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_SWAG_ID = DataWatcher.defineId(EntityLlama.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_VARIANT_ID = DataWatcher.defineId(EntityLlama.class, DataWatcherRegistry.INT);
    boolean didSpit;
    @Nullable
    private EntityLlama caravanHead;
    @Nullable
    private EntityLlama caravanTail;

    public EntityLlama(EntityTypes<? extends EntityLlama> type, World world) {
        super(type, world);
    }

    public boolean isTraderLlama() {
        return false;
    }

    public void setStrength(int strength) {
        this.entityData.set(DATA_STRENGTH_ID, Math.max(1, Math.min(5, strength)));
    }

    private void setRandomStrength() {
        int i = this.random.nextFloat() < 0.04F ? 5 : 3;
        this.setStrength(1 + this.random.nextInt(i));
    }

    public int getStrength() {
        return this.entityData.get(DATA_STRENGTH_ID);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Variant", this.getVariant());
        nbt.setInt("Strength", this.getStrength());
        if (!this.inventory.getItem(1).isEmpty()) {
            nbt.set("DecorItem", this.inventory.getItem(1).save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.setStrength(nbt.getInt("Strength"));
        super.loadData(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.hasKeyOfType("DecorItem", 10)) {
            this.inventory.setItem(1, ItemStack.of(nbt.getCompound("DecorItem")));
        }

        this.updateContainerEquipment();
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalTame(this, 1.2D));
        this.goalSelector.addGoal(2, new PathfinderGoalLlamaFollow(this, (double)2.1F));
        this.goalSelector.addGoal(3, new PathfinderGoalArrowAttack(this, 1.25D, 40, 20.0F));
        this.goalSelector.addGoal(3, new PathfinderGoalPanic(this, 1.2D));
        this.goalSelector.addGoal(4, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(5, new PathfinderGoalFollowParent(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomStrollLand(this, 0.7D));
        this.goalSelector.addGoal(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new EntityLlama.LlamaHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new EntityLlama.LlamaAttackWolfGoal(this));
    }

    public static AttributeProvider.Builder createAttributes() {
        return createBaseChestedHorseAttributes().add(GenericAttributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_STRENGTH_ID, 0);
        this.entityData.register(DATA_SWAG_ID, -1);
        this.entityData.register(DATA_VARIANT_ID, 0);
    }

    public int getVariant() {
        return MathHelper.clamp(this.entityData.get(DATA_VARIANT_ID), 0, 3);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    @Override
    protected int getChestSlots() {
        return this.isCarryingChest() ? 2 + 3 * this.getInventoryColumns() : super.getChestSlots();
    }

    @Override
    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            float f = MathHelper.cos(this.yBodyRot * ((float)Math.PI / 180F));
            float g = MathHelper.sin(this.yBodyRot * ((float)Math.PI / 180F));
            float h = 0.3F;
            passenger.setPosition(this.locX() + (double)(0.3F * g), this.locY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.locZ() - (double)(0.3F * f));
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return (double)this.getHeight() * 0.67D;
    }

    @Override
    public boolean canBeControlledByRider() {
        return false;
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    @Override
    protected boolean handleEating(EntityHuman player, ItemStack item) {
        int i = 0;
        int j = 0;
        float f = 0.0F;
        boolean bl = false;
        if (item.is(Items.WHEAT)) {
            i = 10;
            j = 3;
            f = 2.0F;
        } else if (item.is(Blocks.HAY_BLOCK.getItem())) {
            i = 90;
            j = 6;
            f = 10.0F;
            if (this.isTamed() && this.getAge() == 0 && this.canFallInLove()) {
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
            this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
            if (!this.isSilent()) {
                SoundEffect soundEvent = this.getEatingSound();
                if (soundEvent != null) {
                    this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), this.getEatingSound(), this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
                }
            }
        }

        return bl;
    }

    @Override
    protected boolean isFrozen() {
        return this.isDeadOrDying() || this.isEating();
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setRandomStrength();
        int i;
        if (entityData instanceof EntityLlama.LlamaGroupData) {
            i = ((EntityLlama.LlamaGroupData)entityData).variant;
        } else {
            i = this.random.nextInt(4);
            entityData = new EntityLlama.LlamaGroupData(i);
        }

        this.setVariant(i);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected SoundEffect getSoundAngry() {
        return SoundEffects.LLAMA_ANGRY;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.LLAMA_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.LLAMA_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.LLAMA_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getEatingSound() {
        return SoundEffects.LLAMA_EAT;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.LLAMA_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void playChestEquipsSound() {
        this.playSound(SoundEffects.LLAMA_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public void makeMad() {
        SoundEffect soundEvent = this.getSoundAngry();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public int getInventoryColumns() {
        return this.getStrength();
    }

    @Override
    public boolean canWearArmor() {
        return true;
    }

    @Override
    public boolean isWearingArmor() {
        return !this.inventory.getItem(1).isEmpty();
    }

    @Override
    public boolean isArmor(ItemStack item) {
        return item.is(TagsItem.CARPETS);
    }

    @Override
    public boolean canSaddle() {
        return false;
    }

    @Override
    public void containerChanged(IInventory sender) {
        EnumColor dyeColor = this.getSwag();
        super.containerChanged(sender);
        EnumColor dyeColor2 = this.getSwag();
        if (this.tickCount > 20 && dyeColor2 != null && dyeColor2 != dyeColor) {
            this.playSound(SoundEffects.LLAMA_SWAG, 0.5F, 1.0F);
        }

    }

    @Override
    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            super.updateContainerEquipment();
            this.setSwag(getDyeColor(this.inventory.getItem(1)));
        }
    }

    private void setSwag(@Nullable EnumColor color) {
        this.entityData.set(DATA_SWAG_ID, color == null ? -1 : color.getColorIndex());
    }

    @Nullable
    private static EnumColor getDyeColor(ItemStack color) {
        Block block = Block.asBlock(color.getItem());
        return block instanceof BlockCarpet ? ((BlockCarpet)block).getColor() : null;
    }

    @Nullable
    public EnumColor getSwag() {
        int i = this.entityData.get(DATA_SWAG_ID);
        return i == -1 ? null : EnumColor.fromColorIndex(i);
    }

    @Override
    public int getMaxDomestication() {
        return 30;
    }

    @Override
    public boolean mate(EntityAnimal other) {
        return other != this && other instanceof EntityLlama && this.canParent() && ((EntityLlama)other).canParent();
    }

    @Override
    public EntityLlama getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityLlama llama = this.makeBabyLlama();
        this.setOffspringAttributes(ageableMob, llama);
        EntityLlama llama2 = (EntityLlama)ageableMob;
        int i = this.random.nextInt(Math.max(this.getStrength(), llama2.getStrength())) + 1;
        if (this.random.nextFloat() < 0.03F) {
            ++i;
        }

        llama.setStrength(i);
        llama.setVariant(this.random.nextBoolean() ? this.getVariant() : llama2.getVariant());
        return llama;
    }

    protected EntityLlama makeBabyLlama() {
        return EntityTypes.LLAMA.create(this.level);
    }

    private void spit(EntityLiving target) {
        EntityLlamaSpit llamaSpit = new EntityLlamaSpit(this.level, this);
        double d = target.locX() - this.locX();
        double e = target.getY(0.3333333333333333D) - llamaSpit.locY();
        double f = target.locZ() - this.locZ();
        double g = Math.sqrt(d * d + f * f) * (double)0.2F;
        llamaSpit.shoot(d, e + g, f, 1.5F, 10.0F);
        if (!this.isSilent()) {
            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.LLAMA_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }

        this.level.addEntity(llamaSpit);
        this.didSpit = true;
    }

    void setDidSpit(boolean spit) {
        this.didSpit = spit;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        int i = this.calculateFallDamage(fallDistance, damageMultiplier);
        if (i <= 0) {
            return false;
        } else {
            if (fallDistance >= 6.0F) {
                this.damageEntity(damageSource, (float)i);
                if (this.isVehicle()) {
                    for(Entity entity : this.getAllPassengers()) {
                        entity.damageEntity(damageSource, (float)i);
                    }
                }
            }

            this.playBlockStepSound();
            return true;
        }
    }

    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityLlama llama) {
        this.caravanHead = llama;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityLlama getCaravanHead() {
        return this.caravanHead;
    }

    @Override
    protected double followLeashSpeed() {
        return 2.0D;
    }

    @Override
    protected void followMommy() {
        if (!this.inCaravan() && this.isBaby()) {
            super.followMommy();
        }

    }

    @Override
    public boolean canEatGrass() {
        return false;
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        this.spit(target);
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, 0.75D * (double)this.getHeadHeight(), (double)this.getWidth() * 0.5D);
    }

    static class LlamaAttackWolfGoal extends PathfinderGoalNearestAttackableTarget<EntityWolf> {
        public LlamaAttackWolfGoal(EntityLlama llama) {
            super(llama, EntityWolf.class, 16, false, true, (wolf) -> {
                return !((EntityWolf)wolf).isTamed();
            });
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.25D;
        }
    }

    static class LlamaGroupData extends EntityAgeable.GroupDataAgeable {
        public final int variant;

        LlamaGroupData(int variant) {
            super(true);
            this.variant = variant;
        }
    }

    static class LlamaHurtByTargetGoal extends PathfinderGoalHurtByTarget {
        public LlamaHurtByTargetGoal(EntityLlama llama) {
            super(llama);
        }

        @Override
        public boolean canContinueToUse() {
            if (this.mob instanceof EntityLlama) {
                EntityLlama llama = (EntityLlama)this.mob;
                if (llama.didSpit) {
                    llama.setDidSpit(false);
                    return false;
                }
            }

            return super.canContinueToUse();
        }
    }
}
