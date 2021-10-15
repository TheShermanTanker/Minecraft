package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutTileEntityData implements Packet<PacketListenerPlayOut> {
    public static final int TYPE_MOB_SPAWNER = 1;
    public static final int TYPE_ADV_COMMAND = 2;
    public static final int TYPE_BEACON = 3;
    public static final int TYPE_SKULL = 4;
    public static final int TYPE_CONDUIT = 5;
    public static final int TYPE_BANNER = 6;
    public static final int TYPE_STRUCT_COMMAND = 7;
    public static final int TYPE_END_GATEWAY = 8;
    public static final int TYPE_SIGN = 9;
    public static final int TYPE_BED = 11;
    public static final int TYPE_JIGSAW = 12;
    public static final int TYPE_CAMPFIRE = 13;
    public static final int TYPE_BEEHIVE = 14;
    private final BlockPosition pos;
    private final int type;
    private final NBTTagCompound tag;

    public PacketPlayOutTileEntityData(BlockPosition pos, int blockEntityType, NBTTagCompound nbt) {
        this.pos = pos;
        this.type = blockEntityType;
        this.tag = nbt;
    }

    public PacketPlayOutTileEntityData(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.type = buf.readUnsignedByte();
        this.tag = buf.readNbt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte((byte)this.type);
        buf.writeNbt(this.tag);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockEntityData(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public int getType() {
        return this.type;
    }

    public NBTTagCompound getTag() {
        return this.tag;
    }
}
