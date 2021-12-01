package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.BlockAccessAir;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.configurations.RangeConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class WorldGenFeatureNetherFossil extends NoiseAffectingStructureFeature<RangeConfiguration> {
    public WorldGenFeatureNetherFossil(Codec<RangeConfiguration> configCodec) {
        super(configCodec, WorldGenFeatureNetherFossil::pieceGeneratorSupplier);
    }

    private static Optional<PieceGenerator<RangeConfiguration>> pieceGeneratorSupplier(PieceGeneratorSupplier.Context<RangeConfiguration> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        int i = context.chunkPos().getMinBlockX() + worldgenRandom.nextInt(16);
        int j = context.chunkPos().getMinBlockZ() + worldgenRandom.nextInt(16);
        int k = context.chunkGenerator().getSeaLevel();
        WorldGenerationContext worldGenerationContext = new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor());
        int l = (context.config()).height.sample(worldgenRandom, worldGenerationContext);
        BlockColumn noiseColumn = context.chunkGenerator().getBaseColumn(i, j, context.heightAccessor());
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(i, l, j);

        while(l > k) {
            IBlockData blockState = noiseColumn.getBlock(l);
            --l;
            IBlockData blockState2 = noiseColumn.getBlock(l);
            if (blockState.isAir() && (blockState2.is(Blocks.SOUL_SAND) || blockState2.isFaceSturdy(BlockAccessAir.INSTANCE, mutableBlockPos.setY(l), EnumDirection.UP))) {
                break;
            }
        }

        if (l <= k) {
            return Optional.empty();
        } else if (!context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(l), QuartPos.fromBlock(j)))) {
            return Optional.empty();
        } else {
            BlockPosition blockPos = new BlockPosition(i, l, j);
            return Optional.of((structurePiecesBuilder, context2) -> {
                WorldGenNetherFossil.addPieces(context.structureManager(), structurePiecesBuilder, worldgenRandom, blockPos);
            });
        }
    }
}
