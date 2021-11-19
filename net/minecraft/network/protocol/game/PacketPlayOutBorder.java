package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class PacketPlayOutBorder implements Packet<PacketListenerPlayOut> {
    private final double newCenterX;
    private final double newCenterZ;
    private final double oldSize;
    private final double newSize;
    private final long lerpTime;
    private final int newAbsoluteMaxSize;
    private final int warningBlocks;
    private final int warningTime;

    public PacketPlayOutBorder(PacketDataSerializer buf) {
        this.newCenterX = buf.readDouble();
        this.newCenterZ = buf.readDouble();
        this.oldSize = buf.readDouble();
        this.newSize = buf.readDouble();
        this.lerpTime = buf.readVarLong();
        this.newAbsoluteMaxSize = buf.readVarInt();
        this.warningBlocks = buf.readVarInt();
        this.warningTime = buf.readVarInt();
    }

    public PacketPlayOutBorder(WorldBorder worldBorder) {
        this.newCenterX = worldBorder.getCenterX();
        this.newCenterZ = worldBorder.getCenterZ();
        this.oldSize = worldBorder.getSize();
        this.newSize = worldBorder.getLerpTarget();
        this.lerpTime = worldBorder.getLerpRemainingTime();
        this.newAbsoluteMaxSize = worldBorder.getAbsoluteMaxSize();
        this.warningBlocks = worldBorder.getWarningDistance();
        this.warningTime = worldBorder.getWarningTime();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.newCenterX);
        buf.writeDouble(this.newCenterZ);
        buf.writeDouble(this.oldSize);
        buf.writeDouble(this.newSize);
        buf.writeVarLong(this.lerpTime);
        buf.writeVarInt(this.newAbsoluteMaxSize);
        buf.writeVarInt(this.warningBlocks);
        buf.writeVarInt(this.warningTime);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleInitializeBorder(this);
    }

    public double getNewCenterX() {
        return this.newCenterX;
    }

    public double getNewCenterZ() {
        return this.newCenterZ;
    }

    public double getNewSize() {
        return this.newSize;
    }

    public double getOldSize() {
        return this.oldSize;
    }

    public long getLerpTime() {
        return this.lerpTime;
    }

    public int getNewAbsoluteMaxSize() {
        return this.newAbsoluteMaxSize;
    }

    public int getWarningTime() {
        return this.warningTime;
    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }
}
