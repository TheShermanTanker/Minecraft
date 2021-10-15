package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;

public class PacketPlayOutAdvancements implements Packet<PacketListenerPlayOut> {
    private final boolean reset;
    private final Map<MinecraftKey, Advancement.SerializedAdvancement> added;
    private final Set<MinecraftKey> removed;
    private final Map<MinecraftKey, AdvancementProgress> progress;

    public PacketPlayOutAdvancements(boolean clearCurrent, Collection<Advancement> toEarn, Set<MinecraftKey> toRemove, Map<MinecraftKey, AdvancementProgress> toSetProgress) {
        this.reset = clearCurrent;
        Builder<MinecraftKey, Advancement.SerializedAdvancement> builder = ImmutableMap.builder();

        for(Advancement advancement : toEarn) {
            builder.put(advancement.getName(), advancement.deconstruct());
        }

        this.added = builder.build();
        this.removed = ImmutableSet.copyOf(toRemove);
        this.progress = ImmutableMap.copyOf(toSetProgress);
    }

    public PacketPlayOutAdvancements(PacketDataSerializer buf) {
        this.reset = buf.readBoolean();
        this.added = buf.readMap(PacketDataSerializer::readResourceLocation, Advancement.SerializedAdvancement::fromNetwork);
        this.removed = buf.readCollection(Sets::newLinkedHashSetWithExpectedSize, PacketDataSerializer::readResourceLocation);
        this.progress = buf.readMap(PacketDataSerializer::readResourceLocation, AdvancementProgress::fromNetwork);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBoolean(this.reset);
        buf.writeMap(this.added, PacketDataSerializer::writeResourceLocation, (bufx, task) -> {
            task.serializeToNetwork(bufx);
        });
        buf.writeCollection(this.removed, PacketDataSerializer::writeResourceLocation);
        buf.writeMap(this.progress, PacketDataSerializer::writeResourceLocation, (bufx, progress) -> {
            progress.serializeToNetwork(bufx);
        });
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleUpdateAdvancementsPacket(this);
    }

    public Map<MinecraftKey, Advancement.SerializedAdvancement> getAdded() {
        return this.added;
    }

    public Set<MinecraftKey> getRemoved() {
        return this.removed;
    }

    public Map<MinecraftKey, AdvancementProgress> getProgress() {
        return this.progress;
    }

    public boolean shouldReset() {
        return this.reset;
    }
}
