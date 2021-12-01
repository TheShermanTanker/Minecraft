package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterCustomNameEntity extends DataFix {
    public DataConverterCustomNameEntity(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        OpticFinder<String> opticFinder = DSL.fieldFinder("id", DataConverterSchemaNamed.namespacedString());
        return this.fixTypeEverywhereTyped("EntityCustomNameToComponentFix", this.getInputSchema().getType(DataConverterTypes.ENTITY), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                Optional<String> optional = typed.getOptional(opticFinder);
                return optional.isPresent() && Objects.equals(optional.get(), "minecraft:commandblock_minecart") ? dynamic : fixTagCustomName(dynamic);
            });
        });
    }

    public static Dynamic<?> fixTagCustomName(Dynamic<?> dynamic) {
        String string = dynamic.get("CustomName").asString("");
        return string.isEmpty() ? dynamic.remove("CustomName") : dynamic.set("CustomName", dynamic.createString(IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(string))));
    }
}
