package net.minecraft.world.level.timers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;

@FunctionalInterface
public interface CustomFunctionCallbackTimer<T> {
    void handle(T server, CustomFunctionCallbackTimerQueue<T> events, long time);

    public abstract static class Serializer<T, C extends CustomFunctionCallbackTimer<T>> {
        private final MinecraftKey id;
        private final Class<?> cls;

        public Serializer(MinecraftKey id, Class<?> callbackClass) {
            this.id = id;
            this.cls = callbackClass;
        }

        public MinecraftKey getId() {
            return this.id;
        }

        public Class<?> getCls() {
            return this.cls;
        }

        public abstract void serialize(NBTTagCompound nbt, C callback);

        public abstract C deserialize(NBTTagCompound nbt);
    }
}
