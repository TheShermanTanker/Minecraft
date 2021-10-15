package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.monster.piglin.EntityPiglinAbstract;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.pathfinder.PathType;

public class EntitySkeletonWither extends EntitySkeletonAbstract {
    public EntitySkeletonWither(EntityTypes<? extends EntitySkeletonWither> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
    }

    @Override
    protected void initPathfinder() {
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityPiglinAbstract.class, true));
        super.initPathfinder();
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.WITHER_SKELETON_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.WITHER_SKELETON_DEATH;
    }

    @Override
    SoundEffect getStepSound() {
        return SoundEffects.WITHER_SKELETON_STEP;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        Entity entity = source.getEntity();
        if (entity instanceof EntityCreeper) {
            EntityCreeper creeper = (EntityCreeper)entity;
            if (creeper.canCauseHeadDrop()) {
                creeper.setCausedHeadDrop();
                this.spawnAtLocation(Items.WITHER_SKELETON_SKULL);
            }
        }

    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(DifficultyDamageScaler difficulty) {
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        GroupDataEntity spawnGroupData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(4.0D);
        this.reassessWeaponGoal();
        return spawnGroupData;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 2.1F;
    }

    @Override
    public boolean attackEntity(Entity target) {
        if (!super.attackEntity(target)) {
            return false;
        } else {
            if (target instanceof EntityLiving) {
                ((EntityLiving)target).addEffect(new MobEffect(MobEffects.WITHER, 200), this);
            }

            return true;
        }
    }

    @Override
    protected EntityArrow getArrow(ItemStack arrow, float damageModifier) {
        EntityArrow abstractArrow = super.getArrow(arrow, damageModifier);
        abstractArrow.setOnFire(100);
        return abstractArrow;
    }

    @Override
    public boolean canBeAffected(MobEffect effect) {
        return effect.getMobEffect() == MobEffects.WITHER ? false : super.canBeAffected(effect);
    }
}
