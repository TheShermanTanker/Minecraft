package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterArmorStand extends DataConverterNamedEntity {
    public DataConverterArmorStand(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityArmorStandSilentFix", DataConverterTypes.ENTITY, "ArmorStand");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.get("Silent").asBoolean(false) && !dynamic.get("Marker").asBoolean(false) ? dynamic.remove("Silent") : dynamic;
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
