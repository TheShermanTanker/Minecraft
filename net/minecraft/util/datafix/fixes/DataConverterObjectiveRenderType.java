package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Optional;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class DataConverterObjectiveRenderType extends DataFix {
    public DataConverterObjectiveRenderType(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private static IScoreboardCriteria.EnumScoreboardHealthDisplay getRenderType(String oldName) {
        return oldName.equals("health") ? IScoreboardCriteria.EnumScoreboardHealthDisplay.HEARTS : IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER;
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.OBJECTIVE);
        return this.fixTypeEverywhereTyped("ObjectiveRenderTypeFix", type, (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                Optional<String> optional = dynamic.get("RenderType").asString().result();
                if (!optional.isPresent()) {
                    String string = dynamic.get("CriteriaName").asString("");
                    IScoreboardCriteria.EnumScoreboardHealthDisplay renderType = getRenderType(string);
                    return dynamic.set("RenderType", dynamic.createString(renderType.getId()));
                } else {
                    return dynamic;
                }
            });
        });
    }
}
