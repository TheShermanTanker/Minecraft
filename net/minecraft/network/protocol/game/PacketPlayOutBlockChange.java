package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class PacketPlayOutBlockChange implements Packet<PacketListenerPlayOut> {
    private final BlockPosition pos;
    public final IBlockData blockState;

    public PacketPlayOutBlockChange(BlockPosition pos, IBlockData state) {
        this.pos = pos;
        this.blockState = state;
    }

    public PacketPlayOutBlockChange(IBlockAccess world, BlockPosition pos) {
        this(pos, world.getType(pos));
    }

    public PacketPlayOutBlockChange(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.blockState = Block.BLOCK_STATE_REGISTRY.fromId(buf.readVarInt());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Block.getCombinedId(this.blockState));
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockUpdate(this);
    }

    public IBlockData getBlockState() {
        return this.blockState;
    }

    public BlockPosition getPos() {
        return this.pos;
    }
}
