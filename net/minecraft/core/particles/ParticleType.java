package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;

public class ParticleType extends Particle<ParticleType> implements ParticleParam {
    private static final ParticleParam.Deserializer<ParticleType> DESERIALIZER = new ParticleParam.Deserializer<ParticleType>() {
        @Override
        public ParticleType fromCommand(Particle<ParticleType> particleType, StringReader stringReader) {
            return (ParticleType)particleType;
        }

        @Override
        public ParticleType fromNetwork(Particle<ParticleType> particleType, PacketDataSerializer friendlyByteBuf) {
            return (ParticleType)particleType;
        }
    };
    private final Codec<ParticleType> codec = Codec.unit(this::getType);

    protected ParticleType(boolean alwaysShow) {
        super(alwaysShow, DESERIALIZER);
    }

    @Override
    public ParticleType getType() {
        return this;
    }

    @Override
    public Codec<ParticleType> codec() {
        return this.codec;
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
    }

    @Override
    public String writeToString() {
        return IRegistry.PARTICLE_TYPE.getKey(this).toString();
    }
}
