package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutExperience implements Packet<PacketListenerPlayOut> {
    private final float experienceProgress;
    private final int totalExperience;
    private final int experienceLevel;

    public PacketPlayOutExperience(float barProgress, int experienceLevel, int experience) {
        this.experienceProgress = barProgress;
        this.totalExperience = experienceLevel;
        this.experienceLevel = experience;
    }

    public PacketPlayOutExperience(PacketDataSerializer buf) {
        this.experienceProgress = buf.readFloat();
        this.experienceLevel = buf.readVarInt();
        this.totalExperience = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeFloat(this.experienceProgress);
        buf.writeVarInt(this.experienceLevel);
        buf.writeVarInt(this.totalExperience);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetExperience(this);
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }
}
