package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SoundTrack {
    public static final Codec<SoundTrack> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SoundEffect.CODEC.fieldOf("sound").forGetter((music) -> {
            return music.event;
        }), Codec.INT.fieldOf("min_delay").forGetter((music) -> {
            return music.minDelay;
        }), Codec.INT.fieldOf("max_delay").forGetter((music) -> {
            return music.maxDelay;
        }), Codec.BOOL.fieldOf("replace_current_music").forGetter((music) -> {
            return music.replaceCurrentMusic;
        })).apply(instance, SoundTrack::new);
    });
    private final SoundEffect event;
    private final int minDelay;
    private final int maxDelay;
    private final boolean replaceCurrentMusic;

    public SoundTrack(SoundEffect sound, int minDelay, int maxDelay, boolean replaceCurrentMusic) {
        this.event = sound;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.replaceCurrentMusic = replaceCurrentMusic;
    }

    public SoundEffect getEvent() {
        return this.event;
    }

    public int getMinDelay() {
        return this.minDelay;
    }

    public int getMaxDelay() {
        return this.maxDelay;
    }

    public boolean replaceCurrentMusic() {
        return this.replaceCurrentMusic;
    }
}
