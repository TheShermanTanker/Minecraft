package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.ChatTypeAdapterFactory;
import net.minecraft.util.FormattedString;

public interface IChatBaseComponent extends Message, IChatFormatted {
    ChatModifier getChatModifier();

    String getContents();

    @Override
    default String getString() {
        return IChatFormatted.super.getString();
    }

    default String getString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        this.visit((string) -> {
            int j = length - stringBuilder.length();
            if (j <= 0) {
                return STOP_ITERATION;
            } else {
                stringBuilder.append(string.length() <= j ? string : string.substring(0, j));
                return Optional.empty();
            }
        });
        return stringBuilder.toString();
    }

    List<IChatBaseComponent> getSiblings();

    IChatMutableComponent plainCopy();

    IChatMutableComponent mutableCopy();

    FormattedString getVisualOrderText();

    @Override
    default <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style) {
        ChatModifier style2 = this.getChatModifier().setChatModifier(style);
        Optional<T> optional = this.visitSelf(styledVisitor, style2);
        if (optional.isPresent()) {
            return optional;
        } else {
            for(IChatBaseComponent component : this.getSiblings()) {
                Optional<T> optional2 = component.visit(styledVisitor, style2);
                if (optional2.isPresent()) {
                    return optional2;
                }
            }

            return Optional.empty();
        }
    }

    @Override
    default <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor) {
        Optional<T> optional = this.visitSelf(visitor);
        if (optional.isPresent()) {
            return optional;
        } else {
            for(IChatBaseComponent component : this.getSiblings()) {
                Optional<T> optional2 = component.visit(visitor);
                if (optional2.isPresent()) {
                    return optional2;
                }
            }

            return Optional.empty();
        }
    }

    default <T> Optional<T> visitSelf(IChatFormatted.StyledContentConsumer<T> visitor, ChatModifier style) {
        return visitor.accept(style, this.getContents());
    }

    default <T> Optional<T> visitSelf(IChatFormatted.ContentConsumer<T> visitor) {
        return visitor.accept(this.getContents());
    }

    default List<IChatBaseComponent> toFlatList(ChatModifier style) {
        List<IChatBaseComponent> list = Lists.newArrayList();
        this.visit((styleOverride, text) -> {
            if (!text.isEmpty()) {
                list.add((new ChatComponentText(text)).withStyle(styleOverride));
            }

            return Optional.empty();
        }, style);
        return list;
    }

    static IChatBaseComponent nullToEmpty(@Nullable String string) {
        return (IChatBaseComponent)(string != null ? new ChatComponentText(string) : ChatComponentText.EMPTY);
    }

    public static class ChatSerializer implements JsonDeserializer<IChatMutableComponent>, JsonSerializer<IChatBaseComponent> {
        private static final Gson GSON = SystemUtils.make(() -> {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.disableHtmlEscaping();
            gsonBuilder.registerTypeHierarchyAdapter(IChatBaseComponent.class, new IChatBaseComponent.ChatSerializer());
            gsonBuilder.registerTypeHierarchyAdapter(ChatModifier.class, new ChatModifier.ChatModifierSerializer());
            gsonBuilder.registerTypeAdapterFactory(new ChatTypeAdapterFactory());
            return gsonBuilder.create();
        });
        private static final Field JSON_READER_POS = SystemUtils.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("pos");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
            }
        });
        private static final Field JSON_READER_LINESTART = SystemUtils.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("lineStart");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
            }
        });

        @Override
        public IChatMutableComponent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return new ChatComponentText(jsonElement.getAsString());
            } else if (!jsonElement.isJsonObject()) {
                if (jsonElement.isJsonArray()) {
                    JsonArray jsonArray3 = jsonElement.getAsJsonArray();
                    IChatMutableComponent mutableComponent13 = null;

                    for(JsonElement jsonElement2 : jsonArray3) {
                        IChatMutableComponent mutableComponent14 = this.deserialize(jsonElement2, jsonElement2.getClass(), jsonDeserializationContext);
                        if (mutableComponent13 == null) {
                            mutableComponent13 = mutableComponent14;
                        } else {
                            mutableComponent13.addSibling(mutableComponent14);
                        }
                    }

                    return mutableComponent13;
                } else {
                    throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                }
            } else {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                IChatMutableComponent mutableComponent;
                if (jsonObject.has("text")) {
                    mutableComponent = new ChatComponentText(ChatDeserializer.getAsString(jsonObject, "text"));
                } else if (jsonObject.has("translate")) {
                    String string = ChatDeserializer.getAsString(jsonObject, "translate");
                    if (jsonObject.has("with")) {
                        JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "with");
                        Object[] objects = new Object[jsonArray.size()];

                        for(int i = 0; i < objects.length; ++i) {
                            objects[i] = this.deserialize(jsonArray.get(i), type, jsonDeserializationContext);
                            if (objects[i] instanceof ChatComponentText) {
                                ChatComponentText textComponent = (ChatComponentText)objects[i];
                                if (textComponent.getChatModifier().isEmpty() && textComponent.getSiblings().isEmpty()) {
                                    objects[i] = textComponent.getText();
                                }
                            }
                        }

                        mutableComponent = new ChatMessage(string, objects);
                    } else {
                        mutableComponent = new ChatMessage(string);
                    }
                } else if (jsonObject.has("score")) {
                    JsonObject jsonObject2 = ChatDeserializer.getAsJsonObject(jsonObject, "score");
                    if (!jsonObject2.has("name") || !jsonObject2.has("objective")) {
                        throw new JsonParseException("A score component needs a least a name and an objective");
                    }

                    mutableComponent = new ChatComponentScore(ChatDeserializer.getAsString(jsonObject2, "name"), ChatDeserializer.getAsString(jsonObject2, "objective"));
                } else if (jsonObject.has("selector")) {
                    Optional<IChatBaseComponent> optional = this.parseSeparator(type, jsonDeserializationContext, jsonObject);
                    mutableComponent = new ChatComponentSelector(ChatDeserializer.getAsString(jsonObject, "selector"), optional);
                } else if (jsonObject.has("keybind")) {
                    mutableComponent = new ChatComponentKeybind(ChatDeserializer.getAsString(jsonObject, "keybind"));
                } else {
                    if (!jsonObject.has("nbt")) {
                        throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                    }

                    String string2 = ChatDeserializer.getAsString(jsonObject, "nbt");
                    Optional<IChatBaseComponent> optional2 = this.parseSeparator(type, jsonDeserializationContext, jsonObject);
                    boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "interpret", false);
                    if (jsonObject.has("block")) {
                        mutableComponent = new ChatComponentNBT.BlockNbtComponent(string2, bl, ChatDeserializer.getAsString(jsonObject, "block"), optional2);
                    } else if (jsonObject.has("entity")) {
                        mutableComponent = new ChatComponentNBT.EntityNbtComponent(string2, bl, ChatDeserializer.getAsString(jsonObject, "entity"), optional2);
                    } else {
                        if (!jsonObject.has("storage")) {
                            throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                        }

                        mutableComponent = new ChatComponentNBT.StorageNbtComponent(string2, bl, new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "storage")), optional2);
                    }
                }

                if (jsonObject.has("extra")) {
                    JsonArray jsonArray2 = ChatDeserializer.getAsJsonArray(jsonObject, "extra");
                    if (jsonArray2.size() <= 0) {
                        throw new JsonParseException("Unexpected empty array of components");
                    }

                    for(int j = 0; j < jsonArray2.size(); ++j) {
                        mutableComponent.addSibling(this.deserialize(jsonArray2.get(j), type, jsonDeserializationContext));
                    }
                }

                mutableComponent.setChatModifier(jsonDeserializationContext.deserialize(jsonElement, ChatModifier.class));
                return mutableComponent;
            }
        }

        private Optional<IChatBaseComponent> parseSeparator(Type type, JsonDeserializationContext context, JsonObject json) {
            return json.has("separator") ? Optional.of(this.deserialize(json.get("separator"), type, context)) : Optional.empty();
        }

        private void serializeStyle(ChatModifier style, JsonObject json, JsonSerializationContext context) {
            JsonElement jsonElement = context.serialize(style);
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = (JsonObject)jsonElement;

                for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }

        }

        @Override
        public JsonElement serialize(IChatBaseComponent component, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            if (!component.getChatModifier().isEmpty()) {
                this.serializeStyle(component.getChatModifier(), jsonObject, jsonSerializationContext);
            }

            if (!component.getSiblings().isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(IChatBaseComponent component2 : component.getSiblings()) {
                    jsonArray.add(this.serialize(component2, component2.getClass(), jsonSerializationContext));
                }

                jsonObject.add("extra", jsonArray);
            }

            if (component instanceof ChatComponentText) {
                jsonObject.addProperty("text", ((ChatComponentText)component).getText());
            } else if (component instanceof ChatMessage) {
                ChatMessage translatableComponent = (ChatMessage)component;
                jsonObject.addProperty("translate", translatableComponent.getKey());
                if (translatableComponent.getArgs() != null && translatableComponent.getArgs().length > 0) {
                    JsonArray jsonArray2 = new JsonArray();

                    for(Object object : translatableComponent.getArgs()) {
                        if (object instanceof IChatBaseComponent) {
                            jsonArray2.add(this.serialize((IChatBaseComponent)object, object.getClass(), jsonSerializationContext));
                        } else {
                            jsonArray2.add(new JsonPrimitive(String.valueOf(object)));
                        }
                    }

                    jsonObject.add("with", jsonArray2);
                }
            } else if (component instanceof ChatComponentScore) {
                ChatComponentScore scoreComponent = (ChatComponentScore)component;
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("name", scoreComponent.getName());
                jsonObject2.addProperty("objective", scoreComponent.getObjective());
                jsonObject.add("score", jsonObject2);
            } else if (component instanceof ChatComponentSelector) {
                ChatComponentSelector selectorComponent = (ChatComponentSelector)component;
                jsonObject.addProperty("selector", selectorComponent.getPattern());
                this.serializeSeparator(jsonSerializationContext, jsonObject, selectorComponent.getSeparator());
            } else if (component instanceof ChatComponentKeybind) {
                ChatComponentKeybind keybindComponent = (ChatComponentKeybind)component;
                jsonObject.addProperty("keybind", keybindComponent.getName());
            } else {
                if (!(component instanceof ChatComponentNBT)) {
                    throw new IllegalArgumentException("Don't know how to serialize " + component + " as a Component");
                }

                ChatComponentNBT nbtComponent = (ChatComponentNBT)component;
                jsonObject.addProperty("nbt", nbtComponent.getNbtPath());
                jsonObject.addProperty("interpret", nbtComponent.isInterpreting());
                this.serializeSeparator(jsonSerializationContext, jsonObject, nbtComponent.separator);
                if (component instanceof ChatComponentNBT.BlockNbtComponent) {
                    ChatComponentNBT.BlockNbtComponent blockNbtComponent = (ChatComponentNBT.BlockNbtComponent)component;
                    jsonObject.addProperty("block", blockNbtComponent.getPos());
                } else if (component instanceof ChatComponentNBT.EntityNbtComponent) {
                    ChatComponentNBT.EntityNbtComponent entityNbtComponent = (ChatComponentNBT.EntityNbtComponent)component;
                    jsonObject.addProperty("entity", entityNbtComponent.getSelector());
                } else {
                    if (!(component instanceof ChatComponentNBT.StorageNbtComponent)) {
                        throw new IllegalArgumentException("Don't know how to serialize " + component + " as a Component");
                    }

                    ChatComponentNBT.StorageNbtComponent storageNbtComponent = (ChatComponentNBT.StorageNbtComponent)component;
                    jsonObject.addProperty("storage", storageNbtComponent.getId().toString());
                }
            }

            return jsonObject;
        }

        private void serializeSeparator(JsonSerializationContext context, JsonObject json, Optional<IChatBaseComponent> separator) {
            separator.ifPresent((separatorx) -> {
                json.add("separator", this.serialize(separatorx, separatorx.getClass(), context));
            });
        }

        public static String toJson(IChatBaseComponent text) {
            return GSON.toJson(text);
        }

        public static JsonElement toJsonTree(IChatBaseComponent text) {
            return GSON.toJsonTree(text);
        }

        @Nullable
        public static IChatMutableComponent fromJson(String json) {
            return ChatDeserializer.fromJson(GSON, json, IChatMutableComponent.class, false);
        }

        @Nullable
        public static IChatMutableComponent fromJson(JsonElement json) {
            return GSON.fromJson(json, IChatMutableComponent.class);
        }

        @Nullable
        public static IChatMutableComponent fromJsonLenient(String json) {
            return ChatDeserializer.fromJson(GSON, json, IChatMutableComponent.class, true);
        }

        public static IChatMutableComponent fromJson(com.mojang.brigadier.StringReader reader) {
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(reader.getRemaining()));
                jsonReader.setLenient(false);
                IChatMutableComponent mutableComponent = GSON.getAdapter(IChatMutableComponent.class).read(jsonReader);
                reader.setCursor(reader.getCursor() + getPos(jsonReader));
                return mutableComponent;
            } catch (StackOverflowError | IOException var3) {
                throw new JsonParseException(var3);
            }
        }

        private static int getPos(JsonReader reader) {
            try {
                return JSON_READER_POS.getInt(reader) - JSON_READER_LINESTART.getInt(reader) + 1;
            } catch (IllegalAccessException var2) {
                throw new IllegalStateException("Couldn't read position of JsonReader", var2);
            }
        }
    }
}
