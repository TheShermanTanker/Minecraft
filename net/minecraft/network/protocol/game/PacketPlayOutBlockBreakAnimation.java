package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutBlockBreakAnimation implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final BlockPosition pos;
    private final int progress;

    public PacketPlayOutBlockBreakAnimation(int entityId, BlockPosition pos, int progress) {
        this.id = entityId;
        this.pos = pos;
        this.progress = progress;
    }

    public PacketPlayOutBlockBreakAnimation(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.pos = buf.readBlockPos();
        this.progress = buf.readUnsignedByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.progress);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockDestruction(this);
    }

    public int getId() {
        return this.id;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public int getProgress() {
        return this.progress;
    }
}
