package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class EntityHorseSkeleton extends EntityHorseAbstract {
    private final PathfinderGoalHorseTrap skeletonTrapGoal = new PathfinderGoalHorseTrap(this);
    private static final int TRAP_MAX_LIFE = 18000;
    private boolean isTrap;
    public int trapTime;

    public EntityHorseSkeleton(EntityTypes<? extends EntityHorseSkeleton> type, World world) {
        super(type, world);
    }

    public static AttributeProvider.Builder createAttributes() {
        return createBaseHorseAttributes().add(GenericAttributes.MAX_HEALTH, 15.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.2F);
    }

    @Override
    protected void randomizeAttributes() {
        this.getAttributeInstance(GenericAttributes.JUMP_STRENGTH).setValue(this.generateRandomJumpStrength());
    }

    @Override
    protected void addBehaviourGoals() {
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        super.getSoundAmbient();
        return this.isEyeInFluid(TagsFluid.WATER) ? SoundEffects.SKELETON_HORSE_AMBIENT_WATER : SoundEffects.SKELETON_HORSE_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        super.getSoundDeath();
        return SoundEffects.SKELETON_HORSE_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        super.getSoundHurt(source);
        return SoundEffects.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        if (this.onGround) {
            if (!this.isVehicle()) {
                return SoundEffects.SKELETON_HORSE_STEP_WATER;
            }

            ++this.gallopSoundCounter;
            if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                return SoundEffects.SKELETON_HORSE_GALLOP_WATER;
            }

            if (this.gallopSoundCounter <= 5) {
                return SoundEffects.SKELETON_HORSE_STEP_WATER;
            }
        }

        return SoundEffects.SKELETON_HORSE_SWIM;
    }

    @Override
    protected void playSwimSound(float volume) {
        if (this.onGround) {
            super.playSwimSound(0.3F);
        } else {
            super.playSwimSound(Math.min(0.1F, volume * 25.0F));
        }

    }

    @Override
    protected void playJumpSound() {
        if (this.isInWater()) {
            this.playSound(SoundEffects.SKELETON_HORSE_JUMP_WATER, 0.4F, 1.0F);
        } else {
            super.playJumpSound();
        }

    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    public double getPassengersRidingOffset() {
        return super.getPassengersRidingOffset() - 0.1875D;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.isTrap() && this.trapTime++ >= 18000) {
            this.die();
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("SkeletonTrap", this.isTrap());
        nbt.setInt("SkeletonTrapTime", this.trapTime);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setTrap(nbt.getBoolean("SkeletonTrap"));
        this.trapTime = nbt.getInt("SkeletonTrapTime");
    }

    @Override
    public boolean rideableUnderWater() {
        return true;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.96F;
    }

    public boolean isTrap() {
        return this.isTrap;
    }

    public void setTrap(boolean trapped) {
        if (trapped != this.isTrap) {
            this.isTrap = trapped;
            if (trapped) {
                this.goalSelector.addGoal(1, this.skeletonTrapGoal);
            } else {
                this.goalSelector.removeGoal(this.skeletonTrapGoal);
            }

        }
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return EntityTypes.SKELETON_HORSE.create(world);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isTamed()) {
            return EnumInteractionResult.PASS;
        } else if (this.isBaby()) {
            return super.mobInteract(player, hand);
        } else if (player.isSecondaryUseActive()) {
            this.openInventory(player);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        } else {
            if (!itemStack.isEmpty()) {
                if (itemStack.is(Items.SADDLE) && !this.hasSaddle()) {
                    this.openInventory(player);
                    return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
                }

                EnumInteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
                if (interactionResult.consumesAction()) {
                    return interactionResult;
                }
            }

            this.doPlayerRide(player);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        }
    }
}
