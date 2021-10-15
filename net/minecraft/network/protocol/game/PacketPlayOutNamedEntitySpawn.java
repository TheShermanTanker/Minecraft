package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.EntityHuman;

public class PacketPlayOutNamedEntitySpawn implements Packet<PacketListenerPlayOut> {
    private final int entityId;
    private final UUID playerId;
    private final double x;
    private final double y;
    private final double z;
    private final byte yRot;
    private final byte xRot;

    public PacketPlayOutNamedEntitySpawn(EntityHuman player) {
        this.entityId = player.getId();
        this.playerId = player.getProfile().getId();
        this.x = player.locX();
        this.y = player.locY();
        this.z = player.locZ();
        this.yRot = (byte)((int)(player.getYRot() * 256.0F / 360.0F));
        this.xRot = (byte)((int)(player.getXRot() * 256.0F / 360.0F));
    }

    public PacketPlayOutNamedEntitySpawn(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        this.playerId = buf.readUUID();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readByte();
        this.xRot = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeUUID(this.playerId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(this.yRot);
        buf.writeByte(this.xRot);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddPlayer(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public byte getyRot() {
        return this.yRot;
    }

    public byte getxRot() {
        return this.xRot;
    }
}
