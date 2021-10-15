package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerDouble implements ArgumentSerializer<DoubleArgumentType> {
    @Override
    public void serializeToNetwork(DoubleArgumentType doubleArgumentType, PacketDataSerializer friendlyByteBuf) {
        boolean bl = doubleArgumentType.getMinimum() != -Double.MAX_VALUE;
        boolean bl2 = doubleArgumentType.getMaximum() != Double.MAX_VALUE;
        friendlyByteBuf.writeByte(ArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            friendlyByteBuf.writeDouble(doubleArgumentType.getMinimum());
        }

        if (bl2) {
            friendlyByteBuf.writeDouble(doubleArgumentType.getMaximum());
        }

    }

    @Override
    public DoubleArgumentType deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        double d = ArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readDouble() : -Double.MAX_VALUE;
        double e = ArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readDouble() : Double.MAX_VALUE;
        return DoubleArgumentType.doubleArg(d, e);
    }

    @Override
    public void serializeToJson(DoubleArgumentType doubleArgumentType, JsonObject jsonObject) {
        if (doubleArgumentType.getMinimum() != -Double.MAX_VALUE) {
            jsonObject.addProperty("min", doubleArgumentType.getMinimum());
        }

        if (doubleArgumentType.getMaximum() != Double.MAX_VALUE) {
            jsonObject.addProperty("max", doubleArgumentType.getMaximum());
        }

    }
}
