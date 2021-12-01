package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class DataConverterSavedDataUUID extends DataConverterUUIDBase {
    public DataConverterSavedDataUUID(Schema outputSchema) {
        super(outputSchema, DataConverterTypes.SAVED_DATA);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("SavedDataUUIDFix", this.getInputSchema().getType(this.typeReference), (typed) -> {
            return typed.updateTyped(typed.getType().findField("data"), (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    return dynamic.update("Raids", (dynamicx) -> {
                        return dynamicx.createList(dynamicx.asStream().map((dynamic) -> {
                            return dynamic.update("HeroesOfTheVillage", (dynamicx) -> {
                                return dynamicx.createList(dynamicx.asStream().map((dynamic) -> {
                                    return createUUIDFromLongs(dynamic, "UUIDMost", "UUIDLeast").orElseGet(() -> {
                                        LOGGER.warn("HeroesOfTheVillage contained invalid UUIDs.");
                                        return dynamic;
                                    });
                                }));
                            });
                        }));
                    });
                });
            });
        });
    }
}
