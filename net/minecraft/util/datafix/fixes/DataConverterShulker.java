package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterShulker extends DataConverterNamedEntity {
    public DataConverterShulker(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityShulkerColorFix", DataConverterTypes.ENTITY, "minecraft:shulker");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return !dynamic.get("Color").map(Dynamic::asNumber).result().isPresent() ? dynamic.set("Color", dynamic.createByte((byte)10)) : dynamic;
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
