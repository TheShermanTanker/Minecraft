package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;

public class EntityHorseMule extends EntityHorseChestedAbstract {
    public EntityHorseMule(EntityTypes<? extends EntityHorseMule> type, World world) {
        super(type, world);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        super.getSoundAmbient();
        return SoundEffects.MULE_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundAngry() {
        super.getSoundAngry();
        return SoundEffects.MULE_ANGRY;
    }

    @Override
    public SoundEffect getSoundDeath() {
        super.getSoundDeath();
        return SoundEffects.MULE_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getEatingSound() {
        return SoundEffects.MULE_EAT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        super.getSoundHurt(source);
        return SoundEffects.MULE_HURT;
    }

    @Override
    protected void playChestEquipsSound() {
        this.playSound(SoundEffects.MULE_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return EntityTypes.MULE.create(world);
    }
}
