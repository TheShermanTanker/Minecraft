package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickListChunk;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.chunk.BiomeStorage;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.chunk.ProtoChunkTickList;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkRegionLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String TAG_UPGRADE_DATA = "UpgradeData";

    public static ProtoChunk loadChunk(WorldServer world, DefinedStructureManager structureManager, VillagePlace poiStorage, ChunkCoordIntPair pos, NBTTagCompound nbt) {
        ChunkGenerator chunkGenerator = world.getChunkSource().getChunkGenerator();
        WorldChunkManager biomeSource = chunkGenerator.getWorldChunkManager();
        NBTTagCompound compoundTag = nbt.getCompound("Level");
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(compoundTag.getInt("xPos"), compoundTag.getInt("zPos"));
        if (!Objects.equals(pos, chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, chunkPos);
        }

        BiomeStorage chunkBiomeContainer = new BiomeStorage(world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), world, pos, biomeSource, compoundTag.hasKeyOfType("Biomes", 11) ? compoundTag.getIntArray("Biomes") : null);
        ChunkConverter upgradeData = compoundTag.hasKeyOfType("UpgradeData", 10) ? new ChunkConverter(compoundTag.getCompound("UpgradeData"), world) : ChunkConverter.EMPTY;
        ProtoChunkTickList<Block> protoTickList = new ProtoChunkTickList<>((block) -> {
            return block == null || block.getBlockData().isAir();
        }, pos, compoundTag.getList("ToBeTicked", 9), world);
        ProtoChunkTickList<FluidType> protoTickList2 = new ProtoChunkTickList<>((fluid) -> {
            return fluid == null || fluid == FluidTypes.EMPTY;
        }, pos, compoundTag.getList("LiquidsToBeTicked", 9), world);
        boolean bl = compoundTag.getBoolean("isLightOn");
        NBTTagList listTag = compoundTag.getList("Sections", 10);
        int i = world.getSectionsCount();
        ChunkSection[] levelChunkSections = new ChunkSection[i];
        boolean bl2 = world.getDimensionManager().hasSkyLight();
        IChunkProvider chunkSource = world.getChunkSource();
        LightEngine levelLightEngine = chunkSource.getLightEngine();
        if (bl) {
            levelLightEngine.retainData(pos, true);
        }

        for(int j = 0; j < listTag.size(); ++j) {
            NBTTagCompound compoundTag2 = listTag.getCompound(j);
            int k = compoundTag2.getByte("Y");
            if (compoundTag2.hasKeyOfType("Palette", 9) && compoundTag2.hasKeyOfType("BlockStates", 12)) {
                ChunkSection levelChunkSection = new ChunkSection(k);
                levelChunkSection.getBlocks().read(compoundTag2.getList("Palette", 10), compoundTag2.getLongArray("BlockStates"));
                levelChunkSection.recalcBlockCounts();
                if (!levelChunkSection.isEmpty()) {
                    levelChunkSections[world.getSectionIndexFromSectionY(k)] = levelChunkSection;
                }

                poiStorage.checkConsistencyWithBlocks(pos, levelChunkSection);
            }

            if (bl) {
                if (compoundTag2.hasKeyOfType("BlockLight", 7)) {
                    levelLightEngine.queueSectionData(EnumSkyBlock.BLOCK, SectionPosition.of(pos, k), new NibbleArray(compoundTag2.getByteArray("BlockLight")), true);
                }

                if (bl2 && compoundTag2.hasKeyOfType("SkyLight", 7)) {
                    levelLightEngine.queueSectionData(EnumSkyBlock.SKY, SectionPosition.of(pos, k), new NibbleArray(compoundTag2.getByteArray("SkyLight")), true);
                }
            }
        }

        long l = compoundTag.getLong("InhabitedTime");
        ChunkStatus.Type chunkType = getChunkTypeFromTag(nbt);
        IChunkAccess chunkAccess;
        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            TickList<Block> tickList;
            if (compoundTag.hasKeyOfType("TileTicks", 9)) {
                tickList = TickListChunk.create(compoundTag.getList("TileTicks", 10), IRegistry.BLOCK::getKey, IRegistry.BLOCK::get);
            } else {
                tickList = protoTickList;
            }

            TickList<FluidType> tickList3;
            if (compoundTag.hasKeyOfType("LiquidTicks", 9)) {
                tickList3 = TickListChunk.create(compoundTag.getList("LiquidTicks", 10), IRegistry.FLUID::getKey, IRegistry.FLUID::get);
            } else {
                tickList3 = protoTickList2;
            }

            chunkAccess = new Chunk(world.getLevel(), pos, chunkBiomeContainer, upgradeData, tickList, tickList3, l, levelChunkSections, (levelChunk) -> {
                loadEntities(world, compoundTag, levelChunk);
            });
        } else {
            ProtoChunk protoChunk = new ProtoChunk(pos, upgradeData, levelChunkSections, protoTickList, protoTickList2, world);
            protoChunk.setBiomes(chunkBiomeContainer);
            chunkAccess = protoChunk;
            protoChunk.setInhabitedTime(l);
            protoChunk.setStatus(ChunkStatus.byName(compoundTag.getString("Status")));
            if (protoChunk.getChunkStatus().isOrAfter(ChunkStatus.FEATURES)) {
                protoChunk.setLightEngine(levelLightEngine);
            }

            if (!bl && protoChunk.getChunkStatus().isOrAfter(ChunkStatus.LIGHT)) {
                for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.getMinBlockX(), world.getMinBuildHeight(), pos.getMinBlockZ(), pos.getMaxBlockX(), world.getMaxBuildHeight() - 1, pos.getMaxBlockZ())) {
                    if (chunkAccess.getType(blockPos).getLightEmission() != 0) {
                        protoChunk.addLight(blockPos);
                    }
                }
            }
        }

        chunkAccess.setLightCorrect(bl);
        NBTTagCompound compoundTag3 = compoundTag.getCompound("Heightmaps");
        EnumSet<HeightMap.Type> enumSet = EnumSet.noneOf(HeightMap.Type.class);

        for(HeightMap.Type types : chunkAccess.getChunkStatus().heightmapsAfter()) {
            String string = types.getSerializationKey();
            if (compoundTag3.hasKeyOfType(string, 12)) {
                chunkAccess.setHeightmap(types, compoundTag3.getLongArray(string));
            } else {
                enumSet.add(types);
            }
        }

        HeightMap.primeHeightmaps(chunkAccess, enumSet);
        NBTTagCompound compoundTag4 = compoundTag.getCompound("Structures");
        chunkAccess.setAllStarts(unpackStructureStart(world, compoundTag4, world.getSeed()));
        chunkAccess.setAllReferences(unpackStructureReferences(pos, compoundTag4));
        if (compoundTag.getBoolean("shouldSave")) {
            chunkAccess.setNeedsSaving(true);
        }

        NBTTagList listTag2 = compoundTag.getList("PostProcessing", 9);

        for(int m = 0; m < listTag2.size(); ++m) {
            NBTTagList listTag3 = listTag2.getList(m);

            for(int n = 0; n < listTag3.size(); ++n) {
                chunkAccess.addPackedPostProcess(listTag3.getShort(n), m);
            }
        }

        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            return new ProtoChunkExtension((Chunk)chunkAccess);
        } else {
            ProtoChunk protoChunk2 = (ProtoChunk)chunkAccess;
            NBTTagList listTag4 = compoundTag.getList("Entities", 10);

            for(int o = 0; o < listTag4.size(); ++o) {
                protoChunk2.addEntity(listTag4.getCompound(o));
            }

            NBTTagList listTag5 = compoundTag.getList("TileEntities", 10);

            for(int p = 0; p < listTag5.size(); ++p) {
                NBTTagCompound compoundTag5 = listTag5.getCompound(p);
                chunkAccess.setBlockEntityNbt(compoundTag5);
            }

            NBTTagList listTag6 = compoundTag.getList("Lights", 9);

            for(int q = 0; q < listTag6.size(); ++q) {
                NBTTagList listTag7 = listTag6.getList(q);

                for(int r = 0; r < listTag7.size(); ++r) {
                    protoChunk2.addLight(listTag7.getShort(r), q);
                }
            }

            NBTTagCompound compoundTag6 = compoundTag.getCompound("CarvingMasks");

            for(String string2 : compoundTag6.getKeys()) {
                WorldGenStage.Features carving = WorldGenStage.Features.valueOf(string2);
                protoChunk2.setCarvingMask(carving, BitSet.valueOf(compoundTag6.getByteArray(string2)));
            }

            return protoChunk2;
        }
    }

    public static NBTTagCompound saveChunk(WorldServer world, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagCompound compoundTag2 = new NBTTagCompound();
        compoundTag.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        compoundTag.set("Level", compoundTag2);
        compoundTag2.setInt("xPos", chunkPos.x);
        compoundTag2.setInt("zPos", chunkPos.z);
        compoundTag2.setLong("LastUpdate", world.getTime());
        compoundTag2.setLong("InhabitedTime", chunk.getInhabitedTime());
        compoundTag2.setString("Status", chunk.getChunkStatus().getName());
        ChunkConverter upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isEmpty()) {
            compoundTag2.set("UpgradeData", upgradeData.write());
        }

        ChunkSection[] levelChunkSections = chunk.getSections();
        NBTTagList listTag = new NBTTagList();
        LightEngine levelLightEngine = world.getChunkSource().getLightEngine();
        boolean bl = chunk.isLightCorrect();

        for(int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i) {
            int j = i;
            ChunkSection levelChunkSection = Arrays.stream(levelChunkSections).filter((chunkSection) -> {
                return chunkSection != null && SectionPosition.blockToSectionCoord(chunkSection.getYPosition()) == j;
            }).findFirst().orElse(Chunk.EMPTY_SECTION);
            NibbleArray dataLayer = levelLightEngine.getLayerListener(EnumSkyBlock.BLOCK).getDataLayerData(SectionPosition.of(chunkPos, j));
            NibbleArray dataLayer2 = levelLightEngine.getLayerListener(EnumSkyBlock.SKY).getDataLayerData(SectionPosition.of(chunkPos, j));
            if (levelChunkSection != Chunk.EMPTY_SECTION || dataLayer != null || dataLayer2 != null) {
                NBTTagCompound compoundTag3 = new NBTTagCompound();
                compoundTag3.setByte("Y", (byte)(j & 255));
                if (levelChunkSection != Chunk.EMPTY_SECTION) {
                    levelChunkSection.getBlocks().write(compoundTag3, "Palette", "BlockStates");
                }

                if (dataLayer != null && !dataLayer.isEmpty()) {
                    compoundTag3.setByteArray("BlockLight", dataLayer.asBytes());
                }

                if (dataLayer2 != null && !dataLayer2.isEmpty()) {
                    compoundTag3.setByteArray("SkyLight", dataLayer2.asBytes());
                }

                listTag.add(compoundTag3);
            }
        }

        compoundTag2.set("Sections", listTag);
        if (bl) {
            compoundTag2.setBoolean("isLightOn", true);
        }

        BiomeStorage chunkBiomeContainer = chunk.getBiomeIndex();
        if (chunkBiomeContainer != null) {
            compoundTag2.setIntArray("Biomes", chunkBiomeContainer.writeBiomes());
        }

        NBTTagList listTag2 = new NBTTagList();

        for(BlockPosition blockPos : chunk.getBlockEntitiesPos()) {
            NBTTagCompound compoundTag4 = chunk.getBlockEntityNbtForSaving(blockPos);
            if (compoundTag4 != null) {
                listTag2.add(compoundTag4);
            }
        }

        compoundTag2.set("TileEntities", listTag2);
        if (chunk.getChunkStatus().getType() == ChunkStatus.Type.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            NBTTagList listTag3 = new NBTTagList();
            listTag3.addAll(protoChunk.getEntities());
            compoundTag2.set("Entities", listTag3);
            compoundTag2.set("Lights", packOffsets(protoChunk.getPackedLights()));
            NBTTagCompound compoundTag5 = new NBTTagCompound();

            for(WorldGenStage.Features carving : WorldGenStage.Features.values()) {
                BitSet bitSet = protoChunk.getCarvingMask(carving);
                if (bitSet != null) {
                    compoundTag5.setByteArray(carving.toString(), bitSet.toByteArray());
                }
            }

            compoundTag2.set("CarvingMasks", compoundTag5);
        }

        TickList<Block> tickList = chunk.getBlockTicks();
        if (tickList instanceof ProtoChunkTickList) {
            compoundTag2.set("ToBeTicked", ((ProtoChunkTickList)tickList).save());
        } else if (tickList instanceof TickListChunk) {
            compoundTag2.set("TileTicks", ((TickListChunk)tickList).save());
        } else {
            compoundTag2.set("TileTicks", world.getBlockTicks().save(chunkPos));
        }

        TickList<FluidType> tickList2 = chunk.getLiquidTicks();
        if (tickList2 instanceof ProtoChunkTickList) {
            compoundTag2.set("LiquidsToBeTicked", ((ProtoChunkTickList)tickList2).save());
        } else if (tickList2 instanceof TickListChunk) {
            compoundTag2.set("LiquidTicks", ((TickListChunk)tickList2).save());
        } else {
            compoundTag2.set("LiquidTicks", world.getLiquidTicks().save(chunkPos));
        }

        compoundTag2.set("PostProcessing", packOffsets(chunk.getPostProcessing()));
        NBTTagCompound compoundTag6 = new NBTTagCompound();

        for(Entry<HeightMap.Type, HeightMap> entry : chunk.getHeightmaps()) {
            if (chunk.getChunkStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundTag6.set(entry.getKey().getSerializationKey(), new NBTTagLongArray(entry.getValue().getRawData()));
            }
        }

        compoundTag2.set("Heightmaps", compoundTag6);
        compoundTag2.set("Structures", packStructureData(world, chunkPos, chunk.getAllStarts(), chunk.getAllReferences()));
        return compoundTag;
    }

    public static ChunkStatus.Type getChunkTypeFromTag(@Nullable NBTTagCompound nbt) {
        if (nbt != null) {
            ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getCompound("Level").getString("Status"));
            if (chunkStatus != null) {
                return chunkStatus.getType();
            }
        }

        return ChunkStatus.Type.PROTOCHUNK;
    }

    private static void loadEntities(WorldServer world, NBTTagCompound nbt, Chunk chunk) {
        if (nbt.hasKeyOfType("Entities", 9)) {
            NBTTagList listTag = nbt.getList("Entities", 10);
            if (!listTag.isEmpty()) {
                world.addLegacyChunkEntities(EntityTypes.loadEntitiesRecursive(listTag, world));
            }
        }

        NBTTagList listTag2 = nbt.getList("TileEntities", 10);

        for(int i = 0; i < listTag2.size(); ++i) {
            NBTTagCompound compoundTag = listTag2.getCompound(i);
            boolean bl = compoundTag.getBoolean("keepPacked");
            if (bl) {
                chunk.setBlockEntityNbt(compoundTag);
            } else {
                BlockPosition blockPos = new BlockPosition(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
                TileEntity blockEntity = TileEntity.create(blockPos, chunk.getType(blockPos), compoundTag);
                if (blockEntity != null) {
                    chunk.setTileEntity(blockEntity);
                }
            }
        }

    }

    private static NBTTagCompound packStructureData(WorldServer world, ChunkCoordIntPair chunkPos, Map<StructureGenerator<?>, StructureStart<?>> map, Map<StructureGenerator<?>, LongSet> map2) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagCompound compoundTag2 = new NBTTagCompound();

        for(Entry<StructureGenerator<?>, StructureStart<?>> entry : map.entrySet()) {
            compoundTag2.set(entry.getKey().getFeatureName(), entry.getValue().createTag(world, chunkPos));
        }

        compoundTag.set("Starts", compoundTag2);
        NBTTagCompound compoundTag3 = new NBTTagCompound();

        for(Entry<StructureGenerator<?>, LongSet> entry2 : map2.entrySet()) {
            compoundTag3.set(entry2.getKey().getFeatureName(), new NBTTagLongArray(entry2.getValue()));
        }

        compoundTag.set("References", compoundTag3);
        return compoundTag;
    }

    private static Map<StructureGenerator<?>, StructureStart<?>> unpackStructureStart(WorldServer serverLevel, NBTTagCompound nbt, long worldSeed) {
        Map<StructureGenerator<?>, StructureStart<?>> map = Maps.newHashMap();
        NBTTagCompound compoundTag = nbt.getCompound("Starts");

        for(String string : compoundTag.getKeys()) {
            String string2 = string.toLowerCase(Locale.ROOT);
            StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string2);
            if (structureFeature == null) {
                LOGGER.error("Unknown structure start: {}", (Object)string2);
            } else {
                StructureStart<?> structureStart = StructureGenerator.loadStaticStart(serverLevel, compoundTag.getCompound(string), worldSeed);
                if (structureStart != null) {
                    map.put(structureFeature, structureStart);
                }
            }
        }

        return map;
    }

    private static Map<StructureGenerator<?>, LongSet> unpackStructureReferences(ChunkCoordIntPair pos, NBTTagCompound nbt) {
        Map<StructureGenerator<?>, LongSet> map = Maps.newHashMap();
        NBTTagCompound compoundTag = nbt.getCompound("References");

        for(String string : compoundTag.getKeys()) {
            String string2 = string.toLowerCase(Locale.ROOT);
            StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string2);
            if (structureFeature == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", string2, pos);
            } else {
                map.put(structureFeature, new LongOpenHashSet(Arrays.stream(compoundTag.getLongArray(string)).filter((packedPos) -> {
                    ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(packedPos);
                    if (chunkPos2.getChessboardDistance(pos) > 8) {
                        LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", string2, chunkPos2, pos);
                        return false;
                    } else {
                        return true;
                    }
                }).toArray()));
            }
        }

        return map;
    }

    public static NBTTagList packOffsets(ShortList[] lists) {
        NBTTagList listTag = new NBTTagList();

        for(ShortList shortList : lists) {
            NBTTagList listTag2 = new NBTTagList();
            if (shortList != null) {
                for(Short short_ : shortList) {
                    listTag2.add(NBTTagShort.valueOf(short_));
                }
            }

            listTag.add(listTag2);
        }

        return listTag;
    }
}
