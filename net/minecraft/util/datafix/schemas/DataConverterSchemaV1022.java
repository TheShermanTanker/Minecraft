package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV1022 extends Schema {
    public DataConverterSchemaV1022(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(false, DataConverterTypes.RECIPE, () -> {
            return DSL.constType(DataConverterSchemaNamed.namespacedString());
        });
        schema.registerType(false, DataConverterTypes.PLAYER, () -> {
            return DSL.optionalFields("RootVehicle", DSL.optionalFields("Entity", DataConverterTypes.ENTITY_TREE.in(schema)), "Inventory", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "EnderItems", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), DSL.optionalFields("ShoulderEntityLeft", DataConverterTypes.ENTITY_TREE.in(schema), "ShoulderEntityRight", DataConverterTypes.ENTITY_TREE.in(schema), "recipeBook", DSL.optionalFields("recipes", DSL.list(DataConverterTypes.RECIPE.in(schema)), "toBeDisplayed", DSL.list(DataConverterTypes.RECIPE.in(schema)))));
        });
        schema.registerType(false, DataConverterTypes.HOTBAR, () -> {
            return DSL.compoundList(DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
    }
}
