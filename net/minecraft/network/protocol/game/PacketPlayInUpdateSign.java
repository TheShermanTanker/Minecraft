package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInUpdateSign implements Packet<PacketListenerPlayIn> {
    private static final int MAX_STRING_LENGTH = 384;
    private final BlockPosition pos;
    private final String[] lines;

    public PacketPlayInUpdateSign(BlockPosition pos, String line1, String line2, String line3, String line4) {
        this.pos = pos;
        this.lines = new String[]{line1, line2, line3, line4};
    }

    public PacketPlayInUpdateSign(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.lines = new String[4];

        for(int i = 0; i < 4; ++i) {
            this.lines[i] = buf.readUtf(384);
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);

        for(int i = 0; i < 4; ++i) {
            buf.writeUtf(this.lines[i]);
        }

    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSignUpdate(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public String[] getLines() {
        return this.lines;
    }
}
