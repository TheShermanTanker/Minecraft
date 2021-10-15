package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;

public class DataConverterZombieVillagerLevelXp extends DataConverterNamedEntity {
    public DataConverterZombieVillagerLevelXp(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Zombie Villager XP rebuild", DataConverterTypes.ENTITY, "minecraft:zombie_villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            Optional<Number> optional = dynamic.get("Xp").asNumber().result();
            if (!optional.isPresent()) {
                int i = dynamic.get("VillagerData").get("level").asInt(1);
                return dynamic.set("Xp", dynamic.createInt(DataConverterVillagerLevelXp.getMinXpPerLevel(i)));
            } else {
                return dynamic;
            }
        });
    }
}
