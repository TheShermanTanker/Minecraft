package net.minecraft.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AdvancementDisplay {
    private final IChatBaseComponent title;
    private final IChatBaseComponent description;
    private final ItemStack icon;
    @Nullable
    private final MinecraftKey background;
    private final AdvancementFrameType frame;
    private final boolean showToast;
    private final boolean announceChat;
    private final boolean hidden;
    private float x;
    private float y;

    public AdvancementDisplay(ItemStack icon, IChatBaseComponent title, IChatBaseComponent description, @Nullable MinecraftKey background, AdvancementFrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.background = background;
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceToChat;
        this.hidden = hidden;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public IChatBaseComponent getTitle() {
        return this.title;
    }

    public IChatBaseComponent getDescription() {
        return this.description;
    }

    public ItemStack getIcon() {
        return this.icon;
    }

    @Nullable
    public MinecraftKey getBackground() {
        return this.background;
    }

    public AdvancementFrameType getFrame() {
        return this.frame;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public boolean shouldShowToast() {
        return this.showToast;
    }

    public boolean shouldAnnounceChat() {
        return this.announceChat;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public static AdvancementDisplay fromJson(JsonObject obj) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(obj.get("title"));
        IChatBaseComponent component2 = IChatBaseComponent.ChatSerializer.fromJson(obj.get("description"));
        if (component != null && component2 != null) {
            ItemStack itemStack = getIcon(ChatDeserializer.getAsJsonObject(obj, "icon"));
            MinecraftKey resourceLocation = obj.has("background") ? new MinecraftKey(ChatDeserializer.getAsString(obj, "background")) : null;
            AdvancementFrameType frameType = obj.has("frame") ? AdvancementFrameType.byName(ChatDeserializer.getAsString(obj, "frame")) : AdvancementFrameType.TASK;
            boolean bl = ChatDeserializer.getAsBoolean(obj, "show_toast", true);
            boolean bl2 = ChatDeserializer.getAsBoolean(obj, "announce_to_chat", true);
            boolean bl3 = ChatDeserializer.getAsBoolean(obj, "hidden", false);
            return new AdvancementDisplay(itemStack, component, component2, resourceLocation, frameType, bl, bl2, bl3);
        } else {
            throw new JsonSyntaxException("Both title and description must be set");
        }
    }

    private static ItemStack getIcon(JsonObject json) {
        if (!json.has("item")) {
            throw new JsonSyntaxException("Unsupported icon type, currently only items are supported (add 'item' key)");
        } else {
            Item item = ChatDeserializer.getAsItem(json, "item");
            if (json.has("data")) {
                throw new JsonParseException("Disallowed data tag found");
            } else {
                ItemStack itemStack = new ItemStack(item);
                if (json.has("nbt")) {
                    try {
                        NBTTagCompound compoundTag = MojangsonParser.parse(ChatDeserializer.convertToString(json.get("nbt"), "nbt"));
                        itemStack.setTag(compoundTag);
                    } catch (CommandSyntaxException var4) {
                        throw new JsonSyntaxException("Invalid nbt tag: " + var4.getMessage());
                    }
                }

                return itemStack;
            }
        }
    }

    public void serializeToNetwork(PacketDataSerializer buf) {
        buf.writeComponent(this.title);
        buf.writeComponent(this.description);
        buf.writeItem(this.icon);
        buf.writeEnum(this.frame);
        int i = 0;
        if (this.background != null) {
            i |= 1;
        }

        if (this.showToast) {
            i |= 2;
        }

        if (this.hidden) {
            i |= 4;
        }

        buf.writeInt(i);
        if (this.background != null) {
            buf.writeResourceLocation(this.background);
        }

        buf.writeFloat(this.x);
        buf.writeFloat(this.y);
    }

    public static AdvancementDisplay fromNetwork(PacketDataSerializer buf) {
        IChatBaseComponent component = buf.readComponent();
        IChatBaseComponent component2 = buf.readComponent();
        ItemStack itemStack = buf.readItem();
        AdvancementFrameType frameType = buf.readEnum(AdvancementFrameType.class);
        int i = buf.readInt();
        MinecraftKey resourceLocation = (i & 1) != 0 ? buf.readResourceLocation() : null;
        boolean bl = (i & 2) != 0;
        boolean bl2 = (i & 4) != 0;
        AdvancementDisplay displayInfo = new AdvancementDisplay(itemStack, component, component2, resourceLocation, frameType, bl, false, bl2);
        displayInfo.setLocation(buf.readFloat(), buf.readFloat());
        return displayInfo;
    }

    public JsonElement serializeToJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("icon", this.serializeIcon());
        jsonObject.add("title", IChatBaseComponent.ChatSerializer.toJsonTree(this.title));
        jsonObject.add("description", IChatBaseComponent.ChatSerializer.toJsonTree(this.description));
        jsonObject.addProperty("frame", this.frame.getName());
        jsonObject.addProperty("show_toast", this.showToast);
        jsonObject.addProperty("announce_to_chat", this.announceChat);
        jsonObject.addProperty("hidden", this.hidden);
        if (this.background != null) {
            jsonObject.addProperty("background", this.background.toString());
        }

        return jsonObject;
    }

    private JsonObject serializeIcon() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("item", IRegistry.ITEM.getKey(this.icon.getItem()).toString());
        if (this.icon.hasTag()) {
            jsonObject.addProperty("nbt", this.icon.getTag().toString());
        }

        return jsonObject;
    }
}
