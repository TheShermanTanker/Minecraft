package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV102 extends Schema {
    public DataConverterSchemaV102(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(true, DataConverterTypes.ITEM_STACK, () -> {
            return DSL.hook(DSL.optionalFields("id", DataConverterTypes.ITEM_NAME.in(schema), "tag", DSL.optionalFields("EntityTag", DataConverterTypes.ENTITY_TREE.in(schema), "BlockEntityTag", DataConverterTypes.BLOCK_ENTITY.in(schema), "CanDestroy", DSL.list(DataConverterTypes.BLOCK_NAME.in(schema)), "CanPlaceOn", DSL.list(DataConverterTypes.BLOCK_NAME.in(schema)), "Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)))), DataConverterSchemaV99.ADD_NAMES, HookFunction.IDENTITY);
        });
    }
}
