package net.minecraft.world.level.timers;

import net.minecraft.commands.CustomFunction;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.CustomFunctionData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.Tag;

public class CustomFunctionCallbackTag implements CustomFunctionCallbackTimer<MinecraftServer> {
    final MinecraftKey tagId;

    public CustomFunctionCallbackTag(MinecraftKey name) {
        this.tagId = name;
    }

    @Override
    public void handle(MinecraftServer server, CustomFunctionCallbackTimerQueue<MinecraftServer> events, long time) {
        CustomFunctionData serverFunctionManager = server.getFunctionData();
        Tag<CustomFunction> tag = serverFunctionManager.getTag(this.tagId);

        for(CustomFunction commandFunction : tag.getTagged()) {
            serverFunctionManager.execute(commandFunction, serverFunctionManager.getGameLoopSender());
        }

    }

    public static class Serializer extends CustomFunctionCallbackTimer.Serializer<MinecraftServer, CustomFunctionCallbackTag> {
        public Serializer() {
            super(new MinecraftKey("function_tag"), CustomFunctionCallbackTag.class);
        }

        @Override
        public void serialize(NBTTagCompound nbt, CustomFunctionCallbackTag callback) {
            nbt.setString("Name", callback.tagId.toString());
        }

        @Override
        public CustomFunctionCallbackTag deserialize(NBTTagCompound compoundTag) {
            MinecraftKey resourceLocation = new MinecraftKey(compoundTag.getString("Name"));
            return new CustomFunctionCallbackTag(resourceLocation);
        }
    }
}
