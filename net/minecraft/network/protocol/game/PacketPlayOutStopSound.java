package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundCategory;

public class PacketPlayOutStopSound implements Packet<PacketListenerPlayOut> {
    private static final int HAS_SOURCE = 1;
    private static final int HAS_SOUND = 2;
    @Nullable
    private final MinecraftKey name;
    @Nullable
    private final SoundCategory source;

    public PacketPlayOutStopSound(@Nullable MinecraftKey soundId, @Nullable SoundCategory category) {
        this.name = soundId;
        this.source = category;
    }

    public PacketPlayOutStopSound(PacketDataSerializer buf) {
        int i = buf.readByte();
        if ((i & 1) > 0) {
            this.source = buf.readEnum(SoundCategory.class);
        } else {
            this.source = null;
        }

        if ((i & 2) > 0) {
            this.name = buf.readResourceLocation();
        } else {
            this.name = null;
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        if (this.source != null) {
            if (this.name != null) {
                buf.writeByte(3);
                buf.writeEnum(this.source);
                buf.writeResourceLocation(this.name);
            } else {
                buf.writeByte(1);
                buf.writeEnum(this.source);
            }
        } else if (this.name != null) {
            buf.writeByte(2);
            buf.writeResourceLocation(this.name);
        } else {
            buf.writeByte(0);
        }

    }

    @Nullable
    public MinecraftKey getName() {
        return this.name;
    }

    @Nullable
    public SoundCategory getSource() {
        return this.source;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleStopSoundEvent(this);
    }
}
