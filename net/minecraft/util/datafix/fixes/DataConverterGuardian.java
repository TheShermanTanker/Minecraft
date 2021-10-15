package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class DataConverterGuardian extends DataConverterEntityNameAbstract {
    public DataConverterGuardian(Schema outputSchema, boolean changesType) {
        super("EntityElderGuardianSplitFix", outputSchema, changesType);
    }

    @Override
    protected Pair<String, Dynamic<?>> getNewNameAndTag(String choice, Dynamic<?> dynamic) {
        return Pair.of(Objects.equals(choice, "Guardian") && dynamic.get("Elder").asBoolean(false) ? "ElderGuardian" : choice, dynamic);
    }
}
