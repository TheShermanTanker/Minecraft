package net.minecraft.network.protocol.game;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;

public class PacketPlayInSpectate implements Packet<PacketListenerPlayIn> {
    private final UUID uuid;

    public PacketPlayInSpectate(UUID targetUuid) {
        this.uuid = targetUuid;
    }

    public PacketPlayInSpectate(PacketDataSerializer buf) {
        this.uuid = buf.readUUID();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUUID(this.uuid);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleTeleportToEntityPacket(this);
    }

    @Nullable
    public Entity getEntity(WorldServer world) {
        return world.getEntity(this.uuid);
    }
}
