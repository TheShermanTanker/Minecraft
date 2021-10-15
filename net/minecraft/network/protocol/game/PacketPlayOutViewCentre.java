package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutViewCentre implements Packet<PacketListenerPlayOut> {
    private final int x;
    private final int z;

    public PacketPlayOutViewCentre(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public PacketPlayOutViewCentre(PacketDataSerializer buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.z);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetChunkCacheCenter(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }
}
