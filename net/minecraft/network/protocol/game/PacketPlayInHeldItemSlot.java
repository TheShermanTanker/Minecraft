package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInHeldItemSlot implements Packet<PacketListenerPlayIn> {
    private final int slot;

    public PacketPlayInHeldItemSlot(int selectedSlot) {
        this.slot = selectedSlot;
    }

    public PacketPlayInHeldItemSlot(PacketDataSerializer buf) {
        this.slot = buf.readShort();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeShort(this.slot);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetCarriedItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
