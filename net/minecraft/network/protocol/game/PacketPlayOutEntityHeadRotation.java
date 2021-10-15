package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class PacketPlayOutEntityHeadRotation implements Packet<PacketListenerPlayOut> {
    private final int entityId;
    private final byte yHeadRot;

    public PacketPlayOutEntityHeadRotation(Entity entity, byte headYaw) {
        this.entityId = entity.getId();
        this.yHeadRot = headYaw;
    }

    public PacketPlayOutEntityHeadRotation(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        this.yHeadRot = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.yHeadRot);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleRotateMob(this);
    }

    public Entity getEntity(World world) {
        return world.getEntity(this.entityId);
    }

    public byte getYHeadRot() {
        return this.yHeadRot;
    }
}
