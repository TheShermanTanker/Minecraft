package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerLong implements ArgumentSerializer<LongArgumentType> {
    @Override
    public void serializeToNetwork(LongArgumentType type, PacketDataSerializer buf) {
        boolean bl = type.getMinimum() != Long.MIN_VALUE;
        boolean bl2 = type.getMaximum() != Long.MAX_VALUE;
        buf.writeByte(ArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            buf.writeLong(type.getMinimum());
        }

        if (bl2) {
            buf.writeLong(type.getMaximum());
        }

    }

    @Override
    public LongArgumentType deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        long l = ArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readLong() : Long.MIN_VALUE;
        long m = ArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readLong() : Long.MAX_VALUE;
        return LongArgumentType.longArg(l, m);
    }

    @Override
    public void serializeToJson(LongArgumentType type, JsonObject json) {
        if (type.getMinimum() != Long.MIN_VALUE) {
            json.addProperty("min", type.getMinimum());
        }

        if (type.getMaximum() != Long.MAX_VALUE) {
            json.addProperty("max", type.getMaximum());
        }

    }
}
