package net.minecraft.network.protocol.game;

import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import org.apache.commons.lang3.Validate;

public class PacketPlayOutNamedSoundEffect implements Packet<PacketListenerPlayOut> {
    public static final float LOCATION_ACCURACY = 8.0F;
    private final SoundEffect sound;
    private final EnumSoundCategory source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;

    public PacketPlayOutNamedSoundEffect(SoundEffect sound, EnumSoundCategory category, double x, double y, double z, float volume, float pitch) {
        Validate.notNull(sound, "sound");
        this.sound = sound;
        this.source = category;
        this.x = (int)(x * 8.0D);
        this.y = (int)(y * 8.0D);
        this.z = (int)(z * 8.0D);
        this.volume = volume;
        this.pitch = pitch;
    }

    public PacketPlayOutNamedSoundEffect(PacketDataSerializer buf) {
        this.sound = IRegistry.SOUND_EVENT.fromId(buf.readVarInt());
        this.source = buf.readEnum(EnumSoundCategory.class);
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(IRegistry.SOUND_EVENT.getId(this.sound));
        buf.writeEnum(this.source);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }

    public SoundEffect getSound() {
        return this.sound;
    }

    public EnumSoundCategory getSource() {
        return this.source;
    }

    public double getX() {
        return (double)((float)this.x / 8.0F);
    }

    public double getY() {
        return (double)((float)this.y / 8.0F);
    }

    public double getZ() {
        return (double)((float)this.z / 8.0F);
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSoundEvent(this);
    }
}
