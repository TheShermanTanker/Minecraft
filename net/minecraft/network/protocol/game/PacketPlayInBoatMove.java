package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInBoatMove implements Packet<PacketListenerPlayIn> {
    private final boolean left;
    private final boolean right;

    public PacketPlayInBoatMove(boolean leftPaddling, boolean rightPaddling) {
        this.left = leftPaddling;
        this.right = rightPaddling;
    }

    public PacketPlayInBoatMove(PacketDataSerializer buf) {
        this.left = buf.readBoolean();
        this.right = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBoolean(this.left);
        buf.writeBoolean(this.right);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePaddleBoat(this);
    }

    public boolean getLeft() {
        return this.left;
    }

    public boolean getRight() {
        return this.right;
    }
}
