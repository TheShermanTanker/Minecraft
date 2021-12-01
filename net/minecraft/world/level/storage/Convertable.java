package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.FileUtils;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.SessionLock;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Convertable {
    static final Logger LOGGER = LogManager.getLogger();
    static final DateTimeFormatter FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();
    private static final String ICON_FILENAME = "icon.png";
    private static final ImmutableList<String> OLD_SETTINGS_KEYS = ImmutableList.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");
    public final Path baseDir;
    private final Path backupDir;
    final DataFixer fixerUpper;

    public Convertable(Path savesDirectory, Path backupsDirectory, DataFixer dataFixer) {
        this.fixerUpper = dataFixer;

        try {
            Files.createDirectories(Files.exists(savesDirectory) ? savesDirectory.toRealPath() : savesDirectory);
        } catch (IOException var5) {
            throw new RuntimeException(var5);
        }

        this.baseDir = savesDirectory;
        this.backupDir = backupsDirectory;
    }

    public static Convertable createDefault(Path path) {
        return new Convertable(path, path.resolve("../backups"), DataConverterRegistry.getDataFixer());
    }

    private static <T> Pair<GeneratorSettings, Lifecycle> readWorldGenSettings(Dynamic<T> levelData, DataFixer dataFixer, int version) {
        Dynamic<T> dynamic = levelData.get("WorldGenSettings").orElseEmptyMap();

        for(String string : OLD_SETTINGS_KEYS) {
            Optional<? extends Dynamic<?>> optional = levelData.get(string).result();
            if (optional.isPresent()) {
                dynamic = dynamic.set(string, optional.get());
            }
        }

        Dynamic<T> dynamic2 = dataFixer.update(DataConverterTypes.WORLD_GEN_SETTINGS, dynamic, version, SharedConstants.getCurrentVersion().getWorldVersion());
        DataResult<GeneratorSettings> dataResult = GeneratorSettings.CODEC.parse(dynamic2);
        return Pair.of(dataResult.resultOrPartial(SystemUtils.prefix("WorldGenSettings: ", LOGGER::error)).orElseGet(() -> {
            IRegistryCustom registryAccess = IRegistryCustom.Dimension.readFromDisk(dynamic2);
            return GeneratorSettings.makeDefault(registryAccess);
        }), dataResult.lifecycle());
    }

    private static DataPackConfiguration readDataPackConfig(Dynamic<?> dynamic) {
        return DataPackConfiguration.CODEC.parse(dynamic).resultOrPartial(LOGGER::error).orElse(DataPackConfiguration.DEFAULT);
    }

    public String getName() {
        return "Anvil";
    }

    public List<WorldInfo> getLevelList() throws LevelStorageException {
        if (!Files.isDirectory(this.baseDir)) {
            throw new LevelStorageException((new ChatMessage("selectWorld.load_folder_access")).getString());
        } else {
            List<WorldInfo> list = Lists.newArrayList();
            File[] files = this.baseDir.toFile().listFiles();

            for(File file : files) {
                if (file.isDirectory()) {
                    boolean bl;
                    try {
                        bl = SessionLock.isLocked(file.toPath());
                    } catch (Exception var10) {
                        LOGGER.warn("Failed to read {} lock", file, var10);
                        continue;
                    }

                    try {
                        WorldInfo levelSummary = this.readLevelData(file, this.levelSummaryReader(file, bl));
                        if (levelSummary != null) {
                            list.add(levelSummary);
                        }
                    } catch (OutOfMemoryError var9) {
                        MemoryReserve.release();
                        System.gc();
                        LOGGER.fatal("Ran out of memory trying to read summary of {}", (Object)file);
                        throw var9;
                    }
                }
            }

            return list;
        }
    }

    private int getStorageVersion() {
        return 19133;
    }

    @Nullable
    <T> T readLevelData(File file, BiFunction<File, DataFixer, T> levelDataParser) {
        if (!file.exists()) {
            return (T)null;
        } else {
            File file2 = new File(file, "level.dat");
            if (file2.exists()) {
                T object = levelDataParser.apply(file2, this.fixerUpper);
                if (object != null) {
                    return object;
                }
            }

            file2 = new File(file, "level.dat_old");
            return (T)(file2.exists() ? levelDataParser.apply(file2, this.fixerUpper) : null);
        }
    }

    @Nullable
    private static DataPackConfiguration getDataPacks(File file, DataFixer dataFixer) {
        try {
            NBTTagCompound compoundTag = NBTCompressedStreamTools.readCompressed(file);
            NBTTagCompound compoundTag2 = compoundTag.getCompound("Data");
            compoundTag2.remove("Player");
            int i = compoundTag2.hasKeyOfType("DataVersion", 99) ? compoundTag2.getInt("DataVersion") : -1;
            Dynamic<NBTBase> dynamic = dataFixer.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(DynamicOpsNBT.INSTANCE, compoundTag2), i, SharedConstants.getCurrentVersion().getWorldVersion());
            return dynamic.get("DataPacks").result().map(Convertable::readDataPackConfig).orElse(DataPackConfiguration.DEFAULT);
        } catch (Exception var6) {
            LOGGER.error("Exception reading {}", file, var6);
            return null;
        }
    }

    static BiFunction<File, DataFixer, WorldDataServer> getLevelData(DynamicOps<NBTBase> dynamicOps, DataPackConfiguration dataPackSettings) {
        return (file, dataFixer) -> {
            try {
                NBTTagCompound compoundTag = NBTCompressedStreamTools.readCompressed(file);
                NBTTagCompound compoundTag2 = compoundTag.getCompound("Data");
                NBTTagCompound compoundTag3 = compoundTag2.hasKeyOfType("Player", 10) ? compoundTag2.getCompound("Player") : null;
                compoundTag2.remove("Player");
                int i = compoundTag2.hasKeyOfType("DataVersion", 99) ? compoundTag2.getInt("DataVersion") : -1;
                Dynamic<NBTBase> dynamic = dataFixer.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(dynamicOps, compoundTag2), i, SharedConstants.getCurrentVersion().getWorldVersion());
                Pair<GeneratorSettings, Lifecycle> pair = readWorldGenSettings(dynamic, dataFixer, i);
                LevelVersion levelVersion = LevelVersion.parse(dynamic);
                WorldSettings levelSettings = WorldSettings.parse(dynamic, dataPackSettings);
                return WorldDataServer.parse(dynamic, dataFixer, i, compoundTag3, levelSettings, levelVersion, pair.getFirst(), pair.getSecond());
            } catch (Exception var12) {
                LOGGER.error("Exception reading {}", file, var12);
                return null;
            }
        };
    }

    BiFunction<File, DataFixer, WorldInfo> levelSummaryReader(File file, boolean locked) {
        return (filex, dataFixer) -> {
            try {
                NBTTagCompound compoundTag = NBTCompressedStreamTools.readCompressed(filex);
                NBTTagCompound compoundTag2 = compoundTag.getCompound("Data");
                compoundTag2.remove("Player");
                int i = compoundTag2.hasKeyOfType("DataVersion", 99) ? compoundTag2.getInt("DataVersion") : -1;
                Dynamic<NBTBase> dynamic = dataFixer.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(DynamicOpsNBT.INSTANCE, compoundTag2), i, SharedConstants.getCurrentVersion().getWorldVersion());
                LevelVersion levelVersion = LevelVersion.parse(dynamic);
                int j = levelVersion.levelDataVersion();
                if (j != 19132 && j != 19133) {
                    return null;
                } else {
                    boolean bl2 = j != this.getStorageVersion();
                    File file3 = new File(file, "icon.png");
                    DataPackConfiguration dataPackConfig = dynamic.get("DataPacks").result().map(Convertable::readDataPackConfig).orElse(DataPackConfiguration.DEFAULT);
                    WorldSettings levelSettings = WorldSettings.parse(dynamic, dataPackConfig);
                    return new WorldInfo(levelSettings, levelVersion, file.getName(), bl2, locked, file3);
                }
            } catch (Exception var15) {
                LOGGER.error("Exception reading {}", filex, var15);
                return null;
            }
        };
    }

    public boolean isNewLevelIdAcceptable(String name) {
        try {
            Path path = this.baseDir.resolve(name);
            Files.createDirectory(path);
            Files.deleteIfExists(path);
            return true;
        } catch (IOException var3) {
            return false;
        }
    }

    public boolean levelExists(String name) {
        return Files.isDirectory(this.baseDir.resolve(name));
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    public Path getBackupPath() {
        return this.backupDir;
    }

    public Convertable.ConversionSession createAccess(String directoryName) throws IOException {
        return new Convertable.ConversionSession(directoryName);
    }

    public class ConversionSession implements AutoCloseable {
        final SessionLock lock;
        public final Path levelPath;
        private final String levelId;
        private final Map<SavedFile, Path> resources = Maps.newHashMap();

        public ConversionSession(String directoryName) throws IOException {
            this.levelId = directoryName;
            this.levelPath = Convertable.this.baseDir.resolve(directoryName);
            this.lock = SessionLock.create(this.levelPath);
        }

        public String getLevelName() {
            return this.levelId;
        }

        public Path getWorldFolder(SavedFile savePath) {
            return this.resources.computeIfAbsent(savePath, (path) -> {
                return this.levelPath.resolve(path.getId());
            });
        }

        public Path getDimensionPath(ResourceKey<World> key) {
            return DimensionManager.getStorageFolder(key, this.levelPath);
        }

        private void checkSession() {
            if (!this.lock.isValid()) {
                throw new IllegalStateException("Lock is no longer valid");
            }
        }

        public WorldNBTStorage createPlayerStorage() {
            this.checkSession();
            return new WorldNBTStorage(this, Convertable.this.fixerUpper);
        }

        @Nullable
        public WorldInfo getSummary() {
            this.checkSession();
            return Convertable.this.readLevelData(this.levelPath.toFile(), Convertable.this.levelSummaryReader(this.levelPath.toFile(), false));
        }

        @Nullable
        public SaveData getDataTag(DynamicOps<NBTBase> dynamicOps, DataPackConfiguration dataPackSettings) {
            this.checkSession();
            return Convertable.this.readLevelData(this.levelPath.toFile(), Convertable.getLevelData(dynamicOps, dataPackSettings));
        }

        @Nullable
        public DataPackConfiguration getDataPacks() {
            this.checkSession();
            return Convertable.this.readLevelData(this.levelPath.toFile(), Convertable::getDataPacks);
        }

        public void saveDataTag(IRegistryCustom registryManager, SaveData saveProperties) {
            this.saveDataTag(registryManager, saveProperties, (NBTTagCompound)null);
        }

        public void saveDataTag(IRegistryCustom registryManager, SaveData saveProperties, @Nullable NBTTagCompound nbt) {
            File file = this.levelPath.toFile();
            NBTTagCompound compoundTag = saveProperties.createTag(registryManager, nbt);
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            compoundTag2.set("Data", compoundTag);

            try {
                File file2 = File.createTempFile("level", ".dat", file);
                NBTCompressedStreamTools.writeCompressed(compoundTag2, file2);
                File file3 = new File(file, "level.dat_old");
                File file4 = new File(file, "level.dat");
                SystemUtils.safeReplaceFile(file4, file2, file3);
            } catch (Exception var10) {
                Convertable.LOGGER.error("Failed to save level {}", file, var10);
            }

        }

        public Optional<Path> getIconFile() {
            return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelPath.resolve("icon.png"));
        }

        public void deleteLevel() throws IOException {
            this.checkSession();
            final Path path = this.levelPath.resolve("session.lock");

            for(int i = 1; i <= 5; ++i) {
                Convertable.LOGGER.info("Attempt {}...", (int)i);

                try {
                    Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                            if (!path.equals(path)) {
                                Convertable.LOGGER.debug("Deleting {}", (Object)path);
                                Files.delete(path);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
                            if (iOException != null) {
                                throw iOException;
                            } else {
                                if (path.equals(ConversionSession.this.levelPath)) {
                                    ConversionSession.this.lock.close();
                                    Files.deleteIfExists(path);
                                }

                                Files.delete(path);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    });
                    break;
                } catch (IOException var6) {
                    if (i >= 5) {
                        throw var6;
                    }

                    Convertable.LOGGER.warn("Failed to delete {}", this.levelPath, var6);

                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException var5) {
                    }
                }
            }

        }

        public void renameLevel(String name) throws IOException {
            this.checkSession();
            File file = new File(Convertable.this.baseDir.toFile(), this.levelId);
            if (file.exists()) {
                File file2 = new File(file, "level.dat");
                if (file2.exists()) {
                    NBTTagCompound compoundTag = NBTCompressedStreamTools.readCompressed(file2);
                    NBTTagCompound compoundTag2 = compoundTag.getCompound("Data");
                    compoundTag2.setString("LevelName", name);
                    NBTCompressedStreamTools.writeCompressed(compoundTag, file2);
                }

            }
        }

        public long makeWorldBackup() throws IOException {
            this.checkSession();
            String string = LocalDateTime.now().format(Convertable.FORMATTER) + "_" + this.levelId;
            Path path = Convertable.this.getBackupPath();

            try {
                Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
            } catch (IOException var9) {
                throw new RuntimeException(var9);
            }

            Path path2 = path.resolve(FileUtils.findAvailableName(path, string, ".zip"));
            final ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path2)));

            try {
                final Path path3 = Paths.get(this.levelId);
                Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        if (path.endsWith("session.lock")) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            String string = path3.resolve(ConversionSession.this.levelPath.relativize(path)).toString().replace('\\', '/');
                            ZipEntry zipEntry = new ZipEntry(string);
                            zipOutputStream.putNextEntry(zipEntry);
                            com.google.common.io.Files.asByteSource(path.toFile()).copyTo(zipOutputStream);
                            zipOutputStream.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
            } catch (Throwable var8) {
                try {
                    zipOutputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            zipOutputStream.close();
            return Files.size(path2);
        }

        @Override
        public void close() throws IOException {
            this.lock.close();
        }
    }
}
