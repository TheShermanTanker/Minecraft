package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityLootable;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenShipwreck {
    static final BlockPosition PIVOT = new BlockPosition(4, 0, 15);
    private static final MinecraftKey[] STRUCTURE_LOCATION_BEACHED = new MinecraftKey[]{new MinecraftKey("shipwreck/with_mast"), new MinecraftKey("shipwreck/sideways_full"), new MinecraftKey("shipwreck/sideways_fronthalf"), new MinecraftKey("shipwreck/sideways_backhalf"), new MinecraftKey("shipwreck/rightsideup_full"), new MinecraftKey("shipwreck/rightsideup_fronthalf"), new MinecraftKey("shipwreck/rightsideup_backhalf"), new MinecraftKey("shipwreck/with_mast_degraded"), new MinecraftKey("shipwreck/rightsideup_full_degraded"), new MinecraftKey("shipwreck/rightsideup_fronthalf_degraded"), new MinecraftKey("shipwreck/rightsideup_backhalf_degraded")};
    private static final MinecraftKey[] STRUCTURE_LOCATION_OCEAN = new MinecraftKey[]{new MinecraftKey("shipwreck/with_mast"), new MinecraftKey("shipwreck/upsidedown_full"), new MinecraftKey("shipwreck/upsidedown_fronthalf"), new MinecraftKey("shipwreck/upsidedown_backhalf"), new MinecraftKey("shipwreck/sideways_full"), new MinecraftKey("shipwreck/sideways_fronthalf"), new MinecraftKey("shipwreck/sideways_backhalf"), new MinecraftKey("shipwreck/rightsideup_full"), new MinecraftKey("shipwreck/rightsideup_fronthalf"), new MinecraftKey("shipwreck/rightsideup_backhalf"), new MinecraftKey("shipwreck/with_mast_degraded"), new MinecraftKey("shipwreck/upsidedown_full_degraded"), new MinecraftKey("shipwreck/upsidedown_fronthalf_degraded"), new MinecraftKey("shipwreck/upsidedown_backhalf_degraded"), new MinecraftKey("shipwreck/sideways_full_degraded"), new MinecraftKey("shipwreck/sideways_fronthalf_degraded"), new MinecraftKey("shipwreck/sideways_backhalf_degraded"), new MinecraftKey("shipwreck/rightsideup_full_degraded"), new MinecraftKey("shipwreck/rightsideup_fronthalf_degraded"), new MinecraftKey("shipwreck/rightsideup_backhalf_degraded")};

    public static void addPieces(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, StructurePieceAccessor structurePieceAccessor, Random random, WorldGenFeatureShipwreckConfiguration config) {
        MinecraftKey resourceLocation = SystemUtils.getRandom(config.isBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, random);
        structurePieceAccessor.addPiece(new WorldGenShipwreck.ShipwreckPiece(structureManager, resourceLocation, pos, rotation, config.isBeached));
    }

    public static class ShipwreckPiece extends DefinedStructurePiece {
        private final boolean isBeached;

        public ShipwreckPiece(DefinedStructureManager manager, MinecraftKey identifier, BlockPosition pos, EnumBlockRotation rotation, boolean grounded) {
            super(WorldGenFeatureStructurePieceType.SHIPWRECK_PIECE, 0, manager, identifier, identifier.toString(), makeSettings(rotation), pos);
            this.isBeached = grounded;
        }

        public ShipwreckPiece(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.SHIPWRECK_PIECE, nbt, world, (resourceLocation) -> {
                return makeSettings(EnumBlockRotation.valueOf(nbt.getString("Rot")));
            });
            this.isBeached = nbt.getBoolean("isBeached");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("isBeached", this.isBeached);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
        }

        private static DefinedStructureInfo makeSettings(EnumBlockRotation rotation) {
            return (new DefinedStructureInfo()).setRotation(rotation).setMirror(EnumBlockMirror.NONE).setRotationPivot(WorldGenShipwreck.PIVOT).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            if ("map_chest".equals(metadata)) {
                TileEntityLootable.setLootTable(world, random, pos.below(), LootTables.SHIPWRECK_MAP);
            } else if ("treasure_chest".equals(metadata)) {
                TileEntityLootable.setLootTable(world, random, pos.below(), LootTables.SHIPWRECK_TREASURE);
            } else if ("supply_chest".equals(metadata)) {
                TileEntityLootable.setLootTable(world, random, pos.below(), LootTables.SHIPWRECK_SUPPLY);
            }

        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            int i = world.getMaxBuildHeight();
            int j = 0;
            BaseBlockPosition vec3i = this.template.getSize();
            HeightMap.Type types = this.isBeached ? HeightMap.Type.WORLD_SURFACE_WG : HeightMap.Type.OCEAN_FLOOR_WG;
            int k = vec3i.getX() * vec3i.getZ();
            if (k == 0) {
                j = world.getHeight(types, this.templatePosition.getX(), this.templatePosition.getZ());
            } else {
                BlockPosition blockPos = this.templatePosition.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);

                for(BlockPosition blockPos2 : BlockPosition.betweenClosed(this.templatePosition, blockPos)) {
                    int l = world.getHeight(types, blockPos2.getX(), blockPos2.getZ());
                    j += l;
                    i = Math.min(i, l);
                }

                j = j / k;
            }

            int m = this.isBeached ? i - vec3i.getY() / 2 - random.nextInt(3) : j;
            this.templatePosition = new BlockPosition(this.templatePosition.getX(), m, this.templatePosition.getZ());
            return super.postProcess(world, structureAccessor, chunkGenerator, random, boundingBox, chunkPos, pos);
        }
    }
}
