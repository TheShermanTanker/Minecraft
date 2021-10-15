package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumHand;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class PacketPlayInUseItem implements Packet<PacketListenerPlayIn> {
    private final MovingObjectPositionBlock blockHit;
    private final EnumHand hand;

    public PacketPlayInUseItem(EnumHand hand, MovingObjectPositionBlock blockHitResult) {
        this.hand = hand;
        this.blockHit = blockHitResult;
    }

    public PacketPlayInUseItem(PacketDataSerializer buf) {
        this.hand = buf.readEnum(EnumHand.class);
        this.blockHit = buf.readBlockHitResult();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.hand);
        buf.writeBlockHitResult(this.blockHit);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleUseItemOn(this);
    }

    public EnumHand getHand() {
        return this.hand;
    }

    public MovingObjectPositionBlock getHitResult() {
        return this.blockHit;
    }
}
