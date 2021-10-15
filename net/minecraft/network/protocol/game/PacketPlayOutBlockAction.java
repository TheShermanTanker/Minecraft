package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;

public class PacketPlayOutBlockAction implements Packet<PacketListenerPlayOut> {
    private final BlockPosition pos;
    private final int b0;
    private final int b1;
    private final Block block;

    public PacketPlayOutBlockAction(BlockPosition pos, Block block, int type, int data) {
        this.pos = pos;
        this.block = block;
        this.b0 = type;
        this.b1 = data;
    }

    public PacketPlayOutBlockAction(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.b0 = buf.readUnsignedByte();
        this.b1 = buf.readUnsignedByte();
        this.block = IRegistry.BLOCK.fromId(buf.readVarInt());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.b0);
        buf.writeByte(this.b1);
        buf.writeVarInt(IRegistry.BLOCK.getId(this.block));
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockEvent(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public int getB0() {
        return this.b0;
    }

    public int getB1() {
        return this.b1;
    }

    public Block getBlock() {
        return this.block;
    }
}
