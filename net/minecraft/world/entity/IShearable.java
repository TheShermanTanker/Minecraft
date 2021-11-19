package net.minecraft.world.entity;

import net.minecraft.sounds.EnumSoundCategory;

public interface IShearable {
    void shear(EnumSoundCategory shearedSoundCategory);

    boolean canShear();
}
