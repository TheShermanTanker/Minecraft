package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutCombatEnter implements Packet<PacketListenerPlayOut> {
    public PacketPlayOutCombatEnter() {
    }

    public PacketPlayOutCombatEnter(PacketDataSerializer buf) {
    }

    @Override
    public void write(PacketDataSerializer buf) {
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlayerCombatEnter(this);
    }
}
