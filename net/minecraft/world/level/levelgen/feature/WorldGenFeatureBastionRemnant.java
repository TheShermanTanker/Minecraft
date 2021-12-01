package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class WorldGenFeatureBastionRemnant extends WorldGenFeatureJigsaw {
    private static final int BASTION_SPAWN_HEIGHT = 33;

    public WorldGenFeatureBastionRemnant(Codec<WorldGenFeatureVillageConfiguration> configCodec) {
        super(configCodec, 33, false, false, WorldGenFeatureBastionRemnant::checkLocation);
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureVillageConfiguration> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        return worldgenRandom.nextInt(5) >= 2;
    }
}
