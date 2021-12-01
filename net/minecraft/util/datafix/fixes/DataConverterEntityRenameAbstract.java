package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public abstract class DataConverterEntityRenameAbstract extends DataFix {
    private final String name;

    public DataConverterEntityRenameAbstract(String name, Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(DataConverterTypes.ENTITY);
        TaggedChoiceType<String> taggedChoiceType2 = this.getOutputSchema().findChoiceType(DataConverterTypes.ENTITY);
        Type<Pair<String, String>> type = DSL.named(DataConverterTypes.ENTITY_NAME.typeName(), DataConverterSchemaNamed.namespacedString());
        if (!Objects.equals(this.getOutputSchema().getType(DataConverterTypes.ENTITY_NAME), type)) {
            throw new IllegalStateException("Entity name type is not what was expected.");
        } else {
            return TypeRewriteRule.seq(this.fixTypeEverywhere(this.name, taggedChoiceType, taggedChoiceType2, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapFirst((string) -> {
                        String string2 = this.rename(string);
                        Type<?> type = taggedChoiceType.types().get(string);
                        Type<?> type2 = taggedChoiceType2.types().get(string2);
                        if (!type2.equals(type, true, true)) {
                            throw new IllegalStateException(String.format("Dynamic type check failed: %s not equal to %s", type2, type));
                        } else {
                            return string2;
                        }
                    });
                };
            }), this.fixTypeEverywhere(this.name + " for entity name", type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond(this::rename);
                };
            }));
        }
    }

    protected abstract String rename(String oldName);
}
