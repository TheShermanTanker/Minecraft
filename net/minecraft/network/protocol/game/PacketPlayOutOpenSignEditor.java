package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutOpenSignEditor implements Packet<PacketListenerPlayOut> {
    private final BlockPosition pos;

    public PacketPlayOutOpenSignEditor(BlockPosition pos) {
        this.pos = pos;
    }

    public PacketPlayOutOpenSignEditor(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleOpenSignEditor(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }
}
