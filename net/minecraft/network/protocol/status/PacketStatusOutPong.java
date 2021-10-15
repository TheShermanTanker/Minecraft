package net.minecraft.network.protocol.status;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketStatusOutPong implements Packet<PacketStatusOutListener> {
    private final long time;

    public PacketStatusOutPong(long startTime) {
        this.time = startTime;
    }

    public PacketStatusOutPong(PacketDataSerializer buf) {
        this.time = buf.readLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.time);
    }

    @Override
    public void handle(PacketStatusOutListener listener) {
        listener.handlePongResponse(this);
    }

    public long getTime() {
        return this.time;
    }
}
