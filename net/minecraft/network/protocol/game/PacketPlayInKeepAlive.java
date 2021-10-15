package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInKeepAlive implements Packet<PacketListenerPlayIn> {
    private final long id;

    public PacketPlayInKeepAlive(long id) {
        this.id = id;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleKeepAlive(this);
    }

    public PacketPlayInKeepAlive(PacketDataSerializer buf) {
        this.id = buf.readLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.id);
    }

    public long getId() {
        return this.id;
    }
}
