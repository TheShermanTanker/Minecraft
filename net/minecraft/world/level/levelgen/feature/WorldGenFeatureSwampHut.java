package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenWitchHut;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureSwampHut extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> SWAMPHUT_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.WITCH, 1, 1, 1));
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> SWAMPHUT_ANIMALS = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.CAT, 1, 1, 1));

    public WorldGenFeatureSwampHut(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(PieceGeneratorSupplier.checkForBiomeOnTop(HeightMap.Type.WORLD_SURFACE_WG), WorldGenFeatureSwampHut::generatePieces));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        collector.addPiece(new WorldGenWitchHut(context.random(), context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()));
    }
}
