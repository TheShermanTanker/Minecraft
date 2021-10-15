package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumHand;

public class PacketPlayInBlockPlace implements Packet<PacketListenerPlayIn> {
    private final EnumHand hand;

    public PacketPlayInBlockPlace(EnumHand hand) {
        this.hand = hand;
    }

    public PacketPlayInBlockPlace(PacketDataSerializer buf) {
        this.hand = buf.readEnum(EnumHand.class);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleUseItem(this);
    }

    public EnumHand getHand() {
        return this.hand;
    }
}
