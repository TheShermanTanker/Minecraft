package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Const.PrimitiveType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.resources.MinecraftKey;

public class DataConverterSchemaNamed extends Schema {
    public static final PrimitiveCodec<String> NAMESPACED_STRING_CODEC = new PrimitiveCodec<String>() {
        public <T> DataResult<String> read(DynamicOps<T> dynamicOps, T object) {
            return dynamicOps.getStringValue(object).map(DataConverterSchemaNamed::ensureNamespaced);
        }

        public <T> T write(DynamicOps<T> dynamicOps, String string) {
            return dynamicOps.createString(string);
        }

        @Override
        public String toString() {
            return "NamespacedString";
        }
    };
    private static final Type<String> NAMESPACED_STRING = new PrimitiveType<>(NAMESPACED_STRING_CODEC);

    public DataConverterSchemaNamed(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static String ensureNamespaced(String id) {
        MinecraftKey resourceLocation = MinecraftKey.tryParse(id);
        return resourceLocation != null ? resourceLocation.toString() : id;
    }

    public static Type<String> namespacedString() {
        return NAMESPACED_STRING;
    }

    public Type<?> getChoiceType(TypeReference typeReference, String string) {
        return super.getChoiceType(typeReference, ensureNamespaced(string));
    }
}
