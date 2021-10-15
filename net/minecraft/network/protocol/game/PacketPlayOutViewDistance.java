package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutViewDistance implements Packet<PacketListenerPlayOut> {
    private final int radius;

    public PacketPlayOutViewDistance(int distance) {
        this.radius = distance;
    }

    public PacketPlayOutViewDistance(PacketDataSerializer buf) {
        this.radius = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.radius);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetChunkCacheRadius(this);
    }

    public int getRadius() {
        return this.radius;
    }
}
