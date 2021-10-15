package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerLong implements ArgumentSerializer<LongArgumentType> {
    @Override
    public void serializeToNetwork(LongArgumentType longArgumentType, PacketDataSerializer friendlyByteBuf) {
        boolean bl = longArgumentType.getMinimum() != Long.MIN_VALUE;
        boolean bl2 = longArgumentType.getMaximum() != Long.MAX_VALUE;
        friendlyByteBuf.writeByte(ArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            friendlyByteBuf.writeLong(longArgumentType.getMinimum());
        }

        if (bl2) {
            friendlyByteBuf.writeLong(longArgumentType.getMaximum());
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
    public void serializeToJson(LongArgumentType longArgumentType, JsonObject jsonObject) {
        if (longArgumentType.getMinimum() != Long.MIN_VALUE) {
            jsonObject.addProperty("min", longArgumentType.getMinimum());
        }

        if (longArgumentType.getMaximum() != Long.MAX_VALUE) {
            jsonObject.addProperty("max", longArgumentType.getMaximum());
        }

    }
}
