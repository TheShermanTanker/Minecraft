package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutCollect implements Packet<PacketListenerPlayOut> {
    private final int itemId;
    private final int playerId;
    private final int amount;

    public PacketPlayOutCollect(int entityId, int collectorId, int stackAmount) {
        this.itemId = entityId;
        this.playerId = collectorId;
        this.amount = stackAmount;
    }

    public PacketPlayOutCollect(PacketDataSerializer buf) {
        this.itemId = buf.readVarInt();
        this.playerId = buf.readVarInt();
        this.amount = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.itemId);
        buf.writeVarInt(this.playerId);
        buf.writeVarInt(this.amount);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleTakeItemEntity(this);
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public int getAmount() {
        return this.amount;
    }
}
