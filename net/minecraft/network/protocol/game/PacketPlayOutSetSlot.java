package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class PacketPlayOutSetSlot implements Packet<PacketListenerPlayOut> {
    public static final int CARRIED_ITEM = -1;
    public static final int PLAYER_INVENTORY = -2;
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack itemStack;

    public PacketPlayOutSetSlot(int syncId, int revision, int slot, ItemStack stack) {
        this.containerId = syncId;
        this.stateId = revision;
        this.slot = slot;
        this.itemStack = stack.cloneItemStack();
    }

    public PacketPlayOutSetSlot(PacketDataSerializer buf) {
        this.containerId = buf.readByte();
        this.stateId = buf.readVarInt();
        this.slot = buf.readShort();
        this.itemStack = buf.readItem();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeVarInt(this.stateId);
        buf.writeShort(this.slot);
        buf.writeItem(this.itemStack);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleContainerSetSlot(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public int getStateId() {
        return this.stateId;
    }
}
