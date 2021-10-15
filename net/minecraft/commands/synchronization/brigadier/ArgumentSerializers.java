package net.minecraft.commands.synchronization.brigadier;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.commands.synchronization.ArgumentSerializerVoid;

public class ArgumentSerializers {
    private static final byte NUMBER_FLAG_MIN = 1;
    private static final byte NUMBER_FLAG_MAX = 2;

    public static void bootstrap() {
        ArgumentRegistry.register("brigadier:bool", BoolArgumentType.class, new ArgumentSerializerVoid<>(BoolArgumentType::bool));
        ArgumentRegistry.register("brigadier:float", FloatArgumentType.class, new ArgumentSerializerFloat());
        ArgumentRegistry.register("brigadier:double", DoubleArgumentType.class, new ArgumentSerializerDouble());
        ArgumentRegistry.register("brigadier:integer", IntegerArgumentType.class, new ArgumentSerializerInteger());
        ArgumentRegistry.register("brigadier:long", LongArgumentType.class, new ArgumentSerializerLong());
        ArgumentRegistry.register("brigadier:string", StringArgumentType.class, new ArgumentSerializerString());
    }

    public static byte createNumberFlags(boolean hasMin, boolean hasMax) {
        byte b = 0;
        if (hasMin) {
            b = (byte)(b | 1);
        }

        if (hasMax) {
            b = (byte)(b | 2);
        }

        return b;
    }

    public static boolean numberHasMin(byte rangeFlag) {
        return (rangeFlag & 1) != 0;
    }

    public static boolean numberHasMax(byte rangeFlag) {
        return (rangeFlag & 2) != 0;
    }
}
