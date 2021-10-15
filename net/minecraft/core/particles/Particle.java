package net.minecraft.core.particles;

import com.mojang.serialization.Codec;

public abstract class Particle<T extends ParticleParam> {
    private final boolean overrideLimiter;
    private final ParticleParam.Deserializer<T> deserializer;

    protected Particle(boolean alwaysShow, ParticleParam.Deserializer<T> parametersFactory) {
        this.overrideLimiter = alwaysShow;
        this.deserializer = parametersFactory;
    }

    public boolean getOverrideLimiter() {
        return this.overrideLimiter;
    }

    public ParticleParam.Deserializer<T> getDeserializer() {
        return this.deserializer;
    }

    public abstract Codec<T> codec();
}
