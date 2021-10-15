package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutKeepAlive implements Packet<PacketListenerPlayOut> {
    private final long id;

    public PacketPlayOutKeepAlive(long id) {
        this.id = id;
    }

    public PacketPlayOutKeepAlive(PacketDataSerializer buf) {
        this.id = buf.readLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.id);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }
}
