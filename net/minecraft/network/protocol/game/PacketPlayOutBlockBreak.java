package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record PacketPlayOutBlockBreak(BlockPosition pos, IBlockData state, PacketPlayInBlockDig.EnumPlayerDigType action, boolean allGood) implements Packet<PacketListenerPlayOut> {
    private static final Logger LOGGER = LogManager.getLogger();

    public PacketPlayOutBlockBreak(BlockPosition pos, IBlockData state, PacketPlayInBlockDig.EnumPlayerDigType action, boolean approved, String reason) {
        this(pos, state, action, approved);
    }

    public PacketPlayOutBlockBreak(BlockPosition pos, IBlockData state, PacketPlayInBlockDig.EnumPlayerDigType action, boolean approved) {
        pos = pos.immutableCopy();
        this.pos = pos;
        this.state = state;
        this.action = action;
        this.allGood = approved;
    }

    public PacketPlayOutBlockBreak(PacketDataSerializer buf) {
        this(buf.readBlockPos(), Block.BLOCK_STATE_REGISTRY.fromId(buf.readVarInt()), buf.readEnum(PacketPlayInBlockDig.EnumPlayerDigType.class), buf.readBoolean());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Block.getCombinedId(this.state));
        buf.writeEnum(this.action);
        buf.writeBoolean(this.allGood);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockBreakAck(this);
    }

    public BlockPosition pos() {
        return this.pos;
    }

    public IBlockData state() {
        return this.state;
    }

    public PacketPlayInBlockDig.EnumPlayerDigType action() {
        return this.action;
    }

    public boolean allGood() {
        return this.allGood;
    }
}
