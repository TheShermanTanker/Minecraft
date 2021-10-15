package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutUpdateHealth implements Packet<PacketListenerPlayOut> {
    private final float health;
    private final int food;
    private final float saturation;

    public PacketPlayOutUpdateHealth(float health, int food, float saturation) {
        this.health = health;
        this.food = food;
        this.saturation = saturation;
    }

    public PacketPlayOutUpdateHealth(PacketDataSerializer buf) {
        this.health = buf.readFloat();
        this.food = buf.readVarInt();
        this.saturation = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeFloat(this.health);
        buf.writeVarInt(this.food);
        buf.writeFloat(this.saturation);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetHealth(this);
    }

    public float getHealth() {
        return this.health;
    }

    public int getFood() {
        return this.food;
    }

    public float getSaturation() {
        return this.saturation;
    }
}
