package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;

public class VibrationParticleOption implements ParticleParam {
    public static final Codec<VibrationParticleOption> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VibrationPath.CODEC.fieldOf("vibration").forGetter((vibrationParticleOption) -> {
            return vibrationParticleOption.vibrationPath;
        })).apply(instance, VibrationParticleOption::new);
    });
    public static final ParticleParam.Deserializer<VibrationParticleOption> DESERIALIZER = new ParticleParam.Deserializer<VibrationParticleOption>() {
        @Override
        public VibrationParticleOption fromCommand(Particle<VibrationParticleOption> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            float f = (float)stringReader.readDouble();
            stringReader.expect(' ');
            float g = (float)stringReader.readDouble();
            stringReader.expect(' ');
            float h = (float)stringReader.readDouble();
            stringReader.expect(' ');
            float i = (float)stringReader.readDouble();
            stringReader.expect(' ');
            float j = (float)stringReader.readDouble();
            stringReader.expect(' ');
            float k = (float)stringReader.readDouble();
            stringReader.expect(' ');
            int l = stringReader.readInt();
            BlockPosition blockPos = new BlockPosition((double)f, (double)g, (double)h);
            BlockPosition blockPos2 = new BlockPosition((double)i, (double)j, (double)k);
            return new VibrationParticleOption(new VibrationPath(blockPos, new BlockPositionSource(blockPos2), l));
        }

        @Override
        public VibrationParticleOption fromNetwork(Particle<VibrationParticleOption> particleType, PacketDataSerializer friendlyByteBuf) {
            VibrationPath vibrationPath = VibrationPath.read(friendlyByteBuf);
            return new VibrationParticleOption(vibrationPath);
        }
    };
    private final VibrationPath vibrationPath;

    public VibrationParticleOption(VibrationPath vibration) {
        this.vibrationPath = vibration;
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
        VibrationPath.write(buf, this.vibrationPath);
    }

    @Override
    public String writeToString() {
        BlockPosition blockPos = this.vibrationPath.getOrigin();
        double d = (double)blockPos.getX();
        double e = (double)blockPos.getY();
        double f = (double)blockPos.getZ();
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %.2f %d", IRegistry.PARTICLE_TYPE.getKey(this.getParticle()), d, e, f, d, e, f, this.vibrationPath.getArrivalInTicks());
    }

    @Override
    public Particle<VibrationParticleOption> getParticle() {
        return Particles.VIBRATION;
    }

    public VibrationPath getVibrationPath() {
        return this.vibrationPath;
    }
}
