package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterBlockEntityKeepPacked extends DataConverterNamedEntity {
    public DataConverterBlockEntityKeepPacked(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntityKeepPacked", DataConverterTypes.BLOCK_ENTITY, "DUMMY");
    }

    private static Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.set("keepPacked", dynamic.createBoolean(true));
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), DataConverterBlockEntityKeepPacked::fixTag);
    }
}
