package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class DataConverterJukeBox extends DataConverterNamedEntity {
    public DataConverterJukeBox(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntityJukeboxFix", DataConverterTypes.BLOCK_ENTITY, "minecraft:jukebox");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        Type<?> type = this.getInputSchema().getChoiceType(DataConverterTypes.BLOCK_ENTITY, "minecraft:jukebox");
        Type<?> type2 = type.findFieldType("RecordItem");
        OpticFinder<?> opticFinder = DSL.fieldFinder("RecordItem", type2);
        Dynamic<?> dynamic = inputType.get(DSL.remainderFinder());
        int i = dynamic.get("Record").asInt(0);
        if (i > 0) {
            dynamic.remove("Record");
            String string = DataConverterFlatten.updateItem(DataConverterMaterialId.getItem(i), 0);
            if (string != null) {
                Dynamic<?> dynamic2 = dynamic.emptyMap();
                dynamic2 = dynamic2.set("id", dynamic2.createString(string));
                dynamic2 = dynamic2.set("Count", dynamic2.createByte((byte)1));
                return inputType.set(opticFinder, type2.readTyped(dynamic2).result().orElseThrow(() -> {
                    return new IllegalStateException("Could not create record item stack.");
                }).getFirst()).set(DSL.remainderFinder(), dynamic);
            }
        }

        return inputType;
    }
}
