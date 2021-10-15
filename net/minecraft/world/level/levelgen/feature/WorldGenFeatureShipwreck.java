package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenShipwreck;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureShipwreck extends StructureGenerator<WorldGenFeatureShipwreckConfiguration> {
    public WorldGenFeatureShipwreck(Codec<WorldGenFeatureShipwreckConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureShipwreckConfiguration> getStartFactory() {
        return WorldGenFeatureShipwreck.FeatureStart::new;
    }

    public static class FeatureStart extends StructureStart<WorldGenFeatureShipwreckConfiguration> {
        public FeatureStart(StructureGenerator<WorldGenFeatureShipwreckConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureShipwreckConfiguration config, IWorldHeightAccess world) {
            EnumBlockRotation rotation = EnumBlockRotation.getRandom(this.random);
            BlockPosition blockPos = new BlockPosition(pos.getMinBlockX(), 90, pos.getMinBlockZ());
            WorldGenShipwreck.addPieces(manager, blockPos, rotation, this, this.random, config);
        }
    }
}
