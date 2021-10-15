package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenIglooPiece {
    public static final int GENERATION_HEIGHT = 90;
    static final MinecraftKey STRUCTURE_LOCATION_IGLOO = new MinecraftKey("igloo/top");
    private static final MinecraftKey STRUCTURE_LOCATION_LADDER = new MinecraftKey("igloo/middle");
    private static final MinecraftKey STRUCTURE_LOCATION_LABORATORY = new MinecraftKey("igloo/bottom");
    static final Map<MinecraftKey, BlockPosition> PIVOTS = ImmutableMap.of(STRUCTURE_LOCATION_IGLOO, new BlockPosition(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPosition(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPosition(3, 6, 7));
    static final Map<MinecraftKey, BlockPosition> OFFSETS = ImmutableMap.of(STRUCTURE_LOCATION_IGLOO, BlockPosition.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPosition(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPosition(0, -3, -2));

    public static void addPieces(DefinedStructureManager manager, BlockPosition pos, EnumBlockRotation rotation, StructurePieceAccessor structurePieceAccessor, Random random) {
        if (random.nextDouble() < 0.5D) {
            int i = random.nextInt(8) + 4;
            structurePieceAccessor.addPiece(new WorldGenIglooPiece.IglooPiece(manager, STRUCTURE_LOCATION_LABORATORY, pos, rotation, i * 3));

            for(int j = 0; j < i - 1; ++j) {
                structurePieceAccessor.addPiece(new WorldGenIglooPiece.IglooPiece(manager, STRUCTURE_LOCATION_LADDER, pos, rotation, j * 3));
            }
        }

        structurePieceAccessor.addPiece(new WorldGenIglooPiece.IglooPiece(manager, STRUCTURE_LOCATION_IGLOO, pos, rotation, 0));
    }

    public static class IglooPiece extends DefinedStructurePiece {
        public IglooPiece(DefinedStructureManager manager, MinecraftKey identifier, BlockPosition pos, EnumBlockRotation rotation, int yOffset) {
            super(WorldGenFeatureStructurePieceType.IGLOO, 0, manager, identifier, identifier.toString(), makeSettings(rotation, identifier), makePosition(identifier, pos, yOffset));
        }

        public IglooPiece(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.IGLOO, nbt, world, (resourceLocation) -> {
                return makeSettings(EnumBlockRotation.valueOf(nbt.getString("Rot")), resourceLocation);
            });
        }

        private static DefinedStructureInfo makeSettings(EnumBlockRotation rotation, MinecraftKey identifier) {
            return (new DefinedStructureInfo()).setRotation(rotation).setMirror(EnumBlockMirror.NONE).setRotationPivot(WorldGenIglooPiece.PIVOTS.get(identifier)).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK);
        }

        private static BlockPosition makePosition(MinecraftKey identifier, BlockPosition pos, int yOffset) {
            return pos.offset(WorldGenIglooPiece.OFFSETS.get(identifier)).below(yOffset);
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            if ("chest".equals(metadata)) {
                world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 3);
                TileEntity blockEntity = world.getTileEntity(pos.below());
                if (blockEntity instanceof TileEntityChest) {
                    ((TileEntityChest)blockEntity).setLootTable(LootTables.IGLOO_CHEST, random.nextLong());
                }

            }
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            MinecraftKey resourceLocation = new MinecraftKey(this.templateName);
            DefinedStructureInfo structurePlaceSettings = makeSettings(this.placeSettings.getRotation(), resourceLocation);
            BlockPosition blockPos = WorldGenIglooPiece.OFFSETS.get(resourceLocation);
            BlockPosition blockPos2 = this.templatePosition.offset(DefinedStructure.calculateRelativePosition(structurePlaceSettings, new BlockPosition(3 - blockPos.getX(), 0, -blockPos.getZ())));
            int i = world.getHeight(HeightMap.Type.WORLD_SURFACE_WG, blockPos2.getX(), blockPos2.getZ());
            BlockPosition blockPos3 = this.templatePosition;
            this.templatePosition = this.templatePosition.offset(0, i - 90 - 1, 0);
            boolean bl = super.postProcess(world, structureAccessor, chunkGenerator, random, boundingBox, chunkPos, pos);
            if (resourceLocation.equals(WorldGenIglooPiece.STRUCTURE_LOCATION_IGLOO)) {
                BlockPosition blockPos4 = this.templatePosition.offset(DefinedStructure.calculateRelativePosition(structurePlaceSettings, new BlockPosition(3, 0, 5)));
                IBlockData blockState = world.getType(blockPos4.below());
                if (!blockState.isAir() && !blockState.is(Blocks.LADDER)) {
                    world.setTypeAndData(blockPos4, Blocks.SNOW_BLOCK.getBlockData(), 3);
                }
            }

            this.templatePosition = blockPos3;
            return bl;
        }
    }
}
