package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2707 extends DataConverterSchemaNamed {
    public V2707(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerEntity(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, String name) {
        schema.register(entityTypes, name, () -> {
            return DataConverterSchemaV100.equipment(schema);
        });
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerEntity(schema, map, "minecraft:marker");
        return map;
    }
}
