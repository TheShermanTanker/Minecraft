package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Vector3fa;
import java.util.Locale;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.MathHelper;

public abstract class ParticleParamDust implements ParticleParam {
    public static final float MIN_SCALE = 0.01F;
    public static final float MAX_SCALE = 4.0F;
    protected final Vector3fa color;
    protected final float scale;

    public ParticleParamDust(Vector3fa color, float scale) {
        this.color = color;
        this.scale = MathHelper.clamp(scale, 0.01F, 4.0F);
    }

    public static Vector3fa readVector3f(StringReader stringReader) throws CommandSyntaxException {
        stringReader.expect(' ');
        float f = stringReader.readFloat();
        stringReader.expect(' ');
        float g = stringReader.readFloat();
        stringReader.expect(' ');
        float h = stringReader.readFloat();
        return new Vector3fa(f, g, h);
    }

    public static Vector3fa readVector3f(PacketDataSerializer buf) {
        return new Vector3fa(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
        buf.writeFloat(this.color.x());
        buf.writeFloat(this.color.y());
        buf.writeFloat(this.color.z());
        buf.writeFloat(this.scale);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", IRegistry.PARTICLE_TYPE.getKey(this.getParticle()), this.color.x(), this.color.y(), this.color.z(), this.scale);
    }

    public Vector3fa getColor() {
        return this.color;
    }

    public float getScale() {
        return this.scale;
    }
}
