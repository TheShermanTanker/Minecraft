package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.INamable;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureOceanRuin extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration> {
    public WorldGenFeatureOceanRuin(Codec<WorldGenFeatureOceanRuinConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(PieceGeneratorSupplier.checkForBiomeOnTop(HeightMap.Type.OCEAN_FLOOR_WG), WorldGenFeatureOceanRuin::generatePieces));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureOceanRuinConfiguration> context) {
        BlockPosition blockPos = new BlockPosition(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(context.random());
        WorldGenFeatureOceanRuinPieces.addPieces(context.structureManager(), blockPos, rotation, collector, context.random(), context.config());
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
