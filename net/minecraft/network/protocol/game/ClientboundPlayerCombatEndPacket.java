package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<PacketListenerPlayOut> {
    private final int killerId;
    private final int duration;

    public ClientboundPlayerCombatEndPacket(CombatTracker damageTracker) {
        this(damageTracker.getKillerId(), damageTracker.getCombatDuration());
    }

    public ClientboundPlayerCombatEndPacket(int attackerId, int timeSinceLastAttack) {
        this.killerId = attackerId;
        this.duration = timeSinceLastAttack;
    }

    public ClientboundPlayerCombatEndPacket(PacketDataSerializer buf) {
        this.duration = buf.readVarInt();
        this.killerId = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.duration);
        buf.writeInt(this.killerId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlayerCombatEnd(this);
    }
}
