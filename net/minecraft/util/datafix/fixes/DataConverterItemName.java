package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public abstract class DataConverterItemName extends DataFix {
    private final String name;

    public DataConverterItemName(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = DSL.named(DataConverterTypes.ITEM_NAME.typeName(), DataConverterSchemaNamed.namespacedString());
        if (!Objects.equals(this.getInputSchema().getType(DataConverterTypes.ITEM_NAME), type)) {
            throw new IllegalStateException("item name type is not what was expected.");
        } else {
            return this.fixTypeEverywhere(this.name, type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond(this::fixItem);
                };
            });
        }
    }

    protected abstract String fixItem(String input);

    public static DataFix create(Schema outputSchema, String name, Function<String, String> rename) {
        return new DataConverterItemName(outputSchema, name) {
            @Override
            protected String fixItem(String input) {
                return rename.apply(input);
            }
        };
    }
}
