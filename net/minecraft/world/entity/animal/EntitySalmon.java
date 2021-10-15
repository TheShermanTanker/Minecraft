package net.minecraft.world.entity.animal;

import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class EntitySalmon extends EntityFishSchool {
    public EntitySalmon(EntityTypes<? extends EntitySalmon> type, World world) {
        super(type, world);
    }

    @Override
    public int getMaxSchoolSize() {
        return 5;
    }

    @Override
    public ItemStack getBucketItem() {
        return new ItemStack(Items.SALMON_BUCKET);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SALMON_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SALMON_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SALMON_HURT;
    }

    @Override
    protected SoundEffect getSoundFlop() {
        return SoundEffects.SALMON_FLOP;
    }
}
