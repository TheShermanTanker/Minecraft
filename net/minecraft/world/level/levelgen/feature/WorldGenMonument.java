package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenMonumentPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenMonument extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> MONUMENT_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.GUARDIAN, 1, 2, 4));

    public WorldGenMonument(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenMonument::checkLocation, WorldGenMonument::generatePieces));
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureEmptyConfiguration> context) {
        int i = context.chunkPos().getBlockX(9);
        int j = context.chunkPos().getBlockZ(9);

        for(BiomeBase biome : context.biomeSource().getBiomesWithin(i, context.chunkGenerator().getSeaLevel(), j, 29, context.chunkGenerator().climateSampler())) {
            if (biome.getBiomeCategory() != BiomeBase.Geography.OCEAN && biome.getBiomeCategory() != BiomeBase.Geography.RIVER) {
                return false;
            }
        }

        return context.validBiomeOnTop(HeightMap.Type.OCEAN_FLOOR_WG);
    }

    private static StructurePiece createTopPiece(ChunkCoordIntPair pos, SeededRandom random) {
        int i = pos.getMinBlockX() - 29;
        int j = pos.getMinBlockZ() - 29;
        EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
        return new WorldGenMonumentPieces.WorldGenMonumentPiece1(random, i, j, direction);
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        collector.addPiece(createTopPiece(context.chunkPos(), context.random()));
    }

    public static PiecesContainer regeneratePiecesAfterLoad(ChunkCoordIntPair pos, long worldSeed, PiecesContainer pieces) {
        if (pieces.isEmpty()) {
            return pieces;
        } else {
            SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
            worldgenRandom.setLargeFeatureSeed(worldSeed, pos.x, pos.z);
            StructurePiece structurePiece = pieces.pieces().get(0);
            StructureBoundingBox boundingBox = structurePiece.getBoundingBox();
            int i = boundingBox.minX();
            int j = boundingBox.minZ();
            EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(worldgenRandom);
            EnumDirection direction2 = Objects.requireNonNullElse(structurePiece.getOrientation(), direction);
            StructurePiece structurePiece2 = new WorldGenMonumentPieces.WorldGenMonumentPiece1(worldgenRandom, i, j, direction2);
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            structurePiecesBuilder.addPiece(structurePiece2);
            return structurePiecesBuilder.build();
        }
    }
}
