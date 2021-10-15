package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;

public class PacketPlayOutEntityEquipment implements Packet<PacketListenerPlayOut> {
    private static final byte CONTINUE_MASK = -128;
    private final int entity;
    private final List<Pair<EnumItemSlot, ItemStack>> slots;

    public PacketPlayOutEntityEquipment(int id, List<Pair<EnumItemSlot, ItemStack>> equipmentList) {
        this.entity = id;
        this.slots = equipmentList;
    }

    public PacketPlayOutEntityEquipment(PacketDataSerializer buf) {
        this.entity = buf.readVarInt();
        EnumItemSlot[] equipmentSlots = EnumItemSlot.values();
        this.slots = Lists.newArrayList();

        int i;
        do {
            i = buf.readByte();
            EnumItemSlot equipmentSlot = equipmentSlots[i & 127];
            ItemStack itemStack = buf.readItem();
            this.slots.add(Pair.of(equipmentSlot, itemStack));
        } while((i & -128) != 0);

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entity);
        int i = this.slots.size();

        for(int j = 0; j < i; ++j) {
            Pair<EnumItemSlot, ItemStack> pair = this.slots.get(j);
            EnumItemSlot equipmentSlot = pair.getFirst();
            boolean bl = j != i - 1;
            int k = equipmentSlot.ordinal();
            buf.writeByte(bl ? k | -128 : k);
            buf.writeItem(pair.getSecond());
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetEquipment(this);
    }

    public int getEntity() {
        return this.entity;
    }

    public List<Pair<EnumItemSlot, ItemStack>> getSlots() {
        return this.slots;
    }
}
