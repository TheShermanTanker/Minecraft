package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityExperienceOrb;

public class PacketPlayOutSpawnEntityExperienceOrb implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final int value;

    public PacketPlayOutSpawnEntityExperienceOrb(EntityExperienceOrb experienceOrbEntity) {
        this.id = experienceOrbEntity.getId();
        this.x = experienceOrbEntity.locX();
        this.y = experienceOrbEntity.locY();
        this.z = experienceOrbEntity.locZ();
        this.value = experienceOrbEntity.getValue();
    }

    public PacketPlayOutSpawnEntityExperienceOrb(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.value = buf.readShort();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeShort(this.value);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddExperienceOrb(this);
    }

    public int getId() {
        return this.id;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public int getValue() {
        return this.value;
    }
}
