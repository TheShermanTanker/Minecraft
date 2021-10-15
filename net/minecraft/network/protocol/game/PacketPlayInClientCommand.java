package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInClientCommand implements Packet<PacketListenerPlayIn> {
    private final PacketPlayInClientCommand.EnumClientCommand action;

    public PacketPlayInClientCommand(PacketPlayInClientCommand.EnumClientCommand mode) {
        this.action = mode;
    }

    public PacketPlayInClientCommand(PacketDataSerializer buf) {
        this.action = buf.readEnum(PacketPlayInClientCommand.EnumClientCommand.class);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleClientCommand(this);
    }

    public PacketPlayInClientCommand.EnumClientCommand getAction() {
        return this.action;
    }

    public static enum EnumClientCommand {
        PERFORM_RESPAWN,
        REQUEST_STATS;
    }
}
