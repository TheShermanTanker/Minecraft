package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketListener;

public interface PacketLoginOutListener extends PacketListener {
    void handleHello(PacketLoginOutEncryptionBegin packet);

    void handleGameProfile(PacketLoginOutSuccess packet);

    void handleDisconnect(PacketLoginOutDisconnect packet);

    void handleCompression(PacketLoginOutSetCompression packet);

    void handleCustomQuery(PacketLoginOutCustomPayload packet);
}
