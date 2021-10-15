package net.minecraft.network.protocol.status;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketStatusInPing implements Packet<PacketStatusInListener> {
    private final long time;

    public PacketStatusInPing(long startTime) {
        this.time = startTime;
    }

    public PacketStatusInPing(PacketDataSerializer buf) {
        this.time = buf.readLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.time);
    }

    @Override
    public void handle(PacketStatusInListener listener) {
        listener.handlePingRequest(this);
    }

    public long getTime() {
        return this.time;
    }
}
