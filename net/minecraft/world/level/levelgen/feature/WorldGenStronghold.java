package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureStart;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenStrongholdPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenStronghold extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenStronghold(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenStronghold.StrongholdStart::new;
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
        return chunkGenerator.hasStronghold(pos);
    }

    public static class StrongholdStart extends NoiseAffectingStructureStart<WorldGenFeatureEmptyConfiguration> {
        private final long seed;

        public StrongholdStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
            this.seed = seed;
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            int i = 0;

            WorldGenStrongholdPieces.WorldGenStrongholdStart startPiece;
            do {
                this.clearPieces();
                this.random.setLargeFeatureSeed(this.seed + (long)(i++), pos.x, pos.z);
                WorldGenStrongholdPieces.resetPieces();
                startPiece = new WorldGenStrongholdPieces.WorldGenStrongholdStart(this.random, pos.getBlockX(2), pos.getBlockZ(2));
                this.addPiece(startPiece);
                startPiece.addChildren(startPiece, this, this.random);
                List<StructurePiece> list = startPiece.pendingChildren;

                while(!list.isEmpty()) {
                    int j = this.random.nextInt(list.size());
                    StructurePiece structurePiece = list.remove(j);
                    structurePiece.addChildren(startPiece, this, this.random);
                }

                this.moveBelowSeaLevel(chunkGenerator.getSeaLevel(), chunkGenerator.getMinY(), this.random, 10);
            } while(this.hasNoPieces() || startPiece.portalRoomPiece == null);

        }
    }
}
