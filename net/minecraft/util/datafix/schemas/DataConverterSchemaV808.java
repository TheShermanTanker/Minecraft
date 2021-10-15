package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV808 extends DataConverterSchemaNamed {
    public DataConverterSchemaV808(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String blockEntityId) {
        schema.register(map, blockEntityId, () -> {
            return DSL.optionalFields("Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        registerInventory(schema, map, "minecraft:shulker_box");
        return map;
    }
}
