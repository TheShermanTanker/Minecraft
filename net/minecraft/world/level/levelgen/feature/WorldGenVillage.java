package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;

public class WorldGenVillage extends WorldGenFeatureJigsaw {
    public WorldGenVillage(Codec<WorldGenFeatureVillageConfiguration> configCodec) {
        super(configCodec, 0, true, true, (context) -> {
            return true;
        });
    }
}
