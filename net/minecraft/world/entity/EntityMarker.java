package net.minecraft.world.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.World;

public class EntityMarker extends Entity {
    private static final String DATA_TAG = "data";
    private NBTTagCompound data = new NBTTagCompound();

    public EntityMarker(EntityTypes<?> type, World world) {
        super(type, world);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
    }

    @Override
    protected void initDatawatcher() {
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        this.data = nbt.getCompound("data");
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.set("data", this.data.c());
    }

    @Override
    public Packet<?> getPacket() {
        throw new IllegalStateException("Markers should never be sent");
    }

    @Override
    protected void addPassenger(Entity passenger) {
        passenger.stopRiding();
    }
}
