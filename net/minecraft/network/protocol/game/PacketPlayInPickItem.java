package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInPickItem implements Packet<PacketListenerPlayIn> {
    private final int slot;

    public PacketPlayInPickItem(int slot) {
        this.slot = slot;
    }

    public PacketPlayInPickItem(PacketDataSerializer buf) {
        this.slot = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.slot);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePickItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
