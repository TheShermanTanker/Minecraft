package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderLerpSizePacket implements Packet<PacketListenerPlayOut> {
    private final double oldSize;
    private final double newSize;
    private final long lerpTime;

    public ClientboundSetBorderLerpSizePacket(WorldBorder worldBorder) {
        this.oldSize = worldBorder.getSize();
        this.newSize = worldBorder.getLerpTarget();
        this.lerpTime = worldBorder.getLerpRemainingTime();
    }

    public ClientboundSetBorderLerpSizePacket(PacketDataSerializer buf) {
        this.oldSize = buf.readDouble();
        this.newSize = buf.readDouble();
        this.lerpTime = buf.readVarLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.oldSize);
        buf.writeDouble(this.newSize);
        buf.writeVarLong(this.lerpTime);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetBorderLerpSize(this);
    }

    public double getOldSize() {
        return this.oldSize;
    }

    public double getNewSize() {
        return this.newSize;
    }

    public long getLerpTime() {
        return this.lerpTime;
    }
}
