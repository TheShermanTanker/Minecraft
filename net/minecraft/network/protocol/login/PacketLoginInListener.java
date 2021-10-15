package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketListener;

public interface PacketLoginInListener extends PacketListener {
    void handleHello(PacketLoginInStart packet);

    void handleKey(PacketLoginInEncryptionBegin packet);

    void handleCustomQueryPacket(PacketLoginInCustomPayload packet);
}
