package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.Item;

public class PacketPlayOutSetCooldown implements Packet<PacketListenerPlayOut> {
    private final Item item;
    private final int duration;

    public PacketPlayOutSetCooldown(Item item, int cooldown) {
        this.item = item;
        this.duration = cooldown;
    }

    public PacketPlayOutSetCooldown(PacketDataSerializer buf) {
        this.item = Item.getById(buf.readVarInt());
        this.duration = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(Item.getId(this.item));
        buf.writeVarInt(this.duration);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleItemCooldown(this);
    }

    public Item getItem() {
        return this.item;
    }

    public int getDuration() {
        return this.duration;
    }
}
