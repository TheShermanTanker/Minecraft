package net.minecraft.server.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;

public interface PlayerConnectionServer {
    EntityPlayer getPlayer();

    void sendPacket(Packet<?> packet);
}
