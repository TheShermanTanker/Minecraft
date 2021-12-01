package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Vector3fa;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.phys.Vec3D;

public class ParticleParamDustColorTransition extends ParticleParamDust {
    public static final Vector3fa SCULK_PARTICLE_COLOR = new Vector3fa(Vec3D.fromRGB24(3790560));
    public static final ParticleParamDustColorTransition SCULK_TO_REDSTONE = new ParticleParamDustColorTransition(SCULK_PARTICLE_COLOR, ParticleParamRedstone.REDSTONE_PARTICLE_COLOR, 1.0F);
    public static final Codec<ParticleParamDustColorTransition> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Vector3fa.CODEC.fieldOf("fromColor").forGetter((effect) -> {
            return effect.color;
        }), Vector3fa.CODEC.fieldOf("toColor").forGetter((effect) -> {
            return effect.toColor;
        }), Codec.FLOAT.fieldOf("scale").forGetter((effect) -> {
            return effect.scale;
        })).apply(instance, ParticleParamDustColorTransition::new);
    });
    public static final ParticleParam.Deserializer<ParticleParamDustColorTransition> DESERIALIZER = new ParticleParam.Deserializer<ParticleParamDustColorTransition>() {
        @Override
        public ParticleParamDustColorTransition fromCommand(Particle<ParticleParamDustColorTransition> particleType, StringReader stringReader) throws CommandSyntaxException {
            Vector3fa vector3f = ParticleParamDust.readVector3f(stringReader);
            stringReader.expect(' ');
            float f = stringReader.readFloat();
            Vector3fa vector3f2 = ParticleParamDust.readVector3f(stringReader);
            return new ParticleParamDustColorTransition(vector3f, vector3f2, f);
        }

        @Override
        public ParticleParamDustColorTransition fromNetwork(Particle<ParticleParamDustColorTransition> particleType, PacketDataSerializer friendlyByteBuf) {
            Vector3fa vector3f = ParticleParamDust.readVector3f(friendlyByteBuf);
            float f = friendlyByteBuf.readFloat();
            Vector3fa vector3f2 = ParticleParamDust.readVector3f(friendlyByteBuf);
            return new ParticleParamDustColorTransition(vector3f, vector3f2, f);
        }
    };
    private final Vector3fa toColor;

    public ParticleParamDustColorTransition(Vector3fa fromColor, Vector3fa toColor, float scale) {
        super(fromColor, scale);
        this.toColor = toColor;
    }

    public Vector3fa getFromColor() {
        return this.color;
    }

    public Vector3fa getToColor() {
        return this.toColor;
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
        super.writeToNetwork(buf);
        buf.writeFloat(this.toColor.x());
        buf.writeFloat(this.toColor.y());
        buf.writeFloat(this.toColor.z());
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %.2f %.2f", IRegistry.PARTICLE_TYPE.getKey(this.getParticle()), this.color.x(), this.color.y(), this.color.z(), this.scale, this.toColor.x(), this.toColor.y(), this.toColor.z());
    }

    @Override
    public Particle<ParticleParamDustColorTransition> getParticle() {
        return Particles.DUST_COLOR_TRANSITION;
    }
}
