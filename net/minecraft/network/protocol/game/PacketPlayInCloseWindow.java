package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInCloseWindow implements Packet<PacketListenerPlayIn> {
    private final int containerId;

    public PacketPlayInCloseWindow(int syncId) {
        this.containerId = syncId;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleContainerClose(this);
    }

    public PacketPlayInCloseWindow(PacketDataSerializer buf) {
        this.containerId = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
    }

    public int getContainerId() {
        return this.containerId;
    }
}
