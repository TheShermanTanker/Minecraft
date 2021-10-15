package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;

public class DataConverterBeehive extends DataConverterPOIRename {
    public DataConverterBeehive(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected String rename(String input) {
        return input.equals("minecraft:bee_hive") ? "minecraft:beehive" : input;
    }
}
