package net.minecraft.server.packs.metadata.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.packs.metadata.ResourcePackMetaParser;
import net.minecraft.util.ChatDeserializer;

public class ResourcePackInfoDeserializer implements ResourcePackMetaParser<ResourcePackInfo> {
    @Override
    public ResourcePackInfo fromJson(JsonObject jsonObject) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(jsonObject.get("description"));
        if (component == null) {
            throw new JsonParseException("Invalid/missing description!");
        } else {
            int i = ChatDeserializer.getAsInt(jsonObject, "pack_format");
            return new ResourcePackInfo(component, i);
        }
    }

    @Override
    public String getMetadataSectionName() {
        return "pack";
    }
}
