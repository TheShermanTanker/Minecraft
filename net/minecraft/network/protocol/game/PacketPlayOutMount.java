package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutMount implements Packet<PacketListenerPlayOut> {
    private final int vehicle;
    private final int[] passengers;

    public PacketPlayOutMount(Entity entity) {
        this.vehicle = entity.getId();
        List<Entity> list = entity.getPassengers();
        this.passengers = new int[list.size()];

        for(int i = 0; i < list.size(); ++i) {
            this.passengers[i] = list.get(i).getId();
        }

    }

    public PacketPlayOutMount(PacketDataSerializer buf) {
        this.vehicle = buf.readVarInt();
        this.passengers = buf.readVarIntArray();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.vehicle);
        buf.writeVarIntArray(this.passengers);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetEntityPassengersPacket(this);
    }

    public int[] getPassengers() {
        return this.passengers;
    }

    public int getVehicle() {
        return this.vehicle;
    }
}
