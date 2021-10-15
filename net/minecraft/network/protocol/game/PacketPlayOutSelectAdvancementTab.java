package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;

public class PacketPlayOutSelectAdvancementTab implements Packet<PacketListenerPlayOut> {
    @Nullable
    private final MinecraftKey tab;

    public PacketPlayOutSelectAdvancementTab(@Nullable MinecraftKey tabId) {
        this.tab = tabId;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSelectAdvancementsTab(this);
    }

    public PacketPlayOutSelectAdvancementTab(PacketDataSerializer buf) {
        if (buf.readBoolean()) {
            this.tab = buf.readResourceLocation();
        } else {
            this.tab = null;
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBoolean(this.tab != null);
        if (this.tab != null) {
            buf.writeResourceLocation(this.tab);
        }

    }

    @Nullable
    public MinecraftKey getTab() {
        return this.tab;
    }
}
