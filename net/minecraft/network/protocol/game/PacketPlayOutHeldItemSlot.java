package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutHeldItemSlot implements Packet<PacketListenerPlayOut> {
    private final int slot;

    public PacketPlayOutHeldItemSlot(int slot) {
        this.slot = slot;
    }

    public PacketPlayOutHeldItemSlot(PacketDataSerializer buf) {
        this.slot = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.slot);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetCarriedItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
