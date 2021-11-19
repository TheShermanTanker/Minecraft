package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class PacketPlayOutBorderCenter implements Packet<PacketListenerPlayOut> {
    private final double newCenterX;
    private final double newCenterZ;

    public PacketPlayOutBorderCenter(WorldBorder worldBorder) {
        this.newCenterX = worldBorder.getCenterX();
        this.newCenterZ = worldBorder.getCenterZ();
    }

    public PacketPlayOutBorderCenter(PacketDataSerializer buf) {
        this.newCenterX = buf.readDouble();
        this.newCenterZ = buf.readDouble();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.newCenterX);
        buf.writeDouble(this.newCenterZ);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetBorderCenter(this);
    }

    public double getNewCenterZ() {
        return this.newCenterZ;
    }

    public double getNewCenterX() {
        return this.newCenterX;
    }
}
