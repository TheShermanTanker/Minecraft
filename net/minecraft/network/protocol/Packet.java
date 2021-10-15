package net.minecraft.network.protocol;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.PacketListener;

public interface Packet<T extends PacketListener> {
    void write(PacketDataSerializer buf);

    void handle(T listener);

    default boolean isSkippable() {
        return false;
    }
}
