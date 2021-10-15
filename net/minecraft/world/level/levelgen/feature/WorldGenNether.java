package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenNetherPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenNether extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> FORTRESS_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.BLAZE, 10, 2, 3), new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 5, 4, 4), new BiomeSettingsMobs.SpawnerData(EntityTypes.WITHER_SKELETON, 8, 5, 5), new BiomeSettingsMobs.SpawnerData(EntityTypes.SKELETON, 2, 5, 5), new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 3, 4, 4));

    public WorldGenNether(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
        return random.nextInt(5) < 2;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenNether.NetherBridgeStart::new;
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialEnemies() {
        return FORTRESS_ENEMIES;
    }

    public static class NetherBridgeStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        public NetherBridgeStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            WorldGenNetherPieces.WorldGenNetherPiece15 startPiece = new WorldGenNetherPieces.WorldGenNetherPiece15(this.random, pos.getBlockX(2), pos.getBlockZ(2));
            this.addPiece(startPiece);
            startPiece.addChildren(startPiece, this, this.random);
            List<StructurePiece> list = startPiece.pendingChildren;

            while(!list.isEmpty()) {
                int i = this.random.nextInt(list.size());
                StructurePiece structurePiece = list.remove(i);
                structurePiece.addChildren(startPiece, this, this.random);
            }

            this.moveInsideHeights(this.random, 48, 70);
        }
    }
}
