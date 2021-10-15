package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutUnloadChunk implements Packet<PacketListenerPlayOut> {
    private final int x;
    private final int z;

    public PacketPlayOutUnloadChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public PacketPlayOutUnloadChunk(PacketDataSerializer buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.z);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleForgetLevelChunk(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }
}
