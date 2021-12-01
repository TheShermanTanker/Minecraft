package net.minecraft.util.worldupdate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenCustomHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.IChunkLoader;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldPersistentData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldUpgrader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setDaemon(true).build();
    private final GeneratorSettings worldGenSettings;
    private final boolean eraseCache;
    private final Convertable.ConversionSession levelStorage;
    private final Thread thread;
    private final DataFixer dataFixer;
    private volatile boolean running = true;
    private volatile boolean finished;
    private volatile float progress;
    private volatile int totalChunks;
    private volatile int converted;
    private volatile int skipped;
    private final Object2FloatMap<ResourceKey<World>> progressMap = Object2FloatMaps.synchronize(new Object2FloatOpenCustomHashMap<>(SystemUtils.identityStrategy()));
    private volatile IChatBaseComponent status = new ChatMessage("optimizeWorld.stage.counting");
    public static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
    private final WorldPersistentData overworldDataStorage;

    public WorldUpgrader(Convertable.ConversionSession session, DataFixer dataFixer, GeneratorSettings generatorOptions, boolean eraseCache) {
        this.worldGenSettings = generatorOptions;
        this.eraseCache = eraseCache;
        this.dataFixer = dataFixer;
        this.levelStorage = session;
        this.overworldDataStorage = new WorldPersistentData(this.levelStorage.getDimensionPath(World.OVERWORLD).resolve("data").toFile(), dataFixer);
        this.thread = THREAD_FACTORY.newThread(this::work);
        this.thread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Error upgrading world", throwable);
            this.status = new ChatMessage("optimizeWorld.stage.failed");
            this.finished = true;
        });
        this.thread.start();
    }

    public void cancel() {
        this.running = false;

        try {
            this.thread.join();
        } catch (InterruptedException var2) {
        }

    }

    private void work() {
        this.totalChunks = 0;
        Builder<ResourceKey<World>, ListIterator<ChunkCoordIntPair>> builder = ImmutableMap.builder();
        ImmutableSet<ResourceKey<World>> immutableSet = this.worldGenSettings.levels();

        for(ResourceKey<World> resourceKey : immutableSet) {
            List<ChunkCoordIntPair> list = this.getAllChunkPos(resourceKey);
            builder.put(resourceKey, list.listIterator());
            this.totalChunks += list.size();
        }

        if (this.totalChunks == 0) {
            this.finished = true;
        } else {
            float f = (float)this.totalChunks;
            ImmutableMap<ResourceKey<World>, ListIterator<ChunkCoordIntPair>> immutableMap = builder.build();
            Builder<ResourceKey<World>, IChunkLoader> builder2 = ImmutableMap.builder();

            for(ResourceKey<World> resourceKey2 : immutableSet) {
                Path path = this.levelStorage.getDimensionPath(resourceKey2);
                builder2.put(resourceKey2, new IChunkLoader(path.resolve("region"), this.dataFixer, true));
            }

            ImmutableMap<ResourceKey<World>, IChunkLoader> immutableMap2 = builder2.build();
            long l = SystemUtils.getMonotonicMillis();
            this.status = new ChatMessage("optimizeWorld.stage.upgrading");

            while(this.running) {
                boolean bl = false;
                float g = 0.0F;

                for(ResourceKey<World> resourceKey3 : immutableSet) {
                    ListIterator<ChunkCoordIntPair> listIterator = immutableMap.get(resourceKey3);
                    IChunkLoader chunkStorage = immutableMap2.get(resourceKey3);
                    if (listIterator.hasNext()) {
                        ChunkCoordIntPair chunkPos = listIterator.next();
                        boolean bl2 = false;

                        try {
                            NBTTagCompound compoundTag = chunkStorage.read(chunkPos);
                            if (compoundTag != null) {
                                int i = IChunkLoader.getVersion(compoundTag);
                                ChunkGenerator chunkGenerator = this.worldGenSettings.dimensions().get(GeneratorSettings.levelToLevelStem(resourceKey3)).generator();
                                NBTTagCompound compoundTag2 = chunkStorage.upgradeChunkTag(resourceKey3, () -> {
                                    return this.overworldDataStorage;
                                }, compoundTag, chunkGenerator.getTypeNameForDataFixer());
                                ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(compoundTag2.getInt("xPos"), compoundTag2.getInt("zPos"));
                                if (!chunkPos2.equals(chunkPos)) {
                                    LOGGER.warn("Chunk {} has invalid position {}", chunkPos, chunkPos2);
                                }

                                boolean bl3 = i < SharedConstants.getCurrentVersion().getWorldVersion();
                                if (this.eraseCache) {
                                    bl3 = bl3 || compoundTag2.hasKey("Heightmaps");
                                    compoundTag2.remove("Heightmaps");
                                    bl3 = bl3 || compoundTag2.hasKey("isLightOn");
                                    compoundTag2.remove("isLightOn");
                                }

                                if (bl3) {
                                    chunkStorage.write(chunkPos, compoundTag2);
                                    bl2 = true;
                                }
                            }
                        } catch (ReportedException var24) {
                            Throwable throwable = var24.getCause();
                            if (!(throwable instanceof IOException)) {
                                throw var24;
                            }

                            LOGGER.error("Error upgrading chunk {}", chunkPos, throwable);
                        } catch (IOException var25) {
                            LOGGER.error("Error upgrading chunk {}", chunkPos, var25);
                        }

                        if (bl2) {
                            ++this.converted;
                        } else {
                            ++this.skipped;
                        }

                        bl = true;
                    }

                    float h = (float)listIterator.nextIndex() / f;
                    this.progressMap.put(resourceKey3, h);
                    g += h;
                }

                this.progress = g;
                if (!bl) {
                    this.running = false;
                }
            }

            this.status = new ChatMessage("optimizeWorld.stage.finished");

            for(IChunkLoader chunkStorage2 : immutableMap2.values()) {
                try {
                    chunkStorage2.close();
                } catch (IOException var23) {
                    LOGGER.error("Error upgrading chunk", (Throwable)var23);
                }
            }

            this.overworldDataStorage.save();
            l = SystemUtils.getMonotonicMillis() - l;
            LOGGER.info("World optimizaton finished after {} ms", (long)l);
            this.finished = true;
        }
    }

    private List<ChunkCoordIntPair> getAllChunkPos(ResourceKey<World> world) {
        File file = this.levelStorage.getDimensionPath(world).toFile();
        File file2 = new File(file, "region");
        File[] files = file2.listFiles((directory, name) -> {
            return name.endsWith(".mca");
        });
        if (files == null) {
            return ImmutableList.of();
        } else {
            List<ChunkCoordIntPair> list = Lists.newArrayList();

            for(File file3 : files) {
                Matcher matcher = REGEX.matcher(file3.getName());
                if (matcher.matches()) {
                    int i = Integer.parseInt(matcher.group(1)) << 5;
                    int j = Integer.parseInt(matcher.group(2)) << 5;

                    try {
                        RegionFile regionFile = new RegionFile(file3.toPath(), file2.toPath(), true);

                        try {
                            for(int k = 0; k < 32; ++k) {
                                for(int l = 0; l < 32; ++l) {
                                    ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(k + i, l + j);
                                    if (regionFile.doesChunkExist(chunkPos)) {
                                        list.add(chunkPos);
                                    }
                                }
                            }
                        } catch (Throwable var18) {
                            try {
                                regionFile.close();
                            } catch (Throwable var17) {
                                var18.addSuppressed(var17);
                            }

                            throw var18;
                        }

                        regionFile.close();
                    } catch (Throwable var19) {
                    }
                }
            }

            return list;
        }
    }

    public boolean isFinished() {
        return this.finished;
    }

    public ImmutableSet<ResourceKey<World>> levels() {
        return this.worldGenSettings.levels();
    }

    public float dimensionProgress(ResourceKey<World> world) {
        return this.progressMap.getFloat(world);
    }

    public float getProgress() {
        return this.progress;
    }

    public int getTotalChunks() {
        return this.totalChunks;
    }

    public int getConverted() {
        return this.converted;
    }

    public int getSkipped() {
        return this.skipped;
    }

    public IChatBaseComponent getStatus() {
        return this.status;
    }
}
