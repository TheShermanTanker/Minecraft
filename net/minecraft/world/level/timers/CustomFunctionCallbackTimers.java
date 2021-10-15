package net.minecraft.world.level.timers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomFunctionCallbackTimers<C> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final CustomFunctionCallbackTimers<MinecraftServer> SERVER_CALLBACKS = (new CustomFunctionCallbackTimers<MinecraftServer>()).register(new CustomFunctionCallback.Serializer()).register(new CustomFunctionCallbackTag.Serializer());
    private final Map<MinecraftKey, CustomFunctionCallbackTimer.Serializer<C, ?>> idToSerializer = Maps.newHashMap();
    private final Map<Class<?>, CustomFunctionCallbackTimer.Serializer<C, ?>> classToSerializer = Maps.newHashMap();

    public CustomFunctionCallbackTimers<C> register(CustomFunctionCallbackTimer.Serializer<C, ?> serializer) {
        this.idToSerializer.put(serializer.getId(), serializer);
        this.classToSerializer.put(serializer.getCls(), serializer);
        return this;
    }

    private <T extends CustomFunctionCallbackTimer<C>> CustomFunctionCallbackTimer.Serializer<C, T> getSerializer(Class<?> class_) {
        return this.classToSerializer.get(class_);
    }

    public <T extends CustomFunctionCallbackTimer<C>> NBTTagCompound serialize(T callback) {
        CustomFunctionCallbackTimer.Serializer<C, T> serializer = this.getSerializer(callback.getClass());
        NBTTagCompound compoundTag = new NBTTagCompound();
        serializer.serialize(compoundTag, callback);
        compoundTag.setString("Type", serializer.getId().toString());
        return compoundTag;
    }

    @Nullable
    public CustomFunctionCallbackTimer<C> deserialize(NBTTagCompound nbt) {
        MinecraftKey resourceLocation = MinecraftKey.tryParse(nbt.getString("Type"));
        CustomFunctionCallbackTimer.Serializer<C, ?> serializer = this.idToSerializer.get(resourceLocation);
        if (serializer == null) {
            LOGGER.error("Failed to deserialize timer callback: {}", (Object)nbt);
            return null;
        } else {
            try {
                return serializer.deserialize(nbt);
            } catch (Exception var5) {
                LOGGER.error("Failed to deserialize timer callback: {}", nbt, var5);
                return null;
            }
        }
    }
}
