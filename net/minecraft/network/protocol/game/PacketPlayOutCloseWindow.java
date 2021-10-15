package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutCloseWindow implements Packet<PacketListenerPlayOut> {
    private final int containerId;

    public PacketPlayOutCloseWindow(int syncId) {
        this.containerId = syncId;
    }

    public PacketPlayOutCloseWindow(PacketDataSerializer buf) {
        this.containerId = buf.readUnsignedByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleContainerClose(this);
    }

    public int getContainerId() {
        return this.containerId;
    }
}
