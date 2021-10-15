package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumHand;

public class PacketPlayOutOpenBook implements Packet<PacketListenerPlayOut> {
    private final EnumHand hand;

    public PacketPlayOutOpenBook(EnumHand hand) {
        this.hand = hand;
    }

    public PacketPlayOutOpenBook(PacketDataSerializer buf) {
        this.hand = buf.readEnum(EnumHand.class);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleOpenBook(this);
    }

    public EnumHand getHand() {
        return this.hand;
    }
}
