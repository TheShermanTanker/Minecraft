package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class DataConverterVBO extends DataFix {
    public DataConverterVBO(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("OptionsForceVBOFix", this.getInputSchema().getType(DataConverterTypes.OPTIONS), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.set("useVbo", dynamic.createString("true"));
            });
        });
    }
}
