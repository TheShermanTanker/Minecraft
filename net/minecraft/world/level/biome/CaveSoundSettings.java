package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;

public class CaveSoundSettings {
    public static final Codec<CaveSoundSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SoundEffect.CODEC.fieldOf("sound").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.soundEvent;
        }), Codec.INT.fieldOf("tick_delay").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.tickDelay;
        }), Codec.INT.fieldOf("block_search_extent").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.blockSearchExtent;
        }), Codec.DOUBLE.fieldOf("offset").forGetter((ambientMoodSettings) -> {
            return ambientMoodSettings.soundPositionOffset;
        })).apply(instance, CaveSoundSettings::new);
    });
    public static final CaveSoundSettings LEGACY_CAVE_SETTINGS = new CaveSoundSettings(SoundEffects.AMBIENT_CAVE, 6000, 8, 2.0D);
    private final SoundEffect soundEvent;
    private final int tickDelay;
    private final int blockSearchExtent;
    private final double soundPositionOffset;

    public CaveSoundSettings(SoundEffect sound, int cultivationTicks, int spawnRange, double extraDistance) {
        this.soundEvent = sound;
        this.tickDelay = cultivationTicks;
        this.blockSearchExtent = spawnRange;
        this.soundPositionOffset = extraDistance;
    }

    public SoundEffect getSoundEvent() {
        return this.soundEvent;
    }

    public int getTickDelay() {
        return this.tickDelay;
    }

    public int getBlockSearchExtent() {
        return this.blockSearchExtent;
    }

    public double getSoundPositionOffset() {
        return this.soundPositionOffset;
    }
}
