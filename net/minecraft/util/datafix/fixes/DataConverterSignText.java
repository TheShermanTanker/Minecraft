package net.minecraft.util.datafix.fixes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.lang.reflect.Type;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.util.ChatDeserializer;
import org.apache.commons.lang3.StringUtils;

public class DataConverterSignText extends DataConverterNamedEntity {
    public static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(IChatBaseComponent.class, new JsonDeserializer<IChatBaseComponent>() {
        @Override
        public IChatMutableComponent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return new ChatComponentText(jsonElement.getAsString());
            } else if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                IChatMutableComponent mutableComponent = null;

                for(JsonElement jsonElement2 : jsonArray) {
                    IChatMutableComponent mutableComponent2 = this.deserialize(jsonElement2, jsonElement2.getClass(), jsonDeserializationContext);
                    if (mutableComponent == null) {
                        mutableComponent = mutableComponent2;
                    } else {
                        mutableComponent.addSibling(mutableComponent2);
                    }
                }

                return mutableComponent;
            } else {
                throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
            }
        }
    }).create();

    public DataConverterSignText(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntitySignTextStrictJsonFix", DataConverterTypes.BLOCK_ENTITY, "Sign");
    }

    private Dynamic<?> updateLine(Dynamic<?> dynamic, String lineName) {
        String string = dynamic.get(lineName).asString("");
        IChatBaseComponent component = null;
        if (!"null".equals(string) && !StringUtils.isEmpty(string)) {
            if (string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"' || string.charAt(0) == '{' && string.charAt(string.length() - 1) == '}') {
                try {
                    component = ChatDeserializer.fromJson(GSON, string, IChatBaseComponent.class, true);
                    if (component == null) {
                        component = ChatComponentText.EMPTY;
                    }
                } catch (JsonParseException var8) {
                }

                if (component == null) {
                    try {
                        component = IChatBaseComponent.ChatSerializer.fromJson(string);
                    } catch (JsonParseException var7) {
                    }
                }

                if (component == null) {
                    try {
                        component = IChatBaseComponent.ChatSerializer.fromJsonLenient(string);
                    } catch (JsonParseException var6) {
                    }
                }

                if (component == null) {
                    component = new ChatComponentText(string);
                }
            } else {
                component = new ChatComponentText(string);
            }
        } else {
            component = ChatComponentText.EMPTY;
        }

        return dynamic.set(lineName, dynamic.createString(IChatBaseComponent.ChatSerializer.toJson(component)));
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            dynamic = this.updateLine(dynamic, "Text1");
            dynamic = this.updateLine(dynamic, "Text2");
            dynamic = this.updateLine(dynamic, "Text3");
            return this.updateLine(dynamic, "Text4");
        });
    }
}
