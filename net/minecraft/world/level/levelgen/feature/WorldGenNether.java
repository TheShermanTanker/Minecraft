package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.QuartPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenNetherPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenNether extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> FORTRESS_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.BLAZE, 10, 2, 3), new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 5, 4, 4), new BiomeSettingsMobs.SpawnerData(EntityTypes.WITHER_SKELETON, 8, 5, 5), new BiomeSettingsMobs.SpawnerData(EntityTypes.SKELETON, 2, 5, 5), new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 3, 4, 4));

    public WorldGenNether(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenNether::checkLocation, WorldGenNether::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureEmptyConfiguration> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        return worldgenRandom.nextInt(5) >= 2 ? false : context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(context.chunkPos().getMiddleBlockX()), QuartPos.fromBlock(64), QuartPos.fromBlock(context.chunkPos().getMiddleBlockZ())));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        WorldGenNetherPieces.WorldGenNetherPiece15 startPiece = new WorldGenNetherPieces.WorldGenNetherPiece15(context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2));
        collector.addPiece(startPiece);
        startPiece.addChildren(startPiece, collector, context.random());
        List<StructurePiece> list = startPiece.pendingChildren;

        while(!list.isEmpty()) {
            int i = context.random().nextInt(list.size());
            StructurePiece structurePiece = list.remove(i);
            structurePiece.addChildren(startPiece, collector, context.random());
        }

        collector.moveInsideHeights(context.random(), 48, 70);
    }
}
