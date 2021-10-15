package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutOpenWindowHorse implements Packet<PacketListenerPlayOut> {
    private final int containerId;
    private final int size;
    private final int entityId;

    public PacketPlayOutOpenWindowHorse(int syncId, int slotCount, int horseId) {
        this.containerId = syncId;
        this.size = slotCount;
        this.entityId = horseId;
    }

    public PacketPlayOutOpenWindowHorse(PacketDataSerializer buf) {
        this.containerId = buf.readUnsignedByte();
        this.size = buf.readVarInt();
        this.entityId = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeVarInt(this.size);
        buf.writeInt(this.entityId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleHorseScreenOpen(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSize() {
        return this.size;
    }

    public int getEntityId() {
        return this.entityId;
    }
}
