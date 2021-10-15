package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterBiomeBase extends DataFix {
    private final String name;
    private final Map<String, String> biomes;

    public DataConverterBiomeBase(Schema outputSchema, boolean changesType, String name, Map<String, String> changes) {
        super(outputSchema, changesType);
        this.biomes = changes;
        this.name = name;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = DSL.named(DataConverterTypes.BIOME.typeName(), DataConverterSchemaNamed.namespacedString());
        if (!Objects.equals(type, this.getInputSchema().getType(DataConverterTypes.BIOME))) {
            throw new IllegalStateException("Biome type is not what was expected.");
        } else {
            return this.fixTypeEverywhere(this.name, type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond((string) -> {
                        return this.biomes.getOrDefault(string, string);
                    });
                };
            });
        }
    }
}
