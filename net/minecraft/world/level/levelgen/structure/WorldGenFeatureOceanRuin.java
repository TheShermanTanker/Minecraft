package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.INamable;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureOceanRuin extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration> {
    public WorldGenFeatureOceanRuin(Codec<WorldGenFeatureOceanRuinConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureOceanRuinConfiguration> getStartFactory() {
        return WorldGenFeatureOceanRuin.OceanRuinStart::new;
    }

    public static class OceanRuinStart extends StructureStart<WorldGenFeatureOceanRuinConfiguration> {
        public OceanRuinStart(StructureGenerator<WorldGenFeatureOceanRuinConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureOceanRuinConfiguration config, IWorldHeightAccess world) {
            BlockPosition blockPos = new BlockPosition(pos.getMinBlockX(), 90, pos.getMinBlockZ());
            EnumBlockRotation rotation = EnumBlockRotation.getRandom(this.random);
            WorldGenFeatureOceanRuinPieces.addPieces(manager, blockPos, rotation, this, this.random, config);
        }
    }

    public static enum Temperature implements INamable {
        WARM("warm"),
        COLD("cold");

        public static final Codec<WorldGenFeatureOceanRuin.Temperature> CODEC = INamable.fromEnum(WorldGenFeatureOceanRuin.Temperature::values, WorldGenFeatureOceanRuin.Temperature::byName);
        private static final Map<String, WorldGenFeatureOceanRuin.Temperature> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenFeatureOceanRuin.Temperature::getName, (type) -> {
            return type;
        }));
        private final String name;

        private Temperature(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public static WorldGenFeatureOceanRuin.Temperature byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
