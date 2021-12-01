package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class DataConverterSchemaV2502 extends DataConverterSchemaNamed {
    public DataConverterSchemaV2502(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> {
            return DataConverterSchemaV100.equipment(schema);
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerMob(schema, map, "minecraft:hoglin");
        return map;
    }
}
