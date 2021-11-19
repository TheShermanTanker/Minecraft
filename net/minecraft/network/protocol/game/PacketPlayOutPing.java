package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutPing implements Packet<PacketListenerPlayOut> {
    private final int id;

    public PacketPlayOutPing(int parameter) {
        this.id = parameter;
    }

    public PacketPlayOutPing(PacketDataSerializer buf) {
        this.id = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.id);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePing(this);
    }

    public int getId() {
        return this.id;
    }
}
