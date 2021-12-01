package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerVoid<T extends ArgumentType<?>> implements ArgumentSerializer<T> {
    private final Supplier<T> constructor;

    public ArgumentSerializerVoid(Supplier<T> supplier) {
        this.constructor = supplier;
    }

    @Override
    public void serializeToNetwork(T type, PacketDataSerializer buf) {
    }

    @Override
    public T deserializeFromNetwork(PacketDataSerializer buf) {
        return this.constructor.get();
    }

    @Override
    public void serializeToJson(T type, JsonObject json) {
    }
}
