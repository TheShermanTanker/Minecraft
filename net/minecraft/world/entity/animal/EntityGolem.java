package net.minecraft.world.entity.animal;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;

public abstract class EntityGolem extends EntityCreature {
    protected EntityGolem(EntityTypes<? extends EntityGolem> type, World world) {
        super(type, world);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return false;
    }
}
