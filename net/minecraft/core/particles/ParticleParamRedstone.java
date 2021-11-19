package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Vector3fa;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.phys.Vec3D;

public class ParticleParamRedstone extends ParticleParamDust {
    public static final Vector3fa REDSTONE_PARTICLE_COLOR = new Vector3fa(Vec3D.fromRGB24(16711680));
    public static final ParticleParamRedstone REDSTONE = new ParticleParamRedstone(REDSTONE_PARTICLE_COLOR, 1.0F);
    public static final Codec<ParticleParamRedstone> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Vector3fa.CODEC.fieldOf("color").forGetter((dustParticleOptions) -> {
            return dustParticleOptions.color;
        }), Codec.FLOAT.fieldOf("scale").forGetter((dustParticleOptions) -> {
            return dustParticleOptions.scale;
        })).apply(instance, ParticleParamRedstone::new);
    });
    public static final ParticleParam.Deserializer<ParticleParamRedstone> DESERIALIZER = new ParticleParam.Deserializer<ParticleParamRedstone>() {
        @Override
        public ParticleParamRedstone fromCommand(Particle<ParticleParamRedstone> particleType, StringReader stringReader) throws CommandSyntaxException {
            Vector3fa vector3f = ParticleParamDust.readVector3f(stringReader);
            stringReader.expect(' ');
            float f = stringReader.readFloat();
            return new ParticleParamRedstone(vector3f, f);
        }

        @Override
        public ParticleParamRedstone fromNetwork(Particle<ParticleParamRedstone> particleType, PacketDataSerializer friendlyByteBuf) {
            return new ParticleParamRedstone(ParticleParamDust.readVector3f(friendlyByteBuf), friendlyByteBuf.readFloat());
        }
    };

    public ParticleParamRedstone(Vector3fa color, float scale) {
        super(color, scale);
    }

    @Override
    public Particle<ParticleParamRedstone> getParticle() {
        return Particles.DUST;
    }
}
