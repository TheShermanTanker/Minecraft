package net.minecraft.world.entity.monster;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBowShoot;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFleeSun;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRestrictSun;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class EntitySkeletonAbstract extends EntityMonster implements IRangedEntity {
    private final PathfinderGoalBowShoot<EntitySkeletonAbstract> bowGoal = new PathfinderGoalBowShoot<>(this, 1.0D, 20, 15.0F);
    private final PathfinderGoalMeleeAttack meleeGoal = new PathfinderGoalMeleeAttack(this, 1.2D, false) {
        @Override
        public void stop() {
            super.stop();
            EntitySkeletonAbstract.this.setAggressive(false);
        }

        @Override
        public void start() {
            super.start();
            EntitySkeletonAbstract.this.setAggressive(true);
        }
    };

    protected EntitySkeletonAbstract(EntityTypes<? extends EntitySkeletonAbstract> type, World world) {
        super(type, world);
        this.reassessWeaponGoal();
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(2, new PathfinderGoalRestrictSun(this));
        this.goalSelector.addGoal(3, new PathfinderGoalFleeSun(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalAvoidTarget<>(this, EntityWolf.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(this));
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, 10, true, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

    abstract SoundEffect getStepSound();

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    public void movementTick() {
        boolean bl = this.isSunBurnTick();
        if (bl) {
            ItemStack itemStack = this.getEquipment(EnumItemSlot.HEAD);
            if (!itemStack.isEmpty()) {
                if (itemStack.isDamageableItem()) {
                    itemStack.setDamage(itemStack.getDamage() + this.random.nextInt(2));
                    if (itemStack.getDamage() >= itemStack.getMaxDamage()) {
                        this.broadcastItemBreak(EnumItemSlot.HEAD);
                        this.setSlot(EnumItemSlot.HEAD, ItemStack.EMPTY);
                    }
                }

                bl = false;
            }

            if (bl) {
                this.setOnFire(8);
            }
        }

        super.movementTick();
    }

    @Override
    public void passengerTick() {
        super.passengerTick();
        if (this.getVehicle() instanceof EntityCreature) {
            EntityCreature pathfinderMob = (EntityCreature)this.getVehicle();
            this.yBodyRot = pathfinderMob.yBodyRot;
        }

    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        super.populateDefaultEquipmentSlots(difficulty);
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        this.reassessWeaponGoal();
        this.setCanPickupLoot(this.random.nextFloat() < 0.55F * difficulty.getSpecialMultiplier());
        if (this.getEquipment(EnumItemSlot.HEAD).isEmpty()) {
            LocalDate localDate = LocalDate.now();
            int i = localDate.get(ChronoField.DAY_OF_MONTH);
            int j = localDate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && this.random.nextFloat() < 0.25F) {
                this.setSlot(EnumItemSlot.HEAD, new ItemStack(this.random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EnumItemSlot.HEAD.getIndex()] = 0.0F;
            }
        }

        return entityData;
    }

    public void reassessWeaponGoal() {
        if (this.level != null && !this.level.isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ItemStack itemStack = this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, Items.BOW));
            if (itemStack.is(Items.BOW)) {
                int i = 20;
                if (this.level.getDifficulty() != EnumDifficulty.HARD) {
                    i = 40;
                }

                this.bowGoal.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.bowGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }

        }
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        ItemStack itemStack = this.getProjectile(this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, Items.BOW)));
        EntityArrow abstractArrow = this.getArrow(itemStack, pullProgress);
        double d = target.locX() - this.locX();
        double e = target.getY(0.3333333333333333D) - abstractArrow.locY();
        double f = target.locZ() - this.locZ();
        double g = Math.sqrt(d * d + f * f);
        abstractArrow.shoot(d, e + g * (double)0.2F, f, 1.6F, (float)(14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEffects.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addEntity(abstractArrow);
    }

    protected EntityArrow getArrow(ItemStack arrow, float damageModifier) {
        return ProjectileHelper.getMobArrow(this, arrow, damageModifier);
    }

    @Override
    public boolean canFireProjectileWeapon(ItemProjectileWeapon weapon) {
        return weapon == Items.BOW;
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.reassessWeaponGoal();
    }

    @Override
    public void setSlot(EnumItemSlot slot, ItemStack stack) {
        super.setSlot(slot, stack);
        if (!this.level.isClientSide) {
            this.reassessWeaponGoal();
        }

    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 1.74F;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.6D;
    }

    public boolean isShaking() {
        return this.isFullyFrozen();
    }
}
