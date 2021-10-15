package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityChicken extends EntityAnimal {
    private static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS);
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;
    private float nextFlap = 1.0F;
    public int eggTime = this.random.nextInt(6000) + 6000;
    public boolean isChickenJockey;

    public EntityChicken(EntityTypes<? extends EntityChicken> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 1.4D));
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalTempt(this, 1.0D, FOOD_ITEMS, false));
        this.goalSelector.addGoal(4, new PathfinderGoalFollowParent(this, 1.1D));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomLookaround(this));
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? dimensions.height * 0.85F : dimensions.height * 0.92F;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 4.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = (float)((double)this.flapSpeed + (double)(this.onGround ? -1 : 4) * 0.3D);
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
        if (!this.level.isClientSide && this.isAlive() && !this.isBaby() && !this.isChickenJockey() && --this.eggTime <= 0) {
            this.playSound(SoundEffects.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(Items.EGG);
            this.eggTime = this.random.nextInt(6000) + 6000;
        }

    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.CHICKEN_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Override
    public EntityChicken getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        return EntityTypes.CHICKEN.create(serverLevel);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        return this.isChickenJockey() ? 10 : super.getExpValue(player);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.isChickenJockey = nbt.getBoolean("IsChickenJockey");
        if (nbt.hasKey("EggLayTime")) {
            this.eggTime = nbt.getInt("EggLayTime");
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("IsChickenJockey", this.isChickenJockey);
        nbt.setInt("EggLayTime", this.eggTime);
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return this.isChickenJockey();
    }

    @Override
    public void positionRider(Entity passenger) {
        super.positionRider(passenger);
        float f = MathHelper.sin(this.yBodyRot * ((float)Math.PI / 180F));
        float g = MathHelper.cos(this.yBodyRot * ((float)Math.PI / 180F));
        float h = 0.1F;
        float i = 0.0F;
        passenger.setPosition(this.locX() + (double)(0.1F * f), this.getY(0.5D) + passenger.getMyRidingOffset() + 0.0D, this.locZ() - (double)(0.1F * g));
        if (passenger instanceof EntityLiving) {
            ((EntityLiving)passenger).yBodyRot = this.yBodyRot;
        }

    }

    public boolean isChickenJockey() {
        return this.isChickenJockey;
    }

    public void setChickenJockey(boolean hasJockey) {
        this.isChickenJockey = hasJockey;
    }
}
