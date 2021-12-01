package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import java.util.UUID;

public class DataConverterUUID extends DataFix {
    public DataConverterUUID(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("EntityStringUuidFix", this.getInputSchema().getType(DataConverterTypes.ENTITY), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                Optional<String> optional = dynamic.get("UUID").asString().result();
                if (optional.isPresent()) {
                    UUID uUID = UUID.fromString(optional.get());
                    return dynamic.remove("UUID").set("UUIDMost", dynamic.createLong(uUID.getMostSignificantBits())).set("UUIDLeast", dynamic.createLong(uUID.getLeastSignificantBits()));
                } else {
                    return dynamic;
                }
            });
        });
    }
}
