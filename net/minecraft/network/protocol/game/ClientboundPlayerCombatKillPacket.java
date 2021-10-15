package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatKillPacket implements Packet<PacketListenerPlayOut> {
    private final int playerId;
    private final int killerId;
    private final IChatBaseComponent message;

    public ClientboundPlayerCombatKillPacket(CombatTracker damageTracker, IChatBaseComponent message) {
        this(damageTracker.getMob().getId(), damageTracker.getKillerId(), message);
    }

    public ClientboundPlayerCombatKillPacket(int entityId, int killerId, IChatBaseComponent message) {
        this.playerId = entityId;
        this.killerId = killerId;
        this.message = message;
    }

    public ClientboundPlayerCombatKillPacket(PacketDataSerializer buf) {
        this.playerId = buf.readVarInt();
        this.killerId = buf.readInt();
        this.message = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.playerId);
        buf.writeInt(this.killerId);
        buf.writeComponent(this.message);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlayerCombatKill(this);
    }

    @Override
    public boolean isSkippable() {
        return true;
    }

    public int getKillerId() {
        return this.killerId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public IChatBaseComponent getMessage() {
        return this.message;
    }
}
