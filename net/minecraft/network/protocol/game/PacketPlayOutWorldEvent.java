package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutWorldEvent implements Packet<PacketListenerPlayOut> {
    private final int type;
    private final BlockPosition pos;
    private final int data;
    private final boolean globalEvent;

    public PacketPlayOutWorldEvent(int eventId, BlockPosition pos, int data, boolean global) {
        this.type = eventId;
        this.pos = pos.immutableCopy();
        this.data = data;
        this.globalEvent = global;
    }

    public PacketPlayOutWorldEvent(PacketDataSerializer buf) {
        this.type = buf.readInt();
        this.pos = buf.readBlockPos();
        this.data = buf.readInt();
        this.globalEvent = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.type);
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.data);
        buf.writeBoolean(this.globalEvent);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLevelEvent(this);
    }

    public boolean isGlobalEvent() {
        return this.globalEvent;
    }

    public int getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }

    public BlockPosition getPos() {
        return this.pos;
    }
}
