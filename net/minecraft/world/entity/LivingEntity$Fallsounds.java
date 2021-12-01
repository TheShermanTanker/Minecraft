package net.minecraft.world.entity;

import net.minecraft.sounds.SoundEffect;

public record LivingEntity$Fallsounds(SoundEffect small, SoundEffect big) {
    public LivingEntity$Fallsounds(SoundEffect soundEvent, SoundEffect soundEvent2) {
        this.small = soundEvent;
        this.big = soundEvent2;
    }

    public SoundEffect small() {
        return this.small;
    }

    public SoundEffect big() {
        return this.big;
    }
}
