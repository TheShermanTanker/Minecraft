package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayOutCustomSoundEffect implements Packet<PacketListenerPlayOut> {
    public static final float LOCATION_ACCURACY = 8.0F;
    private final MinecraftKey name;
    private final SoundCategory source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;

    public PacketPlayOutCustomSoundEffect(MinecraftKey sound, SoundCategory category, Vec3D pos, float volume, float pitch) {
        this.name = sound;
        this.source = category;
        this.x = (int)(pos.x * 8.0D);
        this.y = (int)(pos.y * 8.0D);
        this.z = (int)(pos.z * 8.0D);
        this.volume = volume;
        this.pitch = pitch;
    }

    public PacketPlayOutCustomSoundEffect(PacketDataSerializer buf) {
        this.name = buf.readResourceLocation();
        this.source = buf.readEnum(SoundCategory.class);
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeResourceLocation(this.name);
        buf.writeEnum(this.source);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }

    public MinecraftKey getName() {
        return this.name;
    }

    public SoundCategory getSource() {
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
        listener.handleCustomSoundEvent(this);
    }
}
