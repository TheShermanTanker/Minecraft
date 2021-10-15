package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.EnumDifficulty;

public class PacketPlayOutServerDifficulty implements Packet<PacketListenerPlayOut> {
    private final EnumDifficulty difficulty;
    private final boolean locked;

    public PacketPlayOutServerDifficulty(EnumDifficulty difficulty, boolean difficultyLocked) {
        this.difficulty = difficulty;
        this.locked = difficultyLocked;
    }

    public PacketPlayOutServerDifficulty(PacketDataSerializer buf) {
        this.difficulty = EnumDifficulty.getById(buf.readUnsignedByte());
        this.locked = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.difficulty.getId());
        buf.writeBoolean(this.locked);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleChangeDifficulty(this);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public EnumDifficulty getDifficulty() {
        return this.difficulty;
    }
}
