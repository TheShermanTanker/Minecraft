package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.TileEntityCommand;

public class PacketPlayInSetCommandBlock implements Packet<PacketListenerPlayIn> {
    private static final int FLAG_TRACK_OUTPUT = 1;
    private static final int FLAG_CONDITIONAL = 2;
    private static final int FLAG_AUTOMATIC = 4;
    private final BlockPosition pos;
    private final String command;
    private final boolean trackOutput;
    private final boolean conditional;
    private final boolean automatic;
    private final TileEntityCommand.Type mode;

    public PacketPlayInSetCommandBlock(BlockPosition pos, String command, TileEntityCommand.Type type, boolean trackOutput, boolean conditional, boolean alwaysActive) {
        this.pos = pos;
        this.command = command;
        this.trackOutput = trackOutput;
        this.conditional = conditional;
        this.automatic = alwaysActive;
        this.mode = type;
    }

    public PacketPlayInSetCommandBlock(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.command = buf.readUtf();
        this.mode = buf.readEnum(TileEntityCommand.Type.class);
        int i = buf.readByte();
        this.trackOutput = (i & 1) != 0;
        this.conditional = (i & 2) != 0;
        this.automatic = (i & 4) != 0;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.command);
        buf.writeEnum(this.mode);
        int i = 0;
        if (this.trackOutput) {
            i |= 1;
        }

        if (this.conditional) {
            i |= 2;
        }

        if (this.automatic) {
            i |= 4;
        }

        buf.writeByte(i);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetCommandBlock(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public boolean isConditional() {
        return this.conditional;
    }

    public boolean isAutomatic() {
        return this.automatic;
    }

    public TileEntityCommand.Type getMode() {
        return this.mode;
    }
}
