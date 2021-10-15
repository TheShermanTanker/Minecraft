package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class DataConverterStructureReference extends DataFix {
    public DataConverterStructureReference(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.STRUCTURE_FEATURE);
        return this.fixTypeEverywhereTyped("Structure Reference Fix", type, (typed) -> {
            return typed.update(DSL.remainderFinder(), DataConverterStructureReference::setCountToAtLeastOne);
        });
    }

    private static <T> Dynamic<T> setCountToAtLeastOne(Dynamic<T> dynamic) {
        return dynamic.update("references", (dynamicx) -> {
            return dynamicx.createInt(dynamicx.asNumber().map(Number::intValue).result().filter((integer) -> {
                return integer > 0;
            }).orElse(1));
        });
    }
}