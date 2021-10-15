package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.Containers;

public class PacketPlayOutOpenWindow implements Packet<PacketListenerPlayOut> {
    private final int containerId;
    private final int type;
    private final IChatBaseComponent title;

    public PacketPlayOutOpenWindow(int syncId, Containers<?> type, IChatBaseComponent name) {
        this.containerId = syncId;
        this.type = IRegistry.MENU.getId(type);
        this.title = name;
    }

    public PacketPlayOutOpenWindow(PacketDataSerializer buf) {
        this.containerId = buf.readVarInt();
        this.type = buf.readVarInt();
        this.title = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.containerId);
        buf.writeVarInt(this.type);
        buf.writeComponent(this.title);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleOpenScreen(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    @Nullable
    public Containers<?> getType() {
        return IRegistry.MENU.fromId(this.type);
    }

    public IChatBaseComponent getTitle() {
        return this.title;
    }
}
