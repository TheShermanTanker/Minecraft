package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class DataConverterColorlessShulkerEntity extends DataConverterNamedEntity {
    public DataConverterColorlessShulkerEntity(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Colorless shulker entity fix", DataConverterTypes.ENTITY, "minecraft:shulker");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.get("Color").asInt(0) == 10 ? dynamic.set("Color", dynamic.createByte((byte)16)) : dynamic;
        });
    }
}
