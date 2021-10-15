package net.minecraft.network.chat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;

public class ChatModifier {
    public static final ChatModifier EMPTY = new ChatModifier((ChatHexColor)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (ChatClickable)null, (ChatHoverable)null, (String)null, (MinecraftKey)null);
    public static final MinecraftKey DEFAULT_FONT = new MinecraftKey("minecraft", "default");
    @Nullable
    final ChatHexColor color;
    @Nullable
    final Boolean bold;
    @Nullable
    final Boolean italic;
    @Nullable
    final Boolean underlined;
    @Nullable
    final Boolean strikethrough;
    @Nullable
    final Boolean obfuscated;
    @Nullable
    final ChatClickable clickEvent;
    @Nullable
    final ChatHoverable hoverEvent;
    @Nullable
    final String insertion;
    @Nullable
    final MinecraftKey font;

    ChatModifier(@Nullable ChatHexColor color, @Nullable Boolean bold, @Nullable Boolean italic, @Nullable Boolean underlined, @Nullable Boolean strikethrough, @Nullable Boolean obfuscated, @Nullable ChatClickable clickEvent, @Nullable ChatHoverable hoverEvent, @Nullable String insertion, @Nullable MinecraftKey font) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
        this.insertion = insertion;
        this.font = font;
    }

    @Nullable
    public ChatHexColor getColor() {
        return this.color;
    }

    public boolean isBold() {
        return this.bold == Boolean.TRUE;
    }

    public boolean isItalic() {
        return this.italic == Boolean.TRUE;
    }

    public boolean isStrikethrough() {
        return this.strikethrough == Boolean.TRUE;
    }

    public boolean isUnderlined() {
        return this.underlined == Boolean.TRUE;
    }

    public boolean isRandom() {
        return this.obfuscated == Boolean.TRUE;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Nullable
    public ChatClickable getClickEvent() {
        return this.clickEvent;
    }

    @Nullable
    public ChatHoverable getHoverEvent() {
        return this.hoverEvent;
    }

    @Nullable
    public String getInsertion() {
        return this.insertion;
    }

    public MinecraftKey getFont() {
        return this.font != null ? this.font : DEFAULT_FONT;
    }

    public ChatModifier setColor(@Nullable ChatHexColor color) {
        return new ChatModifier(color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setColor(@Nullable EnumChatFormat color) {
        return this.setColor(color != null ? ChatHexColor.fromLegacyFormat(color) : null);
    }

    public ChatModifier withColor(int rgbColor) {
        return this.setColor(ChatHexColor.fromRgb(rgbColor));
    }

    public ChatModifier setBold(@Nullable Boolean bold) {
        return new ChatModifier(this.color, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setItalic(@Nullable Boolean italic) {
        return new ChatModifier(this.color, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setUnderline(@Nullable Boolean underline) {
        return new ChatModifier(this.color, this.bold, this.italic, underline, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setStrikethrough(@Nullable Boolean strikethrough) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setRandom(@Nullable Boolean obfuscated) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setChatClickable(@Nullable ChatClickable clickEvent) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setChatHoverable(@Nullable ChatHoverable hoverEvent) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setInsertion(@Nullable String insertion) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, insertion, this.font);
    }

    public ChatModifier withFont(@Nullable MinecraftKey font) {
        return new ChatModifier(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, font);
    }

    public ChatModifier applyFormat(EnumChatFormat formatting) {
        ChatHexColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch(formatting) {
        case OBFUSCATED:
            boolean5 = true;
            break;
        case BOLD:
            boolean_ = true;
            break;
        case STRIKETHROUGH:
            boolean3 = true;
            break;
        case UNDERLINE:
            boolean4 = true;
            break;
        case ITALIC:
            boolean2 = true;
            break;
        case RESET:
            return EMPTY;
        default:
            textColor = ChatHexColor.fromLegacyFormat(formatting);
        }

        return new ChatModifier(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier applyLegacyFormat(EnumChatFormat formatting) {
        ChatHexColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch(formatting) {
        case OBFUSCATED:
            boolean5 = true;
            break;
        case BOLD:
            boolean_ = true;
            break;
        case STRIKETHROUGH:
            boolean3 = true;
            break;
        case UNDERLINE:
            boolean4 = true;
            break;
        case ITALIC:
            boolean2 = true;
            break;
        case RESET:
            return EMPTY;
        default:
            boolean5 = false;
            boolean_ = false;
            boolean3 = false;
            boolean4 = false;
            boolean2 = false;
            textColor = ChatHexColor.fromLegacyFormat(formatting);
        }

        return new ChatModifier(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier applyFormats(EnumChatFormat... formattings) {
        ChatHexColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;

        for(EnumChatFormat chatFormatting : formattings) {
            switch(chatFormatting) {
            case OBFUSCATED:
                boolean5 = true;
                break;
            case BOLD:
                boolean_ = true;
                break;
            case STRIKETHROUGH:
                boolean3 = true;
                break;
            case UNDERLINE:
                boolean4 = true;
                break;
            case ITALIC:
                boolean2 = true;
                break;
            case RESET:
                return EMPTY;
            default:
                textColor = ChatHexColor.fromLegacyFormat(chatFormatting);
            }
        }

        return new ChatModifier(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public ChatModifier setChatModifier(ChatModifier parent) {
        if (this == EMPTY) {
            return parent;
        } else {
            return parent == EMPTY ? this : new ChatModifier(this.color != null ? this.color : parent.color, this.bold != null ? this.bold : parent.bold, this.italic != null ? this.italic : parent.italic, this.underlined != null ? this.underlined : parent.underlined, this.strikethrough != null ? this.strikethrough : parent.strikethrough, this.obfuscated != null ? this.obfuscated : parent.obfuscated, this.clickEvent != null ? this.clickEvent : parent.clickEvent, this.hoverEvent != null ? this.hoverEvent : parent.hoverEvent, this.insertion != null ? this.insertion : parent.insertion, this.font != null ? this.font : parent.font);
        }
    }

    @Override
    public String toString() {
        return "Style{ color=" + this.color + ", bold=" + this.bold + ", italic=" + this.italic + ", underlined=" + this.underlined + ", strikethrough=" + this.strikethrough + ", obfuscated=" + this.obfuscated + ", clickEvent=" + this.getClickEvent() + ", hoverEvent=" + this.getHoverEvent() + ", insertion=" + this.getInsertion() + ", font=" + this.getFont() + "}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatModifier)) {
            return false;
        } else {
            ChatModifier style = (ChatModifier)object;
            return this.isBold() == style.isBold() && Objects.equals(this.getColor(), style.getColor()) && this.isItalic() == style.isItalic() && this.isRandom() == style.isRandom() && this.isStrikethrough() == style.isStrikethrough() && this.isUnderlined() == style.isUnderlined() && Objects.equals(this.getClickEvent(), style.getClickEvent()) && Objects.equals(this.getHoverEvent(), style.getHoverEvent()) && Objects.equals(this.getInsertion(), style.getInsertion()) && Objects.equals(this.getFont(), style.getFont());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion);
    }

    public static class ChatModifierSerializer implements JsonDeserializer<ChatModifier>, JsonSerializer<ChatModifier> {
        @Nullable
        @Override
        public ChatModifier deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject == null) {
                    return null;
                } else {
                    Boolean boolean_ = getOptionalFlag(jsonObject, "bold");
                    Boolean boolean2 = getOptionalFlag(jsonObject, "italic");
                    Boolean boolean3 = getOptionalFlag(jsonObject, "underlined");
                    Boolean boolean4 = getOptionalFlag(jsonObject, "strikethrough");
                    Boolean boolean5 = getOptionalFlag(jsonObject, "obfuscated");
                    ChatHexColor textColor = getTextColor(jsonObject);
                    String string = getInsertion(jsonObject);
                    ChatClickable clickEvent = getClickEvent(jsonObject);
                    ChatHoverable hoverEvent = getHoverEvent(jsonObject);
                    MinecraftKey resourceLocation = getFont(jsonObject);
                    return new ChatModifier(textColor, boolean_, boolean2, boolean3, boolean4, boolean5, clickEvent, hoverEvent, string, resourceLocation);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static MinecraftKey getFont(JsonObject root) {
            if (root.has("font")) {
                String string = ChatDeserializer.getAsString(root, "font");

                try {
                    return new MinecraftKey(string);
                } catch (ResourceKeyInvalidException var3) {
                    throw new JsonSyntaxException("Invalid font name: " + string);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static ChatHoverable getHoverEvent(JsonObject root) {
            if (root.has("hoverEvent")) {
                JsonObject jsonObject = ChatDeserializer.getAsJsonObject(root, "hoverEvent");
                ChatHoverable hoverEvent = ChatHoverable.deserialize(jsonObject);
                if (hoverEvent != null && hoverEvent.getAction().isAllowedFromServer()) {
                    return hoverEvent;
                }
            }

            return null;
        }

        @Nullable
        private static ChatClickable getClickEvent(JsonObject root) {
            if (root.has("clickEvent")) {
                JsonObject jsonObject = ChatDeserializer.getAsJsonObject(root, "clickEvent");
                String string = ChatDeserializer.getAsString(jsonObject, "action", (String)null);
                ChatClickable.EnumClickAction action = string == null ? null : ChatClickable.EnumClickAction.getByName(string);
                String string2 = ChatDeserializer.getAsString(jsonObject, "value", (String)null);
                if (action != null && string2 != null && action.isAllowedFromServer()) {
                    return new ChatClickable(action, string2);
                }
            }

            return null;
        }

        @Nullable
        private static String getInsertion(JsonObject root) {
            return ChatDeserializer.getAsString(root, "insertion", (String)null);
        }

        @Nullable
        private static ChatHexColor getTextColor(JsonObject root) {
            if (root.has("color")) {
                String string = ChatDeserializer.getAsString(root, "color");
                return ChatHexColor.parseColor(string);
            } else {
                return null;
            }
        }

        @Nullable
        private static Boolean getOptionalFlag(JsonObject root, String key) {
            return root.has(key) ? root.get(key).getAsBoolean() : null;
        }

        @Nullable
        @Override
        public JsonElement serialize(ChatModifier style, Type type, JsonSerializationContext jsonSerializationContext) {
            if (style.isEmpty()) {
                return null;
            } else {
                JsonObject jsonObject = new JsonObject();
                if (style.bold != null) {
                    jsonObject.addProperty("bold", style.bold);
                }

                if (style.italic != null) {
                    jsonObject.addProperty("italic", style.italic);
                }

                if (style.underlined != null) {
                    jsonObject.addProperty("underlined", style.underlined);
                }

                if (style.strikethrough != null) {
                    jsonObject.addProperty("strikethrough", style.strikethrough);
                }

                if (style.obfuscated != null) {
                    jsonObject.addProperty("obfuscated", style.obfuscated);
                }

                if (style.color != null) {
                    jsonObject.addProperty("color", style.color.serialize());
                }

                if (style.insertion != null) {
                    jsonObject.add("insertion", jsonSerializationContext.serialize(style.insertion));
                }

                if (style.clickEvent != null) {
                    JsonObject jsonObject2 = new JsonObject();
                    jsonObject2.addProperty("action", style.clickEvent.getAction().getName());
                    jsonObject2.addProperty("value", style.clickEvent.getValue());
                    jsonObject.add("clickEvent", jsonObject2);
                }

                if (style.hoverEvent != null) {
                    jsonObject.add("hoverEvent", style.hoverEvent.serialize());
                }

                if (style.font != null) {
                    jsonObject.addProperty("font", style.font.toString());
                }

                return jsonObject;
            }
        }
    }
}
