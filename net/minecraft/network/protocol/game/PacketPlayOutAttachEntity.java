package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutAttachEntity implements Packet<PacketListenerPlayOut> {
    private final int sourceId;
    private final int destId;

    public PacketPlayOutAttachEntity(Entity attachedEntity, @Nullable Entity holdingEntity) {
        this.sourceId = attachedEntity.getId();
        this.destId = holdingEntity != null ? holdingEntity.getId() : 0;
    }

    public PacketPlayOutAttachEntity(PacketDataSerializer buf) {
        this.sourceId = buf.readInt();
        this.destId = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.sourceId);
        buf.writeInt(this.destId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleEntityLinkPacket(this);
    }

    public int getSourceId() {
        return this.sourceId;
    }

    public int getDestId() {
        return this.destId;
    }
}
