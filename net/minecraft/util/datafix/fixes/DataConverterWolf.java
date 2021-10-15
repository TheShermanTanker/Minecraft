package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterWolf extends DataConverterNamedEntity {
    public DataConverterWolf(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityWolfColorFix", DataConverterTypes.ENTITY, "minecraft:wolf");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.update("CollarColor", (dynamicx) -> {
            return dynamicx.createByte((byte)(15 - dynamicx.asInt(0)));
        });
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
