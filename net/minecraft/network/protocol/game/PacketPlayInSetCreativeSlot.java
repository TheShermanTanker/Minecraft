package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class PacketPlayInSetCreativeSlot implements Packet<PacketListenerPlayIn> {
    private final int slotNum;
    private final ItemStack itemStack;

    public PacketPlayInSetCreativeSlot(int slot, ItemStack stack) {
        this.slotNum = slot;
        this.itemStack = stack.cloneItemStack();
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetCreativeModeSlot(this);
    }

    public PacketPlayInSetCreativeSlot(PacketDataSerializer buf) {
        this.slotNum = buf.readShort();
        this.itemStack = buf.readItem();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeShort(this.slotNum);
        buf.writeItem(this.itemStack);
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }
}
