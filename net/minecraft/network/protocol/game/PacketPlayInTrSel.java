package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInTrSel implements Packet<PacketListenerPlayIn> {
    private final int item;

    public PacketPlayInTrSel(int tradeId) {
        this.item = tradeId;
    }

    public PacketPlayInTrSel(PacketDataSerializer buf) {
        this.item = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.item);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSelectTrade(this);
    }

    public int getItem() {
        return this.item;
    }
}
