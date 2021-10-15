package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class PacketPlayOutWindowItems implements Packet<PacketListenerPlayOut> {
    private final int containerId;
    private final int stateId;
    private final List<ItemStack> items;
    private final ItemStack carriedItem;

    public PacketPlayOutWindowItems(int syncId, int revision, NonNullList<ItemStack> contents, ItemStack cursorStack) {
        this.containerId = syncId;
        this.stateId = revision;
        this.items = NonNullList.withSize(contents.size(), ItemStack.EMPTY);

        for(int i = 0; i < contents.size(); ++i) {
            this.items.set(i, contents.get(i).cloneItemStack());
        }

        this.carriedItem = cursorStack.cloneItemStack();
    }

    public PacketPlayOutWindowItems(PacketDataSerializer buf) {
        this.containerId = buf.readUnsignedByte();
        this.stateId = buf.readVarInt();
        this.items = buf.readCollection(NonNullList::createWithCapacity, PacketDataSerializer::readItem);
        this.carriedItem = buf.readItem();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeVarInt(this.stateId);
        buf.writeCollection(this.items, PacketDataSerializer::writeItem);
        buf.writeItem(this.carriedItem);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleContainerContent(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public int getStateId() {
        return this.stateId;
    }
}
