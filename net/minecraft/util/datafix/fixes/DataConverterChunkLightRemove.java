package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class DataConverterChunkLightRemove extends DataFix {
    public DataConverterChunkLightRemove(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.CHUNK);
        Type<?> type2 = type.findFieldType("Level");
        OpticFinder<?> opticFinder = DSL.fieldFinder("Level", type2);
        return this.fixTypeEverywhereTyped("ChunkLightRemoveFix", type, this.getOutputSchema().getType(DataConverterTypes.CHUNK), (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    return dynamic.remove("isLightOn");
                });
            });
        });
    }
}
