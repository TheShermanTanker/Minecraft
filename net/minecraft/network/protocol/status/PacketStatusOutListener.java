package net.minecraft.network.protocol.status;

import net.minecraft.network.PacketListener;

public interface PacketStatusOutListener extends PacketListener {
    void handleStatusResponse(PacketStatusOutServerInfo packet);

    void handlePongResponse(PacketStatusOutPong packet);
}
