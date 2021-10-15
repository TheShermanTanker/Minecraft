package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class PacketPlayOutEntityStatus implements Packet<PacketListenerPlayOut> {
    private final int entityId;
    private final byte eventId;

    public PacketPlayOutEntityStatus(Entity entity, byte status) {
        this.entityId = entity.getId();
        this.eventId = status;
    }

    public PacketPlayOutEntityStatus(PacketDataSerializer buf) {
        this.entityId = buf.readInt();
        this.eventId = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.eventId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleEntityEvent(this);
    }

    @Nullable
    public Entity getEntity(World world) {
        return world.getEntity(this.entityId);
    }

    public byte getEventId() {
        return this.eventId;
    }
}
