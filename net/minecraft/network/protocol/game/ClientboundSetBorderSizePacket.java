package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderSizePacket implements Packet<PacketListenerPlayOut> {
    private final double size;

    public ClientboundSetBorderSizePacket(WorldBorder worldBorder) {
        this.size = worldBorder.getLerpTarget();
    }

    public ClientboundSetBorderSizePacket(PacketDataSerializer buf) {
        this.size = buf.readDouble();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.size);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetBorderSize(this);
    }

    public double getSize() {
        return this.size;
    }
}
