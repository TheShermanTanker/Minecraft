package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInBlockDig implements Packet<PacketListenerPlayIn> {
    private final BlockPosition pos;
    private final EnumDirection direction;
    private final PacketPlayInBlockDig.EnumPlayerDigType action;

    public PacketPlayInBlockDig(PacketPlayInBlockDig.EnumPlayerDigType action, BlockPosition pos, EnumDirection direction) {
        this.action = action;
        this.pos = pos.immutableCopy();
        this.direction = direction;
    }

    public PacketPlayInBlockDig(PacketDataSerializer buf) {
        this.action = buf.readEnum(PacketPlayInBlockDig.EnumPlayerDigType.class);
        this.pos = buf.readBlockPos();
        this.direction = EnumDirection.fromType1(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.action);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.direction.get3DDataValue());
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePlayerAction(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public EnumDirection getDirection() {
        return this.direction;
    }

    public PacketPlayInBlockDig.EnumPlayerDigType getAction() {
        return this.action;
    }

    public static enum EnumPlayerDigType {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM,
        SWAP_ITEM_WITH_OFFHAND;
    }
}
