package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.network.PacketDataSerializer;

public interface ArgumentSerializer<T extends ArgumentType<?>> {
    void serializeToNetwork(T argumentType, PacketDataSerializer friendlyByteBuf);

    T deserializeFromNetwork(PacketDataSerializer friendlyByteBuf);

    void serializeToJson(T argumentType, JsonObject jsonObject);
}
