package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.MinecraftKey;

public class DataConverterPainting extends DataConverterNamedEntity {
    private static final Map<String, String> MAP = DataFixUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put("donkeykong", "donkey_kong");
        hashMap.put("burningskull", "burning_skull");
        hashMap.put("skullandroses", "skull_and_roses");
    });

    public DataConverterPainting(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityPaintingMotiveFix", DataConverterTypes.ENTITY, "minecraft:painting");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        Optional<String> optional = dynamic.get("Motive").asString().result();
        if (optional.isPresent()) {
            String string = optional.get().toLowerCase(Locale.ROOT);
            return dynamic.set("Motive", dynamic.createString((new MinecraftKey(MAP.getOrDefault(string, string))).toString()));
        } else {
            return dynamic;
        }
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
