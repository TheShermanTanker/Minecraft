package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInEntityNBTQuery implements Packet<PacketListenerPlayIn> {
    private final int transactionId;
    private final int entityId;

    public PacketPlayInEntityNBTQuery(int transactionId, int entityId) {
        this.transactionId = transactionId;
        this.entityId = entityId;
    }

    public PacketPlayInEntityNBTQuery(PacketDataSerializer buf) {
        this.transactionId = buf.readVarInt();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeVarInt(this.entityId);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleEntityTagQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public int getEntityId() {
        return this.entityId;
    }
}
