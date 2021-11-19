package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInPong implements Packet<PacketListenerPlayIn> {
    private final int id;

    public PacketPlayInPong(int parameter) {
        this.id = parameter;
    }

    public PacketPlayInPong(PacketDataSerializer buf) {
        this.id = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.id);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePong(this);
    }

    public int getId() {
        return this.id;
    }
}
