package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInTabComplete implements Packet<PacketListenerPlayIn> {
    private final int id;
    private final String command;

    public PacketPlayInTabComplete(int completionId, String partialCommand) {
        this.id = completionId;
        this.command = partialCommand;
    }

    public PacketPlayInTabComplete(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.command = buf.readUtf(32500);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeUtf(this.command, 32500);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleCustomCommandSuggestions(this);
    }

    public int getId() {
        return this.id;
    }

    public String getCommand() {
        return this.command;
    }
}
