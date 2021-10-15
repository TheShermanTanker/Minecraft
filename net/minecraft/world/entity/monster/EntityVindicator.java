package net.minecraft.world.entity.monster;

import com.google.common.collect.Maps;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreakDoor;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.util.PathfinderGoalUtil;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public class EntityVindicator extends EntityIllagerAbstract {
    private static final String TAG_JOHNNY = "Johnny";
    public static final Predicate<EnumDifficulty> DOOR_BREAKING_PREDICATE = (difficulty) -> {
        return difficulty == EnumDifficulty.NORMAL || difficulty == EnumDifficulty.HARD;
    };
    public boolean isJohnny;

    public EntityVindicator(EntityTypes<? extends EntityVindicator> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityVindicator.VindicatorBreakDoorGoal(this));
        this.goalSelector.addGoal(2, new EntityIllagerAbstract.RaiderOpenDoorGoal(this));
        this.goalSelector.addGoal(3, new EntityRaider.HoldGroundAttackGoal(this, 10.0F));
        this.goalSelector.addGoal(4, new EntityVindicator.VindicatorMeleeAttackGoal(this));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetSelector.addGoal(4, new EntityVindicator.VindicatorJohnnyAttackGoal(this));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
    }

    @Override
    protected void mobTick() {
        if (!this.isNoAI() && PathfinderGoalUtil.hasGroundPathNavigation(this)) {
            boolean bl = ((WorldServer)this.level).isRaided(this.getChunkCoordinates());
            ((Navigation)this.getNavigation()).setCanOpenDoors(bl);
        }

        super.mobTick();
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.35F).add(GenericAttributes.FOLLOW_RANGE, 12.0D).add(GenericAttributes.MAX_HEALTH, 24.0D).add(GenericAttributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.isJohnny) {
            nbt.setBoolean("Johnny", true);
        }

    }

    @Override
    public EntityIllagerAbstract.IllagerArmPose getArmPose() {
        if (this.isAggressive()) {
            return EntityIllagerAbstract.IllagerArmPose.ATTACKING;
        } else {
            return this.isCelebrating() ? EntityIllagerAbstract.IllagerArmPose.CELEBRATING : EntityIllagerAbstract.IllagerArmPose.CROSSED;
        }
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("Johnny", 99)) {
            this.isJohnny = nbt.getBoolean("Johnny");
        }

    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.VINDICATOR_CELEBRATE;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        GroupDataEntity spawnGroupData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        ((Navigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        return spawnGroupData;
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        if (this.getCurrentRaid() == null) {
            this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }

    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) {
            return true;
        } else if (other instanceof EntityLiving && ((EntityLiving)other).getMonsterType() == EnumMonsterType.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        } else {
            return false;
        }
    }

    @Override
    public void setCustomName(@Nullable IChatBaseComponent name) {
        super.setCustomName(name);
        if (!this.isJohnny && name != null && name.getString().equals("Johnny")) {
            this.isJohnny = true;
        }

    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.VINDICATOR_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.VINDICATOR_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.VINDICATOR_HURT;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
        ItemStack itemStack = new ItemStack(Items.IRON_AXE);
        Raid raid = this.getCurrentRaid();
        int i = 1;
        if (wave > raid.getNumGroups(EnumDifficulty.NORMAL)) {
            i = 2;
        }

        boolean bl = this.random.nextFloat() <= raid.getEnchantOdds();
        if (bl) {
            Map<Enchantment, Integer> map = Maps.newHashMap();
            map.put(Enchantments.SHARPNESS, i);
            EnchantmentManager.setEnchantments(map, itemStack);
        }

        this.setSlot(EnumItemSlot.MAINHAND, itemStack);
    }

    static class VindicatorBreakDoorGoal extends PathfinderGoalBreakDoor {
        public VindicatorBreakDoorGoal(EntityInsentient mob) {
            super(mob, 6, EntityVindicator.DOOR_BREAKING_PREDICATE);
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canContinueToUse() {
            EntityVindicator vindicator = (EntityVindicator)this.mob;
            return vindicator.hasActiveRaid() && super.canContinueToUse();
        }

        @Override
        public boolean canUse() {
            EntityVindicator vindicator = (EntityVindicator)this.mob;
            return vindicator.hasActiveRaid() && vindicator.random.nextInt(10) == 0 && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setNoActionTime(0);
        }
    }

    static class VindicatorJohnnyAttackGoal extends PathfinderGoalNearestAttackableTarget<EntityLiving> {
        public VindicatorJohnnyAttackGoal(EntityVindicator vindicator) {
            super(vindicator, EntityLiving.class, 0, true, true, EntityLiving::attackable);
        }

        @Override
        public boolean canUse() {
            return ((EntityVindicator)this.mob).isJohnny && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setNoActionTime(0);
        }
    }

    class VindicatorMeleeAttackGoal extends PathfinderGoalMeleeAttack {
        public VindicatorMeleeAttackGoal(EntityVindicator vindicator) {
            super(vindicator, 1.0D, false);
        }

        @Override
        protected double getAttackReachSqr(EntityLiving entity) {
            if (this.mob.getVehicle() instanceof EntityRavager) {
                float f = this.mob.getVehicle().getWidth() - 0.1F;
                return (double)(f * 2.0F * f * 2.0F + entity.getWidth());
            } else {
                return super.getAttackReachSqr(entity);
            }
        }
    }
}
