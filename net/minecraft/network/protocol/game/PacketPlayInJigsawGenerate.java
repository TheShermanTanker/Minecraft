package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInJigsawGenerate implements Packet<PacketListenerPlayIn> {
    private final BlockPosition pos;
    private final int levels;
    private final boolean keepJigsaws;

    public PacketPlayInJigsawGenerate(BlockPosition pos, int maxDepth, boolean keepJigsaws) {
        this.pos = pos;
        this.levels = maxDepth;
        this.keepJigsaws = keepJigsaws;
    }

    public PacketPlayInJigsawGenerate(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.levels = buf.readVarInt();
        this.keepJigsaws = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.levels);
        buf.writeBoolean(this.keepJigsaws);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleJigsawGenerate(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public int levels() {
        return this.levels;
    }

    public boolean keepJigsaws() {
        return this.keepJigsaws;
    }
}
