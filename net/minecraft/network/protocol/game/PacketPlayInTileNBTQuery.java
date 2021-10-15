package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInTileNBTQuery implements Packet<PacketListenerPlayIn> {
    private final int transactionId;
    private final BlockPosition pos;

    public PacketPlayInTileNBTQuery(int transactionId, BlockPosition pos) {
        this.transactionId = transactionId;
        this.pos = pos;
    }

    public PacketPlayInTileNBTQuery(PacketDataSerializer buf) {
        this.transactionId = buf.readVarInt();
        this.pos = buf.readBlockPos();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleBlockEntityTagQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public BlockPosition getPos() {
        return this.pos;
    }
}
