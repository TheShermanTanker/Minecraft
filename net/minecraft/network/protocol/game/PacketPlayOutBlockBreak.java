package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketPlayOutBlockBreak implements Packet<PacketListenerPlayOut> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPosition pos;
    private final IBlockData state;
    private final PacketPlayInBlockDig.EnumPlayerDigType action;
    private final boolean allGood;

    public PacketPlayOutBlockBreak(BlockPosition pos, IBlockData state, PacketPlayInBlockDig.EnumPlayerDigType action, boolean approved, String reason) {
        this.pos = pos.immutableCopy();
        this.state = state;
        this.action = action;
        this.allGood = approved;
    }

    public PacketPlayOutBlockBreak(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.state = Block.BLOCK_STATE_REGISTRY.fromId(buf.readVarInt());
        this.action = buf.readEnum(PacketPlayInBlockDig.EnumPlayerDigType.class);
        this.allGood = buf.readBoolean();
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

    public IBlockData getState() {
        return this.state;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public boolean allGood() {
        return this.allGood;
    }

    public PacketPlayInBlockDig.EnumPlayerDigType action() {
        return this.action;
    }
}
