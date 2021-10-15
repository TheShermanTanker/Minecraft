package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.PlayerAbilities;

public class PacketPlayInAbilities implements Packet<PacketListenerPlayIn> {
    private static final int FLAG_FLYING = 2;
    private final boolean isFlying;

    public PacketPlayInAbilities(PlayerAbilities abilities) {
        this.isFlying = abilities.flying;
    }

    public PacketPlayInAbilities(PacketDataSerializer buf) {
        byte b = buf.readByte();
        this.isFlying = (b & 2) != 0;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        byte b = 0;
        if (this.isFlying) {
            b = (byte)(b | 2);
        }

        buf.writeByte(b);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePlayerAbilities(this);
    }

    public boolean isFlying() {
        return this.isFlying;
    }
}
