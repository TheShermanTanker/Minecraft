package net.minecraft.network.protocol.game;

import java.util.Map;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.Tags;

public class PacketPlayOutTags implements Packet<PacketListenerPlayOut> {
    private final Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> tags;

    public PacketPlayOutTags(Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> groups) {
        this.tags = groups;
    }

    public PacketPlayOutTags(PacketDataSerializer buf) {
        this.tags = buf.readMap((bufx) -> {
            return ResourceKey.createRegistryKey(bufx.readResourceLocation());
        }, Tags.NetworkPayload::read);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeMap(this.tags, (bufx, registryKey) -> {
            bufx.writeResourceLocation(registryKey.location());
        }, (bufx, serializedGroup) -> {
            serializedGroup.write(bufx);
        });
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleUpdateTags(this);
    }

    public Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> getTags() {
        return this.tags;
    }
}
