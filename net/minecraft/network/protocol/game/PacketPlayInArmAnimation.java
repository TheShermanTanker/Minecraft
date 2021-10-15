package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumHand;

public class PacketPlayInArmAnimation implements Packet<PacketListenerPlayIn> {
    private final EnumHand hand;

    public PacketPlayInArmAnimation(EnumHand hand) {
        this.hand = hand;
    }

    public PacketPlayInArmAnimation(PacketDataSerializer buf) {
        this.hand = buf.readEnum(EnumHand.class);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleAnimate(this);
    }

    public EnumHand getHand() {
        return this.hand;
    }
}
