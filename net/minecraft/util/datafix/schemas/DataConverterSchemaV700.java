package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class DataConverterSchemaV700 extends Schema {
    public DataConverterSchemaV700(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return DataConverterSchemaV100.equipment(schema);
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerMob(schema, map, "ElderGuardian");
        return map;
    }
}
