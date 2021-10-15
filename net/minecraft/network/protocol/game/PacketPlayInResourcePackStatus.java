package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInResourcePackStatus implements Packet<PacketListenerPlayIn> {
    public final PacketPlayInResourcePackStatus.EnumResourcePackStatus action;

    public PacketPlayInResourcePackStatus(PacketPlayInResourcePackStatus.EnumResourcePackStatus status) {
        this.action = status;
    }

    public PacketPlayInResourcePackStatus(PacketDataSerializer buf) {
        this.action = buf.readEnum(PacketPlayInResourcePackStatus.EnumResourcePackStatus.class);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleResourcePackResponse(this);
    }

    public PacketPlayInResourcePackStatus.EnumResourcePackStatus getAction() {
        return this.action;
    }

    public static enum EnumResourcePackStatus {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED;
    }
}
