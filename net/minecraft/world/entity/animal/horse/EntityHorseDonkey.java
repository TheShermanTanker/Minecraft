package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.level.World;

public class EntityHorseDonkey extends EntityHorseChestedAbstract {
    public EntityHorseDonkey(EntityTypes<? extends EntityHorseDonkey> type, World world) {
        super(type, world);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        super.getSoundAmbient();
        return SoundEffects.DONKEY_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundAngry() {
        super.getSoundAngry();
        return SoundEffects.DONKEY_ANGRY;
    }

    @Override
    public SoundEffect getSoundDeath() {
        super.getSoundDeath();
        return SoundEffects.DONKEY_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getEatingSound() {
        return SoundEffects.DONKEY_EAT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        super.getSoundHurt(source);
        return SoundEffects.DONKEY_HURT;
    }

    @Override
    public boolean mate(EntityAnimal other) {
        if (other == this) {
            return false;
        } else if (!(other instanceof EntityHorseDonkey) && !(other instanceof EntityHorse)) {
            return false;
        } else {
            return this.canParent() && ((EntityHorseAbstract)other).canParent();
        }
    }

    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        EntityTypes<? extends EntityHorseAbstract> entityType = entity instanceof EntityHorse ? EntityTypes.MULE : EntityTypes.DONKEY;
        EntityHorseAbstract abstractHorse = entityType.create(world);
        this.setOffspringAttributes(entity, abstractHorse);
        return abstractHorse;
    }
}
