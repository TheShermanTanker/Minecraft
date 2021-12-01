package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV106 extends Schema {
    public DataConverterSchemaV106(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(true, DataConverterTypes.UNTAGGED_SPAWNER, () -> {
            return DSL.optionalFields("SpawnPotentials", DSL.list(DSL.fields("Entity", DataConverterTypes.ENTITY_TREE.in(schema))), "SpawnData", DataConverterTypes.ENTITY_TREE.in(schema));
        });
    }
}
