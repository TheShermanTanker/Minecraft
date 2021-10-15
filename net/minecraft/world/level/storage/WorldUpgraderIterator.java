package net.minecraft.world.level.storage;

import com.google.common.collect.Lists;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.biome.WorldChunkManagerHell;
import net.minecraft.world.level.biome.WorldChunkManagerOverworld;
import net.minecraft.world.level.chunk.storage.OldChunkLoader;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldUpgraderIterator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MCREGION_EXTENSION = ".mcr";

    static boolean convertLevel(Convertable.ConversionSession storageSession, IProgressUpdate progressListener) {
        progressListener.progressStagePercentage(0);
        List<File> list = Lists.newArrayList();
        List<File> list2 = Lists.newArrayList();
        List<File> list3 = Lists.newArrayList();
        File file = storageSession.getDimensionPath(World.OVERWORLD);
        File file2 = storageSession.getDimensionPath(World.NETHER);
        File file3 = storageSession.getDimensionPath(World.END);
        LOGGER.info("Scanning folders...");
        addRegionFiles(file, list);
        if (file2.exists()) {
            addRegionFiles(file2, list2);
        }

        if (file3.exists()) {
            addRegionFiles(file3, list3);
        }

        int i = list.size() + list2.size() + list3.size();
        LOGGER.info("Total conversion count is {}", (int)i);
        IRegistryCustom.Dimension registryHolder = IRegistryCustom.builtin();
        RegistryReadOps<NBTBase> registryReadOps = RegistryReadOps.createAndLoad(DynamicOpsNBT.INSTANCE, IResourceManager.Empty.INSTANCE, registryHolder);
        SaveData worldData = storageSession.getDataTag(registryReadOps, DataPackConfiguration.DEFAULT);
        long l = worldData != null ? worldData.getGeneratorSettings().getSeed() : 0L;
        IRegistry<BiomeBase> registry = registryHolder.registryOrThrow(IRegistry.BIOME_REGISTRY);
        WorldChunkManager biomeSource;
        if (worldData != null && worldData.getGeneratorSettings().isFlatWorld()) {
            biomeSource = new WorldChunkManagerHell(registry.getOrThrow(Biomes.PLAINS));
        } else {
            biomeSource = new WorldChunkManagerOverworld(l, false, false, registry);
        }

        convertRegions(registryHolder, new File(file, "region"), list, biomeSource, 0, i, progressListener);
        convertRegions(registryHolder, new File(file2, "region"), list2, new WorldChunkManagerHell(registry.getOrThrow(Biomes.NETHER_WASTES)), list.size(), i, progressListener);
        convertRegions(registryHolder, new File(file3, "region"), list3, new WorldChunkManagerHell(registry.getOrThrow(Biomes.THE_END)), list.size() + list2.size(), i, progressListener);
        makeMcrLevelDatBackup(storageSession);
        storageSession.saveDataTag(registryHolder, worldData);
        return true;
    }

    private static void makeMcrLevelDatBackup(Convertable.ConversionSession storageSession) {
        File file = storageSession.getWorldFolder(SavedFile.LEVEL_DATA_FILE).toFile();
        if (!file.exists()) {
            LOGGER.warn("Unable to create level.dat_mcr backup");
        } else {
            File file2 = new File(file.getParent(), "level.dat_mcr");
            if (!file.renameTo(file2)) {
                LOGGER.warn("Unable to create level.dat_mcr backup");
            }

        }
    }

    private static void convertRegions(IRegistryCustom.Dimension registryManager, File directory, Iterable<File> files, WorldChunkManager biomeSource, int i, int j, IProgressUpdate progressListener) {
        for(File file : files) {
            convertRegion(registryManager, directory, file, biomeSource, i, j, progressListener);
            ++i;
            int k = (int)Math.round(100.0D * (double)i / (double)j);
            progressListener.progressStagePercentage(k);
        }

    }

    private static void convertRegion(IRegistryCustom.Dimension registryManager, File directory, File file, WorldChunkManager biomeSource, int i, int j, IProgressUpdate progressListener) {
        String string = file.getName();

        try {
            RegionFile regionFile = new RegionFile(file, directory, true);

            try {
                RegionFile regionFile2 = new RegionFile(new File(directory, string.substring(0, string.length() - ".mcr".length()) + ".mca"), directory, true);

                try {
                    for(int k = 0; k < 32; ++k) {
                        for(int l = 0; l < 32; ++l) {
                            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(k, l);
                            if (regionFile.chunkExists(chunkPos) && !regionFile2.chunkExists(chunkPos)) {
                                NBTTagCompound compoundTag;
                                try {
                                    DataInputStream dataInputStream = regionFile.getChunkDataInputStream(chunkPos);

                                    label111: {
                                        try {
                                            if (dataInputStream != null) {
                                                compoundTag = NBTCompressedStreamTools.read(dataInputStream);
                                                break label111;
                                            }

                                            LOGGER.warn("Failed to fetch input stream for chunk {}", (Object)chunkPos);
                                        } catch (Throwable var26) {
                                            if (dataInputStream != null) {
                                                try {
                                                    dataInputStream.close();
                                                } catch (Throwable var24) {
                                                    var26.addSuppressed(var24);
                                                }
                                            }

                                            throw var26;
                                        }

                                        if (dataInputStream != null) {
                                            dataInputStream.close();
                                        }
                                        continue;
                                    }

                                    if (dataInputStream != null) {
                                        dataInputStream.close();
                                    }
                                } catch (IOException var27) {
                                    LOGGER.warn("Failed to read data for chunk {}", chunkPos, var27);
                                    continue;
                                }

                                NBTTagCompound compoundTag4 = compoundTag.getCompound("Level");
                                OldChunkLoader.OldChunk oldLevelChunk = OldChunkLoader.load(compoundTag4);
                                NBTTagCompound compoundTag5 = new NBTTagCompound();
                                NBTTagCompound compoundTag6 = new NBTTagCompound();
                                compoundTag5.set("Level", compoundTag6);
                                OldChunkLoader.convertToAnvilFormat(registryManager, oldLevelChunk, compoundTag6, biomeSource);
                                DataOutputStream dataOutputStream = regionFile2.getChunkDataOutputStream(chunkPos);

                                try {
                                    NBTCompressedStreamTools.write(compoundTag5, dataOutputStream);
                                } catch (Throwable var25) {
                                    if (dataOutputStream != null) {
                                        try {
                                            dataOutputStream.close();
                                        } catch (Throwable var23) {
                                            var25.addSuppressed(var23);
                                        }
                                    }

                                    throw var25;
                                }

                                if (dataOutputStream != null) {
                                    dataOutputStream.close();
                                }
                            }
                        }

                        int m = (int)Math.round(100.0D * (double)(i * 1024) / (double)(j * 1024));
                        int n = (int)Math.round(100.0D * (double)((k + 1) * 32 + i * 1024) / (double)(j * 1024));
                        if (n > m) {
                            progressListener.progressStagePercentage(n);
                        }
                    }
                } catch (Throwable var28) {
                    try {
                        regionFile2.close();
                    } catch (Throwable var22) {
                        var28.addSuppressed(var22);
                    }

                    throw var28;
                }

                regionFile2.close();
            } catch (Throwable var29) {
                try {
                    regionFile.close();
                } catch (Throwable var21) {
                    var29.addSuppressed(var21);
                }

                throw var29;
            }

            regionFile.close();
        } catch (IOException var30) {
            LOGGER.error("Failed to upgrade region file {}", file, var30);
        }

    }

    private static void addRegionFiles(File worldDirectory, Collection<File> files) {
        File file = new File(worldDirectory, "region");
        File[] files2 = file.listFiles((directory, name) -> {
            return name.endsWith(".mcr");
        });
        if (files2 != null) {
            Collections.addAll(files, files2);
        }

    }
}
