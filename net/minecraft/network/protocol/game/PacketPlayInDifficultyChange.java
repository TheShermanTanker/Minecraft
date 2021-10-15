package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumDifficulty;

public class PacketPlayInDifficultyChange implements Packet<PacketListenerPlayIn> {
    private final EnumDifficulty difficulty;

    public PacketPlayInDifficultyChange(EnumDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleChangeDifficulty(this);
    }

    public PacketPlayInDifficultyChange(PacketDataSerializer buf) {
        this.difficulty = EnumDifficulty.getById(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.difficulty.getId());
    }

    public EnumDifficulty getDifficulty() {
        return this.difficulty;
    }
}
