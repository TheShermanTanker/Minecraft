package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;

public class DataConverterTeamDisplayName extends DataFix {
    public DataConverterTeamDisplayName(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, Dynamic<?>>> type = DSL.named(DataConverterTypes.TEAM.typeName(), DSL.remainderType());
        if (!Objects.equals(type, this.getInputSchema().getType(DataConverterTypes.TEAM))) {
            throw new IllegalStateException("Team type is not what was expected.");
        } else {
            return this.fixTypeEverywhere("TeamDisplayNameFix", type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond((dynamic) -> {
                        return dynamic.update("DisplayName", (dynamic2) -> {
                            return DataFixUtils.orElse(dynamic2.asString().map((string) -> {
                                return IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(string));
                            }).map(dynamic::createString).result(), dynamic2);
                        });
                    });
                };
            });
        }
    }
}
