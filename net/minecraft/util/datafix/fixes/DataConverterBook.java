package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonParseException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.ChatDeserializer;
import org.apache.commons.lang3.StringUtils;

public class DataConverterBook extends DataFix {
    public DataConverterBook(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.update("pages", (dynamic2) -> {
            return DataFixUtils.orElse(dynamic2.asStreamOpt().map((stream) -> {
                return stream.map((dynamic) -> {
                    if (!dynamic.asString().result().isPresent()) {
                        return dynamic;
                    } else {
                        String string = dynamic.asString("");
                        IChatBaseComponent component = null;
                        if (!"null".equals(string) && !StringUtils.isEmpty(string)) {
                            if (string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"' || string.charAt(0) == '{' && string.charAt(string.length() - 1) == '}') {
                                try {
                                    component = ChatDeserializer.fromJson(DataConverterSignText.GSON, string, IChatBaseComponent.class, true);
                                    if (component == null) {
                                        component = ChatComponentText.EMPTY;
                                    }
                                } catch (JsonParseException var6) {
                                }

                                if (component == null) {
                                    try {
                                        component = IChatBaseComponent.ChatSerializer.fromJson(string);
                                    } catch (JsonParseException var5) {
                                    }
                                }

                                if (component == null) {
                                    try {
                                        component = IChatBaseComponent.ChatSerializer.fromJsonLenient(string);
                                    } catch (JsonParseException var4) {
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

                        return dynamic.createString(IChatBaseComponent.ChatSerializer.toJson(component));
                    }
                });
            }).map(dynamic::createList).result(), dynamic.emptyList());
        });
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.ITEM_STACK);
        OpticFinder<?> opticFinder = type.findField("tag");
        return this.fixTypeEverywhereTyped("ItemWrittenBookPagesStrictJsonFix", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), this::fixTag);
            });
        });
    }
}
