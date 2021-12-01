package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatHoverable {
    static final Logger LOGGER = LogManager.getLogger();
    private final ChatHoverable.EnumHoverAction<?> action;
    private final Object value;

    public <T> ChatHoverable(ChatHoverable.EnumHoverAction<T> action, T contents) {
        this.action = action;
        this.value = contents;
    }

    public ChatHoverable.EnumHoverAction<?> getAction() {
        return this.action;
    }

    @Nullable
    public <T> T getValue(ChatHoverable.EnumHoverAction<T> action) {
        return (T)(this.action == action ? action.cast(this.value) : null);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            ChatHoverable hoverEvent = (ChatHoverable)object;
            return this.action == hoverEvent.action && Objects.equals(this.value, hoverEvent.value);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "HoverEvent{action=" + this.action + ", value='" + this.value + "'}";
    }

    @Override
    public int hashCode() {
        int i = this.action.hashCode();
        return 31 * i + (this.value != null ? this.value.hashCode() : 0);
    }

    @Nullable
    public static ChatHoverable deserialize(JsonObject json) {
        String string = ChatDeserializer.getAsString(json, "action", (String)null);
        if (string == null) {
            return null;
        } else {
            ChatHoverable.EnumHoverAction<?> action = ChatHoverable.EnumHoverAction.getByName(string);
            if (action == null) {
                return null;
            } else {
                JsonElement jsonElement = json.get("contents");
                if (jsonElement != null) {
                    return action.deserialize(jsonElement);
                } else {
                    IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(json.get("value"));
                    return component != null ? action.deserializeFromLegacy(component) : null;
                }
            }
        }
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", this.action.getName());
        jsonObject.add("contents", this.action.serializeArg(this.value));
        return jsonObject;
    }

    public static class EntityTooltipInfo {
        public final EntityTypes<?> type;
        public final UUID id;
        @Nullable
        public final IChatBaseComponent name;
        @Nullable
        private List<IChatBaseComponent> linesCache;

        public EntityTooltipInfo(EntityTypes<?> entityType, UUID uuid, @Nullable IChatBaseComponent name) {
            this.type = entityType;
            this.id = uuid;
            this.name = name;
        }

        @Nullable
        public static ChatHoverable.EntityTooltipInfo create(JsonElement json) {
            if (!json.isJsonObject()) {
                return null;
            } else {
                JsonObject jsonObject = json.getAsJsonObject();
                EntityTypes<?> entityType = IRegistry.ENTITY_TYPE.get(new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "type")));
                UUID uUID = UUID.fromString(ChatDeserializer.getAsString(jsonObject, "id"));
                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(jsonObject.get("name"));
                return new ChatHoverable.EntityTooltipInfo(entityType, uUID, component);
            }
        }

        @Nullable
        public static ChatHoverable.EntityTooltipInfo create(IChatBaseComponent text) {
            try {
                NBTTagCompound compoundTag = MojangsonParser.parse(text.getString());
                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("name"));
                EntityTypes<?> entityType = IRegistry.ENTITY_TYPE.get(new MinecraftKey(compoundTag.getString("type")));
                UUID uUID = UUID.fromString(compoundTag.getString("id"));
                return new ChatHoverable.EntityTooltipInfo(entityType, uUID, component);
            } catch (CommandSyntaxException | JsonSyntaxException var5) {
                return null;
            }
        }

        public JsonElement serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", IRegistry.ENTITY_TYPE.getKey(this.type).toString());
            jsonObject.addProperty("id", this.id.toString());
            if (this.name != null) {
                jsonObject.add("name", IChatBaseComponent.ChatSerializer.toJsonTree(this.name));
            }

            return jsonObject;
        }

        public List<IChatBaseComponent> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = Lists.newArrayList();
                if (this.name != null) {
                    this.linesCache.add(this.name);
                }

                this.linesCache.add(new ChatMessage("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(new ChatComponentText(this.id.toString()));
            }

            return this.linesCache;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ChatHoverable.EntityTooltipInfo entityTooltipInfo = (ChatHoverable.EntityTooltipInfo)object;
                return this.type.equals(entityTooltipInfo.type) && this.id.equals(entityTooltipInfo.id) && Objects.equals(this.name, entityTooltipInfo.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.type.hashCode();
            i = 31 * i + this.id.hashCode();
            return 31 * i + (this.name != null ? this.name.hashCode() : 0);
        }
    }

    public static class EnumHoverAction<T> {
        public static final ChatHoverable.EnumHoverAction<IChatBaseComponent> SHOW_TEXT = new ChatHoverable.EnumHoverAction<>("show_text", true, IChatBaseComponent.ChatSerializer::fromJson, IChatBaseComponent.ChatSerializer::toJsonTree, Function.identity());
        public static final ChatHoverable.EnumHoverAction<ChatHoverable.ItemStackInfo> SHOW_ITEM = new ChatHoverable.EnumHoverAction<>("show_item", true, ChatHoverable.ItemStackInfo::create, ChatHoverable.ItemStackInfo::serialize, ChatHoverable.ItemStackInfo::create);
        public static final ChatHoverable.EnumHoverAction<ChatHoverable.EntityTooltipInfo> SHOW_ENTITY = new ChatHoverable.EnumHoverAction<>("show_entity", true, ChatHoverable.EntityTooltipInfo::create, ChatHoverable.EntityTooltipInfo::serialize, ChatHoverable.EntityTooltipInfo::create);
        private static final Map<String, ChatHoverable.EnumHoverAction<?>> LOOKUP = Stream.of(SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY).collect(ImmutableMap.toImmutableMap(ChatHoverable.EnumHoverAction::getName, (action) -> {
            return action;
        }));
        private final String name;
        private final boolean allowFromServer;
        private final Function<JsonElement, T> argDeserializer;
        private final Function<T, JsonElement> argSerializer;
        private final Function<IChatBaseComponent, T> legacyArgDeserializer;

        public EnumHoverAction(String name, boolean parsable, Function<JsonElement, T> deserializer, Function<T, JsonElement> serializer, Function<IChatBaseComponent, T> legacyDeserializer) {
            this.name = name;
            this.allowFromServer = parsable;
            this.argDeserializer = deserializer;
            this.argSerializer = serializer;
            this.legacyArgDeserializer = legacyDeserializer;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public static ChatHoverable.EnumHoverAction<?> getByName(String name) {
            return LOOKUP.get(name);
        }

        T cast(Object o) {
            return (T)o;
        }

        @Nullable
        public ChatHoverable deserialize(JsonElement contents) {
            T object = this.argDeserializer.apply(contents);
            return object == null ? null : new ChatHoverable(this, object);
        }

        @Nullable
        public ChatHoverable deserializeFromLegacy(IChatBaseComponent value) {
            T object = this.legacyArgDeserializer.apply(value);
            return object == null ? null : new ChatHoverable(this, object);
        }

        public JsonElement serializeArg(Object contents) {
            return this.argSerializer.apply(this.cast(contents));
        }

        @Override
        public String toString() {
            return "<action " + this.name + ">";
        }
    }

    public static class ItemStackInfo {
        private final Item item;
        private final int count;
        @Nullable
        private final NBTTagCompound tag;
        @Nullable
        private ItemStack itemStack;

        ItemStackInfo(Item item, int count, @Nullable NBTTagCompound nbt) {
            this.item = item;
            this.count = count;
            this.tag = nbt;
        }

        public ItemStackInfo(ItemStack stack) {
            this(stack.getItem(), stack.getCount(), stack.getTag() != null ? stack.getTag().copy() : null);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ChatHoverable.ItemStackInfo itemStackInfo = (ChatHoverable.ItemStackInfo)object;
                return this.count == itemStackInfo.count && this.item.equals(itemStackInfo.item) && Objects.equals(this.tag, itemStackInfo.tag);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.item.hashCode();
            i = 31 * i + this.count;
            return 31 * i + (this.tag != null ? this.tag.hashCode() : 0);
        }

        public ItemStack getItemStack() {
            if (this.itemStack == null) {
                this.itemStack = new ItemStack(this.item, this.count);
                if (this.tag != null) {
                    this.itemStack.setTag(this.tag);
                }
            }

            return this.itemStack;
        }

        private static ChatHoverable.ItemStackInfo create(JsonElement json) {
            if (json.isJsonPrimitive()) {
                return new ChatHoverable.ItemStackInfo(IRegistry.ITEM.get(new MinecraftKey(json.getAsString())), 1, (NBTTagCompound)null);
            } else {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "item");
                Item item = IRegistry.ITEM.get(new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "id")));
                int i = ChatDeserializer.getAsInt(jsonObject, "count", 1);
                if (jsonObject.has("tag")) {
                    String string = ChatDeserializer.getAsString(jsonObject, "tag");

                    try {
                        NBTTagCompound compoundTag = MojangsonParser.parse(string);
                        return new ChatHoverable.ItemStackInfo(item, i, compoundTag);
                    } catch (CommandSyntaxException var6) {
                        ChatHoverable.LOGGER.warn("Failed to parse tag: {}", string, var6);
                    }
                }

                return new ChatHoverable.ItemStackInfo(item, i, (NBTTagCompound)null);
            }
        }

        @Nullable
        private static ChatHoverable.ItemStackInfo create(IChatBaseComponent text) {
            try {
                NBTTagCompound compoundTag = MojangsonParser.parse(text.getString());
                return new ChatHoverable.ItemStackInfo(ItemStack.of(compoundTag));
            } catch (CommandSyntaxException var2) {
                ChatHoverable.LOGGER.warn("Failed to parse item tag: {}", text, var2);
                return null;
            }
        }

        private JsonElement serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", IRegistry.ITEM.getKey(this.item).toString());
            if (this.count != 1) {
                jsonObject.addProperty("count", this.count);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", this.tag.toString());
            }

            return jsonObject;
        }
    }
}
