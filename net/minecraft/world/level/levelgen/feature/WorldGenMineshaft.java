package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenMineshaftPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenMineshaft extends StructureGenerator<WorldGenMineshaftConfiguration> {
    public WorldGenMineshaft(Codec<WorldGenMineshaftConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenMineshaft::checkLocation, WorldGenMineshaft::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenMineshaftConfiguration> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        double d = (double)(context.config()).probability;
        return worldgenRandom.nextDouble() >= d ? false : context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(context.chunkPos().getMiddleBlockX()), QuartPos.fromBlock(50), QuartPos.fromBlock(context.chunkPos().getMiddleBlockZ())));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenMineshaftConfiguration> context) {
        WorldGenMineshaftPieces.WorldGenMineshaftRoom mineShaftRoom = new WorldGenMineshaftPieces.WorldGenMineshaftRoom(0, context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2), (context.config()).type);
        collector.addPiece(mineShaftRoom);
        mineShaftRoom.addChildren(mineShaftRoom, collector, context.random());
        int i = context.chunkGenerator().getSeaLevel();
        if ((context.config()).type == WorldGenMineshaft.Type.MESA) {
            BlockPosition blockPos = collector.getBoundingBox().getCenter();
            int j = context.chunkGenerator().getBaseHeight(blockPos.getX(), blockPos.getZ(), HeightMap.Type.WORLD_SURFACE_WG, context.heightAccessor());
            int k = j <= i ? i : MathHelper.randomBetweenInclusive(context.random(), i, j);
            int l = k - blockPos.getY();
            collector.offsetPiecesVertically(l);
        } else {
            collector.moveBelowSeaLevel(i, context.chunkGenerator().getMinY(), context.random(), 10);
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
