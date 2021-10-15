package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerInteger implements ArgumentSerializer<IntegerArgumentType> {
    @Override
    public void serializeToNetwork(IntegerArgumentType integerArgumentType, PacketDataSerializer friendlyByteBuf) {
        boolean bl = integerArgumentType.getMinimum() != Integer.MIN_VALUE;
        boolean bl2 = integerArgumentType.getMaximum() != Integer.MAX_VALUE;
        friendlyByteBuf.writeByte(ArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            friendlyByteBuf.writeInt(integerArgumentType.getMinimum());
        }

        if (bl2) {
            friendlyByteBuf.writeInt(integerArgumentType.getMaximum());
        }

    }

    @Override
    public IntegerArgumentType deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        int i = ArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readInt() : Integer.MIN_VALUE;
        int j = ArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readInt() : Integer.MAX_VALUE;
        return IntegerArgumentType.integer(i, j);
    }

    @Override
    public void serializeToJson(IntegerArgumentType integerArgumentType, JsonObject jsonObject) {
        if (integerArgumentType.getMinimum() != Integer.MIN_VALUE) {
            jsonObject.addProperty("min", integerArgumentType.getMinimum());
        }

        if (integerArgumentType.getMaximum() != Integer.MAX_VALUE) {
            jsonObject.addProperty("max", integerArgumentType.getMaximum());
        }

    }
}
