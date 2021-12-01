package net.minecraft.world.level.timers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.CustomFunctionData;
import net.minecraft.server.MinecraftServer;

public class CustomFunctionCallback implements CustomFunctionCallbackTimer<MinecraftServer> {
    final MinecraftKey functionId;

    public CustomFunctionCallback(MinecraftKey name) {
        this.functionId = name;
    }

    @Override
    public void handle(MinecraftServer minecraftServer, CustomFunctionCallbackTimerQueue<MinecraftServer> timerQueue, long l) {
        CustomFunctionData serverFunctionManager = minecraftServer.getFunctionData();
        serverFunctionManager.get(this.functionId).ifPresent((function) -> {
            serverFunctionManager.execute(function, serverFunctionManager.getGameLoopSender());
        });
    }

    public static class Serializer extends CustomFunctionCallbackTimer.Serializer<MinecraftServer, CustomFunctionCallback> {
        public Serializer() {
            super(new MinecraftKey("function"), CustomFunctionCallback.class);
        }

        @Override
        public void serialize(NBTTagCompound nbt, CustomFunctionCallback callback) {
            nbt.setString("Name", callback.functionId.toString());
        }

        @Override
        public CustomFunctionCallback deserialize(NBTTagCompound compoundTag) {
            MinecraftKey resourceLocation = new MinecraftKey(compoundTag.getString("Name"));
            return new CustomFunctionCallback(resourceLocation);
        }
    }
}
