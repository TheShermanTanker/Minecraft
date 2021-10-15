package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.PacketDataSerializer;

public interface ParticleParam {
    Particle<?> getParticle();

    void writeToNetwork(PacketDataSerializer buf);

    String writeToString();

    @Deprecated
    public interface Deserializer<T extends ParticleParam> {
        T fromCommand(Particle<T> type, StringReader reader) throws CommandSyntaxException;

        T fromNetwork(Particle<T> type, PacketDataSerializer buf);
    }
}
