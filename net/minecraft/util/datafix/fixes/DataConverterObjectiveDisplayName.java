package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;

public class DataConverterObjectiveDisplayName extends DataFix {
    public DataConverterObjectiveDisplayName(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.OBJECTIVE);
        return this.fixTypeEverywhereTyped("ObjectiveDisplayNameFix", type, (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.update("DisplayName", (dynamic2) -> {
                    return DataFixUtils.orElse(dynamic2.asString().map((string) -> {
                        return IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(string));
                    }).map(dynamic::createString).result(), dynamic2);
                });
            });
        });
    }
}
