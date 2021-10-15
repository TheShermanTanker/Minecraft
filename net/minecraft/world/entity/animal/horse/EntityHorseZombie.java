package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
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

public class EntityHorseZombie extends EntityHorseAbstract {
    public EntityHorseZombie(EntityTypes<? extends EntityHorseZombie> type, World world) {
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
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        super.getSoundAmbient();
        return SoundEffects.ZOMBIE_HORSE_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        super.getSoundDeath();
        return SoundEffects.ZOMBIE_HORSE_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        super.getSoundHurt(source);
        return SoundEffects.ZOMBIE_HORSE_HURT;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return EntityTypes.ZOMBIE_HORSE.create(world);
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

    @Override
    protected void addBehaviourGoals() {
    }
}
