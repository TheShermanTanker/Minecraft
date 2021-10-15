package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV1451_1 extends DataConverterSchemaNamed {
    public DataConverterSchemaV1451_1(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Override
    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(false, DataConverterTypes.CHUNK, () -> {
            return DSL.fields("Level", DSL.optionalFields("Entities", DSL.list(DataConverterTypes.ENTITY_TREE.in(schema)), "TileEntities", DSL.list(DataConverterTypes.BLOCK_ENTITY.in(schema)), "TileTicks", DSL.list(DSL.fields("i", DataConverterTypes.BLOCK_NAME.in(schema))), "Sections", DSL.list(DSL.optionalFields("Palette", DSL.list(DataConverterTypes.BLOCK_STATE.in(schema))))));
        });
    }
}
