package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;

public class PacketPlayInAdvancements implements Packet<PacketListenerPlayIn> {
    private final PacketPlayInAdvancements.Status action;
    @Nullable
    private final MinecraftKey tab;

    public PacketPlayInAdvancements(PacketPlayInAdvancements.Status action, @Nullable MinecraftKey tab) {
        this.action = action;
        this.tab = tab;
    }

    public static PacketPlayInAdvancements openedTab(Advancement advancement) {
        return new PacketPlayInAdvancements(PacketPlayInAdvancements.Status.OPENED_TAB, advancement.getName());
    }

    public static PacketPlayInAdvancements closedScreen() {
        return new PacketPlayInAdvancements(PacketPlayInAdvancements.Status.CLOSED_SCREEN, (MinecraftKey)null);
    }

    public PacketPlayInAdvancements(PacketDataSerializer buf) {
        this.action = buf.readEnum(PacketPlayInAdvancements.Status.class);
        if (this.action == PacketPlayInAdvancements.Status.OPENED_TAB) {
            this.tab = buf.readResourceLocation();
        } else {
            this.tab = null;
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.action);
        if (this.action == PacketPlayInAdvancements.Status.OPENED_TAB) {
            buf.writeResourceLocation(this.tab);
        }

    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSeenAdvancements(this);
    }

    public PacketPlayInAdvancements.Status getAction() {
        return this.action;
    }

    @Nullable
    public MinecraftKey getTab() {
        return this.tab;
    }

    public static enum Status {
        OPENED_TAB,
        CLOSED_SCREEN;
    }
}
