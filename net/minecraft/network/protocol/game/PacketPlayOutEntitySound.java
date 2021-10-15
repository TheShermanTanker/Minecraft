package net.minecraft.network.protocol.game;

import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;

public class PacketPlayOutEntitySound implements Packet<PacketListenerPlayOut> {
    private final SoundEffect sound;
    private final SoundCategory source;
    private final int id;
    private final float volume;
    private final float pitch;

    public PacketPlayOutEntitySound(SoundEffect sound, SoundCategory category, Entity entity, float volume, float pitch) {
        Validate.notNull(sound, "sound");
        this.sound = sound;
        this.source = category;
        this.id = entity.getId();
        this.volume = volume;
        this.pitch = pitch;
    }

    public PacketPlayOutEntitySound(PacketDataSerializer buf) {
        this.sound = IRegistry.SOUND_EVENT.fromId(buf.readVarInt());
        this.source = buf.readEnum(SoundCategory.class);
        this.id = buf.readVarInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(IRegistry.SOUND_EVENT.getId(this.sound));
        buf.writeEnum(this.source);
        buf.writeVarInt(this.id);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }

    public SoundEffect getSound() {
        return this.sound;
    }

    public SoundCategory getSource() {
        return this.source;
    }

    public int getId() {
        return this.id;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSoundEntityEvent(this);
    }
}
