package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.network.PacketDataSerializer;

public interface ArgumentSerializer<T extends ArgumentType<?>> {
    void serializeToNetwork(T type, PacketDataSerializer buf);

    T deserializeFromNetwork(PacketDataSerializer buf);

    void serializeToJson(T type, JsonObject json);
}
