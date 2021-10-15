package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class ClientboundPlayerCombatEnterPacket implements Packet<PacketListenerPlayOut> {
    public ClientboundPlayerCombatEnterPacket() {
    }

    public ClientboundPlayerCombatEnterPacket(PacketDataSerializer buf) {
    }

    @Override
    public void write(PacketDataSerializer buf) {
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlayerCombatEnter(this);
    }
}
