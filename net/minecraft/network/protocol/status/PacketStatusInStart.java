package net.minecraft.network.protocol.status;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketStatusInStart implements Packet<PacketStatusInListener> {
    public PacketStatusInStart() {
    }

    public PacketStatusInStart(PacketDataSerializer buf) {
    }

    @Override
    public void write(PacketDataSerializer buf) {
    }

    @Override
    public void handle(PacketStatusInListener listener) {
        listener.handleStatusRequest(this);
    }
}
