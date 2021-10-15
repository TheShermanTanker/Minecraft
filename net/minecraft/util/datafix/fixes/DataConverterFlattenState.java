package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class DataConverterFlattenState extends DataFix {
    public DataConverterFlattenState(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("BlockStateStructureTemplateFix", this.getInputSchema().getType(DataConverterTypes.BLOCK_STATE), (typed) -> {
            return typed.update(DSL.remainderFinder(), DataConverterFlattenData::upgradeBlockStateTag);
        });
    }
}
