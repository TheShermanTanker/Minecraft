package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInDifficultyLock implements Packet<PacketListenerPlayIn> {
    private final boolean locked;

    public PacketPlayInDifficultyLock(boolean difficultyLocked) {
        this.locked = difficultyLocked;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleLockDifficulty(this);
    }

    public PacketPlayInDifficultyLock(PacketDataSerializer buf) {
        this.locked = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBoolean(this.locked);
    }

    public boolean isLocked() {
        return this.locked;
    }
}
