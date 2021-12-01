package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
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
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkAccess$TicksToSave;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataPaletteBlock;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.chunk.LevelChunk$PostLoadProcessor;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.chunk.PalettedContainer$Strategy;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkRegionLoader {
    public static final Codec<DataPaletteBlock<IBlockData>> BLOCK_STATE_CODEC = DataPaletteBlock.codec(Block.BLOCK_STATE_REGISTRY, IBlockData.CODEC, PalettedContainer$Strategy.SECTION_STATES, Blocks.AIR.getBlockData());
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";

    public static ProtoChunk read(WorldServer world, VillagePlace poiStorage, ChunkCoordIntPair chunkPos, NBTTagCompound nbt) {
        ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(nbt.getInt("xPos"), nbt.getInt("zPos"));
        if (!Objects.equals(chunkPos, chunkPos2)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", chunkPos, chunkPos, chunkPos2);
        }

        ChunkConverter upgradeData = nbt.hasKeyOfType("UpgradeData", 10) ? new ChunkConverter(nbt.getCompound("UpgradeData"), world) : ChunkConverter.EMPTY;
        boolean bl = nbt.getBoolean("isLightOn");
        NBTTagList listTag = nbt.getList("sections", 10);
        int i = world.getSectionsCount();
        ChunkSection[] levelChunkSections = new ChunkSection[i];
        boolean bl2 = world.getDimensionManager().hasSkyLight();
        IChunkProvider chunkSource = world.getChunkSource();
        LightEngine levelLightEngine = chunkSource.getLightEngine();
        if (bl) {
            levelLightEngine.retainData(chunkPos, true);
        }

        IRegistry<BiomeBase> registry = world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY);
        Codec<DataPaletteBlock<BiomeBase>> codec = makeBiomeCodec(registry);

        for(int j = 0; j < listTag.size(); ++j) {
            NBTTagCompound compoundTag = listTag.getCompound(j);
            int k = compoundTag.getByte("Y");
            int l = world.getSectionIndexFromSectionY(k);
            if (l >= 0 && l < levelChunkSections.length) {
                DataPaletteBlock<IBlockData> palettedContainer;
                if (compoundTag.hasKeyOfType("block_states", 10)) {
                    palettedContainer = BLOCK_STATE_CODEC.parse(DynamicOpsNBT.INSTANCE, compoundTag.getCompound("block_states")).promotePartial((errorMessage) -> {
                        logErrors(chunkPos, k, errorMessage);
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedContainer = new DataPaletteBlock<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.getBlockData(), PalettedContainer$Strategy.SECTION_STATES);
                }

                DataPaletteBlock<BiomeBase> palettedContainer3;
                if (compoundTag.hasKeyOfType("biomes", 10)) {
                    palettedContainer3 = codec.parse(DynamicOpsNBT.INSTANCE, compoundTag.getCompound("biomes")).promotePartial((errorMessage) -> {
                        logErrors(chunkPos, k, errorMessage);
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedContainer3 = new DataPaletteBlock<>(registry, registry.getOrThrow(Biomes.PLAINS), PalettedContainer$Strategy.SECTION_BIOMES);
                }

                ChunkSection levelChunkSection = new ChunkSection(k, palettedContainer, palettedContainer3);
                levelChunkSections[l] = levelChunkSection;
                poiStorage.checkConsistencyWithBlocks(chunkPos, levelChunkSection);
            }

            if (bl) {
                if (compoundTag.hasKeyOfType("BlockLight", 7)) {
                    levelLightEngine.queueSectionData(EnumSkyBlock.BLOCK, SectionPosition.of(chunkPos, k), new NibbleArray(compoundTag.getByteArray("BlockLight")), true);
                }

                if (bl2 && compoundTag.hasKeyOfType("SkyLight", 7)) {
                    levelLightEngine.queueSectionData(EnumSkyBlock.SKY, SectionPosition.of(chunkPos, k), new NibbleArray(compoundTag.getByteArray("SkyLight")), true);
                }
            }
        }

        long m = nbt.getLong("InhabitedTime");
        ChunkStatus.Type chunkType = getChunkTypeFromTag(nbt);
        BlendingData blendingData;
        if (nbt.hasKeyOfType("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.getCompound("blending_data"))).resultOrPartial(LOGGER::error).orElse((BlendingData)null);
        } else {
            blendingData = null;
        }

        IChunkAccess chunkAccess;
        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            LevelChunkTicks<Block> levelChunkTicks = LevelChunkTicks.load(nbt.getList("block_ticks", 10), (id) -> {
                return IRegistry.BLOCK.getOptional(MinecraftKey.tryParse(id));
            }, chunkPos);
            LevelChunkTicks<FluidType> levelChunkTicks2 = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10), (id) -> {
                return IRegistry.FLUID.getOptional(MinecraftKey.tryParse(id));
            }, chunkPos);
            chunkAccess = new Chunk(world.getLevel(), chunkPos, upgradeData, levelChunkTicks, levelChunkTicks2, m, levelChunkSections, postLoadChunk(world, nbt), blendingData);
        } else {
            ProtoChunkTicks<Block> protoChunkTicks = ProtoChunkTicks.load(nbt.getList("block_ticks", 10), (id) -> {
                return IRegistry.BLOCK.getOptional(MinecraftKey.tryParse(id));
            }, chunkPos);
            ProtoChunkTicks<FluidType> protoChunkTicks2 = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10), (id) -> {
                return IRegistry.FLUID.getOptional(MinecraftKey.tryParse(id));
            }, chunkPos);
            ProtoChunk protoChunk = new ProtoChunk(chunkPos, upgradeData, levelChunkSections, protoChunkTicks, protoChunkTicks2, world, registry, blendingData);
            chunkAccess = protoChunk;
            protoChunk.setInhabitedTime(m);
            if (nbt.hasKeyOfType("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.getCompound("below_zero_retrogen"))).resultOrPartial(LOGGER::error).ifPresent(protoChunk::setBelowZeroRetrogen);
            }

            ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            protoChunk.setStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.FEATURES)) {
                protoChunk.setLightEngine(levelLightEngine);
            }

            BelowZeroRetrogen belowZeroRetrogen = protoChunk.getBelowZeroRetrogen();
            boolean bl3 = chunkStatus.isOrAfter(ChunkStatus.LIGHT) || belowZeroRetrogen != null && belowZeroRetrogen.targetStatus().isOrAfter(ChunkStatus.LIGHT);
            if (!bl && bl3) {
                for(BlockPosition blockPos : BlockPosition.betweenClosed(chunkPos.getMinBlockX(), world.getMinBuildHeight(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), world.getMaxBuildHeight() - 1, chunkPos.getMaxBlockZ())) {
                    if (chunkAccess.getType(blockPos).getLightEmission() != 0) {
                        protoChunk.addLight(blockPos);
                    }
                }
            }
        }

        chunkAccess.setLightCorrect(bl);
        NBTTagCompound compoundTag2 = nbt.getCompound("Heightmaps");
        EnumSet<HeightMap.Type> enumSet = EnumSet.noneOf(HeightMap.Type.class);

        for(HeightMap.Type types : chunkAccess.getChunkStatus().heightmapsAfter()) {
            String string = types.getSerializationKey();
            if (compoundTag2.hasKeyOfType(string, 12)) {
                chunkAccess.setHeightmap(types, compoundTag2.getLongArray(string));
            } else {
                enumSet.add(types);
            }
        }

        HeightMap.primeHeightmaps(chunkAccess, enumSet);
        NBTTagCompound compoundTag3 = nbt.getCompound("structures");
        chunkAccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(world), compoundTag3, world.getSeed()));
        chunkAccess.setAllReferences(unpackStructureReferences(chunkPos, compoundTag3));
        if (nbt.getBoolean("shouldSave")) {
            chunkAccess.setNeedsSaving(true);
        }

        NBTTagList listTag2 = nbt.getList("PostProcessing", 9);

        for(int n = 0; n < listTag2.size(); ++n) {
            NBTTagList listTag3 = listTag2.getList(n);

            for(int o = 0; o < listTag3.size(); ++o) {
                chunkAccess.addPackedPostProcess(listTag3.getShort(o), n);
            }
        }

        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            return new ProtoChunkExtension((Chunk)chunkAccess, false);
        } else {
            ProtoChunk protoChunk2 = (ProtoChunk)chunkAccess;
            NBTTagList listTag4 = nbt.getList("entities", 10);

            for(int p = 0; p < listTag4.size(); ++p) {
                protoChunk2.addEntity(listTag4.getCompound(p));
            }

            NBTTagList listTag5 = nbt.getList("block_entities", 10);

            for(int q = 0; q < listTag5.size(); ++q) {
                NBTTagCompound compoundTag4 = listTag5.getCompound(q);
                chunkAccess.setBlockEntityNbt(compoundTag4);
            }

            NBTTagList listTag6 = nbt.getList("Lights", 9);

            for(int r = 0; r < listTag6.size(); ++r) {
                NBTTagList listTag7 = listTag6.getList(r);

                for(int s = 0; s < listTag7.size(); ++s) {
                    protoChunk2.addLight(listTag7.getShort(s), r);
                }
            }

            NBTTagCompound compoundTag5 = nbt.getCompound("CarvingMasks");

            for(String string2 : compoundTag5.getKeys()) {
                WorldGenStage.Features carving = WorldGenStage.Features.valueOf(string2);
                protoChunk2.setCarvingMask(carving, new CarvingMask(compoundTag5.getLongArray(string2), chunkAccess.getMinBuildHeight()));
            }

            return protoChunk2;
        }
    }

    private static void logErrors(ChunkCoordIntPair chunkPos, int y, String message) {
        LOGGER.error("Recoverable errors when loading section [" + chunkPos.x + ", " + y + ", " + chunkPos.z + "]: " + message);
    }

    private static Codec<DataPaletteBlock<BiomeBase>> makeBiomeCodec(IRegistry<BiomeBase> biomeRegistry) {
        return DataPaletteBlock.codec(biomeRegistry, biomeRegistry.byNameCodec(), PalettedContainer$Strategy.SECTION_BIOMES, biomeRegistry.getOrThrow(Biomes.PLAINS));
    }

    public static NBTTagCompound saveChunk(WorldServer world, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        compoundTag.setInt("xPos", chunkPos.x);
        compoundTag.setInt("yPos", chunk.getMinSection());
        compoundTag.setInt("zPos", chunkPos.z);
        compoundTag.setLong("LastUpdate", world.getTime());
        compoundTag.setLong("InhabitedTime", chunk.getInhabitedTime());
        compoundTag.setString("Status", chunk.getChunkStatus().getName());
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, blendingData).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                compoundTag.set("blending_data", tag);
            });
        }

        BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, belowZeroRetrogen).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                compoundTag.set("below_zero_retrogen", tag);
            });
        }

        ChunkConverter upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isEmpty()) {
            compoundTag.set("UpgradeData", upgradeData.write());
        }

        ChunkSection[] levelChunkSections = chunk.getSections();
        NBTTagList listTag = new NBTTagList();
        LightEngine levelLightEngine = world.getChunkSource().getLightEngine();
        IRegistry<BiomeBase> registry = world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY);
        Codec<DataPaletteBlock<BiomeBase>> codec = makeBiomeCodec(registry);
        boolean bl = chunk.isLightCorrect();

        for(int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i) {
            int j = chunk.getSectionIndexFromSectionY(i);
            boolean bl2 = j >= 0 && j < levelChunkSections.length;
            NibbleArray dataLayer = levelLightEngine.getLayerListener(EnumSkyBlock.BLOCK).getDataLayerData(SectionPosition.of(chunkPos, i));
            NibbleArray dataLayer2 = levelLightEngine.getLayerListener(EnumSkyBlock.SKY).getDataLayerData(SectionPosition.of(chunkPos, i));
            if (bl2 || dataLayer != null || dataLayer2 != null) {
                NBTTagCompound compoundTag2 = new NBTTagCompound();
                if (bl2) {
                    ChunkSection levelChunkSection = levelChunkSections[j];
                    compoundTag2.set("block_states", BLOCK_STATE_CODEC.encodeStart(DynamicOpsNBT.INSTANCE, levelChunkSection.getBlocks()).getOrThrow(false, LOGGER::error));
                    compoundTag2.set("biomes", codec.encodeStart(DynamicOpsNBT.INSTANCE, levelChunkSection.getBiomes()).getOrThrow(false, LOGGER::error));
                }

                if (dataLayer != null && !dataLayer.isEmpty()) {
                    compoundTag2.setByteArray("BlockLight", dataLayer.asBytes());
                }

                if (dataLayer2 != null && !dataLayer2.isEmpty()) {
                    compoundTag2.setByteArray("SkyLight", dataLayer2.asBytes());
                }

                if (!compoundTag2.isEmpty()) {
                    compoundTag2.setByte("Y", (byte)i);
                    listTag.add(compoundTag2);
                }
            }
        }

        compoundTag.set("sections", listTag);
        if (bl) {
            compoundTag.setBoolean("isLightOn", true);
        }

        NBTTagList listTag2 = new NBTTagList();

        for(BlockPosition blockPos : chunk.getBlockEntitiesPos()) {
            NBTTagCompound compoundTag3 = chunk.getBlockEntityNbtForSaving(blockPos);
            if (compoundTag3 != null) {
                listTag2.add(compoundTag3);
            }
        }

        compoundTag.set("block_entities", listTag2);
        if (chunk.getChunkStatus().getType() == ChunkStatus.Type.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            NBTTagList listTag3 = new NBTTagList();
            listTag3.addAll(protoChunk.getEntities());
            compoundTag.set("entities", listTag3);
            compoundTag.set("Lights", packOffsets(protoChunk.getPackedLights()));
            NBTTagCompound compoundTag4 = new NBTTagCompound();

            for(WorldGenStage.Features carving : WorldGenStage.Features.values()) {
                CarvingMask carvingMask = protoChunk.getCarvingMask(carving);
                if (carvingMask != null) {
                    compoundTag4.putLongArray(carving.toString(), carvingMask.toArray());
                }
            }

            compoundTag.set("CarvingMasks", compoundTag4);
        }

        saveTicks(world, compoundTag, chunk.getTicksForSerialization());
        compoundTag.set("PostProcessing", packOffsets(chunk.getPostProcessing()));
        NBTTagCompound compoundTag5 = new NBTTagCompound();

        for(Entry<HeightMap.Type, HeightMap> entry : chunk.getHeightmaps()) {
            if (chunk.getChunkStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundTag5.set(entry.getKey().getSerializationKey(), new NBTTagLongArray(entry.getValue().getRawData()));
            }
        }

        compoundTag.set("Heightmaps", compoundTag5);
        compoundTag.set("structures", packStructureData(StructurePieceSerializationContext.fromLevel(world), chunkPos, chunk.getAllStarts(), chunk.getAllReferences()));
        return compoundTag;
    }

    private static void saveTicks(WorldServer world, NBTTagCompound nbt, ChunkAccess$TicksToSave tickSchedulers) {
        long l = world.getWorldData().getTime();
        nbt.set("block_ticks", tickSchedulers.blocks().save(l, (block) -> {
            return IRegistry.BLOCK.getKey(block).toString();
        }));
        nbt.set("fluid_ticks", tickSchedulers.fluids().save(l, (fluid) -> {
            return IRegistry.FLUID.getKey(fluid).toString();
        }));
    }

    public static ChunkStatus.Type getChunkTypeFromTag(@Nullable NBTTagCompound nbt) {
        return nbt != null ? ChunkStatus.byName(nbt.getString("Status")).getType() : ChunkStatus.Type.PROTOCHUNK;
    }

    @Nullable
    private static LevelChunk$PostLoadProcessor postLoadChunk(WorldServer world, NBTTagCompound nbt) {
        NBTTagList listTag = getListOfCompoundsOrNull(nbt, "entities");
        NBTTagList listTag2 = getListOfCompoundsOrNull(nbt, "block_entities");
        return listTag == null && listTag2 == null ? null : (chunk) -> {
            if (listTag != null) {
                world.addLegacyChunkEntities(EntityTypes.loadEntitiesRecursive(listTag, world));
            }

            if (listTag2 != null) {
                for(int i = 0; i < listTag2.size(); ++i) {
                    NBTTagCompound compoundTag = listTag2.getCompound(i);
                    boolean bl = compoundTag.getBoolean("keepPacked");
                    if (bl) {
                        chunk.setBlockEntityNbt(compoundTag);
                    } else {
                        BlockPosition blockPos = TileEntity.getPosFromTag(compoundTag);
                        TileEntity blockEntity = TileEntity.create(blockPos, chunk.getType(blockPos), compoundTag);
                        if (blockEntity != null) {
                            chunk.setTileEntity(blockEntity);
                        }
                    }
                }
            }

        };
    }

    @Nullable
    private static NBTTagList getListOfCompoundsOrNull(NBTTagCompound nbt, String key) {
        NBTTagList listTag = nbt.getList(key, 10);
        return listTag.isEmpty() ? null : listTag;
    }

    private static NBTTagCompound packStructureData(StructurePieceSerializationContext context, ChunkCoordIntPair pos, Map<StructureGenerator<?>, StructureStart<?>> starts, Map<StructureGenerator<?>, LongSet> references) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagCompound compoundTag2 = new NBTTagCompound();

        for(Entry<StructureGenerator<?>, StructureStart<?>> entry : starts.entrySet()) {
            compoundTag2.set(entry.getKey().getFeatureName(), entry.getValue().createTag(context, pos));
        }

        compoundTag.set("starts", compoundTag2);
        NBTTagCompound compoundTag3 = new NBTTagCompound();

        for(Entry<StructureGenerator<?>, LongSet> entry2 : references.entrySet()) {
            compoundTag3.set(entry2.getKey().getFeatureName(), new NBTTagLongArray(entry2.getValue()));
        }

        compoundTag.set("References", compoundTag3);
        return compoundTag;
    }

    private static Map<StructureGenerator<?>, StructureStart<?>> unpackStructureStart(StructurePieceSerializationContext context, NBTTagCompound nbt, long worldSeed) {
        Map<StructureGenerator<?>, StructureStart<?>> map = Maps.newHashMap();
        NBTTagCompound compoundTag = nbt.getCompound("starts");

        for(String string : compoundTag.getKeys()) {
            String string2 = string.toLowerCase(Locale.ROOT);
            StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string2);
            if (structureFeature == null) {
                LOGGER.error("Unknown structure start: {}", (Object)string2);
            } else {
                StructureStart<?> structureStart = StructureGenerator.loadStaticStart(context, compoundTag.getCompound(string), worldSeed);
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
