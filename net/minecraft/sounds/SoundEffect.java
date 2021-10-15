package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.resources.MinecraftKey;

public class SoundEffect {
    public static final Codec<SoundEffect> CODEC = MinecraftKey.CODEC.xmap(SoundEffect::new, (soundEvent) -> {
        return soundEvent.location;
    });
    private final MinecraftKey location;

    public SoundEffect(MinecraftKey id) {
        this.location = id;
    }

    public MinecraftKey getLocation() {
        return this.location;
    }
}
