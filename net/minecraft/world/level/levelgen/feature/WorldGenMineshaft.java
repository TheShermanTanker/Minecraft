package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.INamable;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenMineshaftPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenMineshaft extends StructureGenerator<WorldGenMineshaftConfiguration> {
    public WorldGenMineshaft(Codec<WorldGenMineshaftConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenMineshaftConfiguration config, IWorldHeightAccess world) {
        random.setLargeFeatureSeed(worldSeed, pos.x, pos.z);
        double d = (double)config.probability;
        return random.nextDouble() < d;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenMineshaftConfiguration> getStartFactory() {
        return WorldGenMineshaft.MineShaftStart::new;
    }

    public static class MineShaftStart extends StructureStart<WorldGenMineshaftConfiguration> {
        public MineShaftStart(StructureGenerator<WorldGenMineshaftConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenMineshaftConfiguration config, IWorldHeightAccess world) {
            WorldGenMineshaftPieces.WorldGenMineshaftRoom mineShaftRoom = new WorldGenMineshaftPieces.WorldGenMineshaftRoom(0, this.random, pos.getBlockX(2), pos.getBlockZ(2), config.type);
            this.addPiece(mineShaftRoom);
            mineShaftRoom.addChildren(mineShaftRoom, this, this.random);
            if (config.type == WorldGenMineshaft.Type.MESA) {
                int i = -5;
                StructureBoundingBox boundingBox = this.getBoundingBox();
                int j = chunkGenerator.getSeaLevel() - boundingBox.maxY() + boundingBox.getYSpan() / 2 - -5;
                this.offsetPiecesVertically(j);
            } else {
                this.moveBelowSeaLevel(chunkGenerator.getSeaLevel(), chunkGenerator.getMinY(), this.random, 10);
            }

        }
    }

    public static enum Type implements INamable {
        NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
        MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

        public static final Codec<WorldGenMineshaft.Type> CODEC = INamable.fromEnum(WorldGenMineshaft.Type::values, WorldGenMineshaft.Type::byName);
        private static final Map<String, WorldGenMineshaft.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenMineshaft.Type::getName, (type) -> {
            return type;
        }));
        private final String name;
        private final IBlockData woodState;
        private final IBlockData planksState;
        private final IBlockData fenceState;

        private Type(String name, Block log, Block planks, Block fence) {
            this.name = name;
            this.woodState = log.getBlockData();
            this.planksState = planks.getBlockData();
            this.fenceState = fence.getBlockData();
        }

        public String getName() {
            return this.name;
        }

        private static WorldGenMineshaft.Type byName(String name) {
            return BY_NAME.get(name);
        }

        public static WorldGenMineshaft.Type byId(int index) {
            return index >= 0 && index < values().length ? values()[index] : NORMAL;
        }

        public IBlockData getWoodState() {
            return this.woodState;
        }

        public IBlockData getPlanksState() {
            return this.planksState;
        }

        public IBlockData getFenceState() {
            return this.fenceState;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
