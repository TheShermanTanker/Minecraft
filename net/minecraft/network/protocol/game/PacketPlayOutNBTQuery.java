package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutNBTQuery implements Packet<PacketListenerPlayOut> {
    private final int transactionId;
    @Nullable
    private final NBTTagCompound tag;

    public PacketPlayOutNBTQuery(int transactionId, @Nullable NBTTagCompound nbt) {
        this.transactionId = transactionId;
        this.tag = nbt;
    }

    public PacketPlayOutNBTQuery(PacketDataSerializer buf) {
        this.transactionId = buf.readVarInt();
        this.tag = buf.readNbt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeNbt(this.tag);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleTagQueryPacket(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    @Nullable
    public NBTTagCompound getTag() {
        return this.tag;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
