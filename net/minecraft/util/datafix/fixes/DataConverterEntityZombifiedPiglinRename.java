package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class DataConverterEntityZombifiedPiglinRename extends DataConverterEntityRenameAbstract {
    public static final Map<String, String> RENAMED_IDS = ImmutableMap.<String, String>builder().put("minecraft:zombie_pigman_spawn_egg", "minecraft:zombified_piglin_spawn_egg").build();

    public DataConverterEntityZombifiedPiglinRename(Schema outputSchema) {
        super("EntityZombifiedPiglinRenameFix", outputSchema, true);
    }

    @Override
    protected String rename(String oldName) {
        return Objects.equals("minecraft:zombie_pigman", oldName) ? "minecraft:zombified_piglin" : oldName;
    }
}
