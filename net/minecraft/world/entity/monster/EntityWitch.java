package net.minecraft.world.entity.monster;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTargetWitch;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestHealableRaider;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class EntityWitch extends EntityRaider implements IRangedEntity {
    private static final UUID SPEED_MODIFIER_DRINKING_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final AttributeModifier SPEED_MODIFIER_DRINKING = new AttributeModifier(SPEED_MODIFIER_DRINKING_UUID, "Drinking speed penalty", -0.25D, AttributeModifier.Operation.ADDITION);
    private static final DataWatcherObject<Boolean> DATA_USING_ITEM = DataWatcher.defineId(EntityWitch.class, DataWatcherRegistry.BOOLEAN);
    public int usingTime;
    private PathfinderGoalNearestHealableRaider<EntityRaider> healRaidersGoal;
    private PathfinderGoalNearestAttackableTargetWitch<EntityHuman> attackPlayersGoal;

    public EntityWitch(EntityTypes<? extends EntityWitch> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.healRaidersGoal = new PathfinderGoalNearestHealableRaider<>(this, EntityRaider.class, true, (entity) -> {
            return entity != null && this.hasActiveRaid() && entity.getEntityType() != EntityTypes.WITCH;
        });
        this.attackPlayersGoal = new PathfinderGoalNearestAttackableTargetWitch<>(this, EntityHuman.class, 10, true, false, (Predicate<EntityLiving>)null);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new PathfinderGoalArrowAttack(this, 1.0D, 60, 10.0F));
        this.goalSelector.addGoal(2, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(3, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(this, EntityRaider.class));
        this.targetSelector.addGoal(2, this.healRaidersGoal);
        this.targetSelector.addGoal(3, this.attackPlayersGoal);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.getDataWatcher().register(DATA_USING_ITEM, false);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.WITCH_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.WITCH_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.WITCH_DEATH;
    }

    public void setUsingItem(boolean drinking) {
        this.getDataWatcher().set(DATA_USING_ITEM, drinking);
    }

    public boolean isDrinkingPotion() {
        return this.getDataWatcher().get(DATA_USING_ITEM);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 26.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void movementTick() {
        if (!this.level.isClientSide && this.isAlive()) {
            this.healRaidersGoal.decrementCooldown();
            if (this.healRaidersGoal.getCooldown() <= 0) {
                this.attackPlayersGoal.setCanAttack(true);
            } else {
                this.attackPlayersGoal.setCanAttack(false);
            }

            if (this.isDrinkingPotion()) {
                if (this.usingTime-- <= 0) {
                    this.setUsingItem(false);
                    ItemStack itemStack = this.getItemInMainHand();
                    this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                    if (itemStack.is(Items.POTION)) {
                        List<MobEffect> list = PotionUtil.getEffects(itemStack);
                        if (list != null) {
                            for(MobEffect mobEffectInstance : list) {
                                this.addEffect(new MobEffect(mobEffectInstance));
                            }
                        }
                    }

                    this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_DRINKING);
                }
            } else {
                PotionRegistry potion = null;
                if (this.random.nextFloat() < 0.15F && this.isEyeInFluid(TagsFluid.WATER) && !this.hasEffect(MobEffects.WATER_BREATHING)) {
                    potion = Potions.WATER_BREATHING;
                } else if (this.random.nextFloat() < 0.15F && (this.isBurning() || this.getLastDamageSource() != null && this.getLastDamageSource().isFire()) && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    potion = Potions.FIRE_RESISTANCE;
                } else if (this.random.nextFloat() < 0.05F && this.getHealth() < this.getMaxHealth()) {
                    potion = Potions.HEALING;
                } else if (this.random.nextFloat() < 0.5F && this.getGoalTarget() != null && !this.hasEffect(MobEffects.MOVEMENT_SPEED) && this.getGoalTarget().distanceToSqr(this) > 121.0D) {
                    potion = Potions.SWIFTNESS;
                }

                if (potion != null) {
                    this.setSlot(EnumItemSlot.MAINHAND, PotionUtil.setPotion(new ItemStack(Items.POTION), potion));
                    this.usingTime = this.getItemInMainHand().getUseDuration();
                    this.setUsingItem(true);
                    if (!this.isSilent()) {
                        this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.WITCH_DRINK, this.getSoundCategory(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                    }

                    AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
                    attributeInstance.removeModifier(SPEED_MODIFIER_DRINKING);
                    attributeInstance.addTransientModifier(SPEED_MODIFIER_DRINKING);
                }
            }

            if (this.random.nextFloat() < 7.5E-4F) {
                this.level.broadcastEntityEffect(this, (byte)15);
            }
        }

        super.movementTick();
    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.WITCH_CELEBRATE;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 15) {
            for(int i = 0; i < this.random.nextInt(35) + 10; ++i) {
                this.level.addParticle(Particles.WITCH, this.locX() + this.random.nextGaussian() * (double)0.13F, this.getBoundingBox().maxY + 0.5D + this.random.nextGaussian() * (double)0.13F, this.locZ() + this.random.nextGaussian() * (double)0.13F, 0.0D, 0.0D, 0.0D);
            }
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    protected float applyMagicModifier(DamageSource source, float amount) {
        amount = super.applyMagicModifier(source, amount);
        if (source.getEntity() == this) {
            amount = 0.0F;
        }

        if (source.isMagic()) {
            amount = (float)((double)amount * 0.15D);
        }

        return amount;
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        if (!this.isDrinkingPotion()) {
            Vec3D vec3 = target.getMot();
            double d = target.locX() + vec3.x - this.locX();
            double e = target.getHeadY() - (double)1.1F - this.locY();
            double f = target.locZ() + vec3.z - this.locZ();
            double g = Math.sqrt(d * d + f * f);
            PotionRegistry potion = Potions.HARMING;
            if (target instanceof EntityRaider) {
                if (target.getHealth() <= 4.0F) {
                    potion = Potions.HEALING;
                } else {
                    potion = Potions.REGENERATION;
                }

                this.setGoalTarget((EntityLiving)null);
            } else if (g >= 8.0D && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                potion = Potions.SLOWNESS;
            } else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
                potion = Potions.POISON;
            } else if (g <= 3.0D && !target.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
                potion = Potions.WEAKNESS;
            }

            EntityPotion thrownPotion = new EntityPotion(this.level, this);
            thrownPotion.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
            thrownPotion.setXRot(thrownPotion.getXRot() - -20.0F);
            thrownPotion.shoot(d, e + g * 0.2D, f, 0.75F, 8.0F);
            if (!this.isSilent()) {
                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.WITCH_THROW, this.getSoundCategory(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }

            this.level.addEntity(thrownPotion);
        }
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 1.62F;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }
}
