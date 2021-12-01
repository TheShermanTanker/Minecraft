package net.minecraft.world.level.levelgen.structure;

import java.util.Map;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenShipwreck {
    static final BlockPosition PIVOT = new BlockPosition(4, 0, 15);
    private static final MinecraftKey[] STRUCTURE_LOCATION_BEACHED = new MinecraftKey[]{new MinecraftKey("shipwreck/with_mast"), new MinecraftKey("shipwreck/sideways_full"), new MinecraftKey("shipwreck/sideways_fronthalf"), new MinecraftKey("shipwreck/sideways_backhalf"), new MinecraftKey("shipwreck/rightsideup_full"), new MinecraftKey("shipwreck/rightsideup_fronthalf"), new MinecraftKey("shipwreck/rightsideup_backhalf"), new MinecraftKey("shipwreck/with_mast_degraded"), new MinecraftKey("shipwreck/rightsideup_full_degraded"), new MinecraftKey("shipwreck/rightsideup_fronthalf_degraded"), new MinecraftKey("shipwreck/rightsideup_backhalf_degraded")};
    private static final MinecraftKey[] STRUCTURE_LOCATION_OCEAN = new MinecraftKey[]{new MinecraftKey("shipwreck/with_mast"), new MinecraftKey("shipwreck/upsidedown_full"), new MinecraftKey("shipwreck/upsidedown_fronthalf"), new MinecraftKey("shipwreck/upsidedown_backhalf"), new MinecraftKey("shipwreck/sideways_full"), new MinecraftKey("shipwreck/sideways_fronthalf"), new MinecraftKey("shipwreck/sideways_backhalf"), new MinecraftKey("shipwreck/rightsideup_full"), new MinecraftKey("shipwreck/rightsideup_fronthalf"), new MinecraftKey("shipwreck/rightsideup_backhalf"), new MinecraftKey("shipwreck/with_mast_degraded"), new MinecraftKey("shipwreck/upsidedown_full_degraded"), new MinecraftKey("shipwreck/upsidedown_fronthalf_degraded"), new MinecraftKey("shipwreck/upsidedown_backhalf_degraded"), new MinecraftKey("shipwreck/sideways_full_degraded"), new MinecraftKey("shipwreck/sideways_fronthalf_degraded"), new MinecraftKey("shipwreck/sideways_backhalf_degraded"), new MinecraftKey("shipwreck/rightsideup_full_degraded"), new MinecraftKey("shipwreck/rightsideup_fronthalf_degraded"), new MinecraftKey("shipwreck/rightsideup_backhalf_degraded")};
    static final Map<String, MinecraftKey> MARKERS_TO_LOOT = Map.of("map_chest", LootTables.SHIPWRECK_MAP, "treasure_chest", LootTables.SHIPWRECK_TREASURE, "supply_chest", LootTables.SHIPWRECK_SUPPLY);

    public static void addPieces(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, StructurePieceAccessor holder, Random random, WorldGenFeatureShipwreckConfiguration config) {
        MinecraftKey resourceLocation = SystemUtils.getRandom(config.isBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, random);
        holder.addPiece(new WorldGenShipwreck.ShipwreckPiece(structureManager, resourceLocation, pos, rotation, config.isBeached));
    }

    public static class ShipwreckPiece extends DefinedStructurePiece {
        private final boolean isBeached;

        public ShipwreckPiece(DefinedStructureManager manager, MinecraftKey identifier, BlockPosition pos, EnumBlockRotation rotation, boolean grounded) {
            super(WorldGenFeatureStructurePieceType.SHIPWRECK_PIECE, 0, manager, identifier, identifier.toString(), makeSettings(rotation), pos);
            this.isBeached = grounded;
        }

        public ShipwreckPiece(DefinedStructureManager manager, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.SHIPWRECK_PIECE, nbt, manager, (resourceLocation) -> {
                return makeSettings(EnumBlockRotation.valueOf(nbt.getString("Rot")));
            });
            this.isBeached = nbt.getBoolean("isBeached");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.setBoolean("isBeached", this.isBeached);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
        }

        private static DefinedStructureInfo makeSettings(EnumBlockRotation rotation) {
            return (new DefinedStructureInfo()).setRotation(rotation).setMirror(EnumBlockMirror.NONE).setRotationPivot(WorldGenShipwreck.PIVOT).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            MinecraftKey resourceLocation = WorldGenShipwreck.MARKERS_TO_LOOT.get(metadata);
            if (resourceLocation != null) {
                TileEntityLootable.setLootTable(world, random, pos.below(), resourceLocation);
            }

        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
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
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
        }
    }
}
