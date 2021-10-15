package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.EntityMinecartCommandBlock;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.World;

public class PacketPlayInSetCommandMinecart implements Packet<PacketListenerPlayIn> {
    private final int entity;
    private final String command;
    private final boolean trackOutput;

    public PacketPlayInSetCommandMinecart(int entityId, String command, boolean trackOutput) {
        this.entity = entityId;
        this.command = command;
        this.trackOutput = trackOutput;
    }

    public PacketPlayInSetCommandMinecart(PacketDataSerializer buf) {
        this.entity = buf.readVarInt();
        this.command = buf.readUtf();
        this.trackOutput = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entity);
        buf.writeUtf(this.command);
        buf.writeBoolean(this.trackOutput);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetCommandMinecart(this);
    }

    @Nullable
    public CommandBlockListenerAbstract getCommandBlock(World world) {
        Entity entity = world.getEntity(this.entity);
        return entity instanceof EntityMinecartCommandBlock ? ((EntityMinecartCommandBlock)entity).getCommandBlock() : null;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }
}
