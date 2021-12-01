package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterBlockName extends DataFix {
    public DataConverterBlockName(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.BLOCK_NAME);
        Type<?> type2 = this.getOutputSchema().getType(DataConverterTypes.BLOCK_NAME);
        Type<Pair<String, Either<Integer, String>>> type3 = DSL.named(DataConverterTypes.BLOCK_NAME.typeName(), DSL.or(DSL.intType(), DataConverterSchemaNamed.namespacedString()));
        Type<Pair<String, String>> type4 = DSL.named(DataConverterTypes.BLOCK_NAME.typeName(), DataConverterSchemaNamed.namespacedString());
        if (Objects.equals(type, type3) && Objects.equals(type2, type4)) {
            return this.fixTypeEverywhere("BlockNameFlatteningFix", type3, type4, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond((either) -> {
                        return either.map(DataConverterFlattenData::upgradeBlock, (string) -> {
                            return DataConverterFlattenData.upgradeBlock(DataConverterSchemaNamed.ensureNamespaced(string));
                        });
                    });
                };
            });
        } else {
            throw new IllegalStateException("Expected and actual types don't match.");
        }
    }
}
