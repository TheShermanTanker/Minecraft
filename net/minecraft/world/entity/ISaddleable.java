package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.sounds.EnumSoundCategory;

public interface ISaddleable {
    boolean canSaddle();

    void saddle(@Nullable EnumSoundCategory sound);

    boolean hasSaddle();
}
