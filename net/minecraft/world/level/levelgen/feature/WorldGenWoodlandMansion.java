package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenWoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenWoodlandMansion extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenWoodlandMansion(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
        for(BiomeBase biome2 : biomeSource.getBiomesWithin(pos.getBlockX(9), chunkGenerator.getSeaLevel(), pos.getBlockZ(9), 32)) {
            if (!biome2.getGenerationSettings().isValidStart(this)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenWoodlandMansion.WoodlandMansionStart::new;
    }

    public static class WoodlandMansionStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        public WoodlandMansionStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            EnumBlockRotation rotation = EnumBlockRotation.getRandom(this.random);
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

            int k = pos.getBlockX(7);
            int l = pos.getBlockZ(7);
            int m = chunkGenerator.getFirstOccupiedHeight(k, l, HeightMap.Type.WORLD_SURFACE_WG, world);
            int n = chunkGenerator.getFirstOccupiedHeight(k, l + j, HeightMap.Type.WORLD_SURFACE_WG, world);
            int o = chunkGenerator.getFirstOccupiedHeight(k + i, l, HeightMap.Type.WORLD_SURFACE_WG, world);
            int p = chunkGenerator.getFirstOccupiedHeight(k + i, l + j, HeightMap.Type.WORLD_SURFACE_WG, world);
            int q = Math.min(Math.min(m, n), Math.min(o, p));
            if (q >= 60) {
                BlockPosition blockPos = new BlockPosition(pos.getBlockX(8), q + 1, pos.getBlockZ(8));
                List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list = Lists.newLinkedList();
                WorldGenWoodlandMansionPieces.generateMansion(manager, blockPos, rotation, list, this.random);
                list.forEach(this::addPiece);
            }
        }

        @Override
        public void placeInChunk(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox box, ChunkCoordIntPair chunkPos) {
            super.placeInChunk(world, structureAccessor, chunkGenerator, random, box, chunkPos);
            StructureBoundingBox boundingBox = this.getBoundingBox();
            int i = boundingBox.minY();

            for(int j = box.minX(); j <= box.maxX(); ++j) {
                for(int k = box.minZ(); k <= box.maxZ(); ++k) {
                    BlockPosition blockPos = new BlockPosition(j, i, k);
                    if (!world.isEmpty(blockPos) && boundingBox.isInside(blockPos) && this.isInsidePiece(blockPos)) {
                        for(int l = i - 1; l > 1; --l) {
                            BlockPosition blockPos2 = new BlockPosition(j, l, k);
                            if (!world.isEmpty(blockPos2) && !world.getType(blockPos2).getMaterial().isLiquid()) {
                                break;
                            }

                            world.setTypeAndData(blockPos2, Blocks.COBBLESTONE.getBlockData(), 2);
                        }
                    }
                }
            }

        }
    }
}
