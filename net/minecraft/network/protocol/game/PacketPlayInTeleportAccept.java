package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInTeleportAccept implements Packet<PacketListenerPlayIn> {
    private final int id;

    public PacketPlayInTeleportAccept(int teleportId) {
        this.id = teleportId;
    }

    public PacketPlayInTeleportAccept(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleAcceptTeleportPacket(this);
    }

    public int getId() {
        return this.id;
    }
}
