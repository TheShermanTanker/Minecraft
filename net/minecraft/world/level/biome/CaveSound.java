package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEffect;

public class CaveSound {
    public static final Codec<CaveSound> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SoundEffect.CODEC.fieldOf("sound").forGetter((ambientAdditionsSettings) -> {
            return ambientAdditionsSettings.soundEvent;
        }), Codec.DOUBLE.fieldOf("tick_chance").forGetter((ambientAdditionsSettings) -> {
            return ambientAdditionsSettings.tickChance;
        })).apply(instance, CaveSound::new);
    });
    private final SoundEffect soundEvent;
    private final double tickChance;

    public CaveSound(SoundEffect sound, double chance) {
        this.soundEvent = sound;
        this.tickChance = chance;
    }

    public SoundEffect getSoundEvent() {
        return this.soundEvent;
    }

    public double getTickChance() {
        return this.tickChance;
    }
}
