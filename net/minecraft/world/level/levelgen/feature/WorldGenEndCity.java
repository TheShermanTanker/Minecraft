package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenEndCityPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenEndCity extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final int RANDOM_SALT = 10387313;

    public WorldGenEndCity(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
        return getYPositionForFeature(pos, chunkGenerator, world) >= 60;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenEndCity.EndCityStart::new;
    }

    static int getYPositionForFeature(ChunkCoordIntPair chunkPos, ChunkGenerator chunkGenerator, IWorldHeightAccess levelHeightAccessor) {
        Random random = new Random((long)(chunkPos.x + chunkPos.z * 10387313));
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        int i = 5;
        int j = 5;
        if (rotation == EnumBlockRotation.CLOCKWISE_90) {
            i = -5;
        } else if (rotation == EnumBlockRotation.CLOCKWISE_180) {
            i = -5;
            j = -5;
        } else if (rotation == EnumBlockRotation.COUNTERCLOCKWISE_90) {
            j = -5;
        }

        int k = chunkPos.getBlockX(7);
        int l = chunkPos.getBlockZ(7);
        int m = chunkGenerator.getFirstOccupiedHeight(k, l, HeightMap.Type.WORLD_SURFACE_WG, levelHeightAccessor);
        int n = chunkGenerator.getFirstOccupiedHeight(k, l + j, HeightMap.Type.WORLD_SURFACE_WG, levelHeightAccessor);
        int o = chunkGenerator.getFirstOccupiedHeight(k + i, l, HeightMap.Type.WORLD_SURFACE_WG, levelHeightAccessor);
        int p = chunkGenerator.getFirstOccupiedHeight(k + i, l + j, HeightMap.Type.WORLD_SURFACE_WG, levelHeightAccessor);
        return Math.min(Math.min(m, n), Math.min(o, p));
    }

    public static class EndCityStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        public EndCityStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            EnumBlockRotation rotation = EnumBlockRotation.getRandom(this.random);
            int i = WorldGenEndCity.getYPositionForFeature(pos, chunkGenerator, world);
            if (i >= 60) {
                BlockPosition blockPos = pos.getMiddleBlockPosition(i);
                List<StructurePiece> list = Lists.newArrayList();
                WorldGenEndCityPieces.startHouseTower(manager, blockPos, rotation, list, this.random);
                list.forEach(this::addPiece);
            }
        }
    }
}
