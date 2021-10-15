package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import net.minecraft.FileUtils;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SavedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefinedStructureManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String STRUCTURE_DIRECTORY_NAME = "structures";
    private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
    private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
    private final Map<MinecraftKey, Optional<DefinedStructure>> structureRepository = Maps.newConcurrentMap();
    private final DataFixer fixerUpper;
    private IResourceManager resourceManager;
    private final Path generatedDir;

    public DefinedStructureManager(IResourceManager resourceManager, Convertable.ConversionSession session, DataFixer dataFixer) {
        this.resourceManager = resourceManager;
        this.fixerUpper = dataFixer;
        this.generatedDir = session.getWorldFolder(SavedFile.GENERATED_DIR).normalize();
    }

    public DefinedStructure getOrCreate(MinecraftKey id) {
        Optional<DefinedStructure> optional = this.get(id);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            DefinedStructure structureTemplate = new DefinedStructure();
            this.structureRepository.put(id, Optional.of(structureTemplate));
            return structureTemplate;
        }
    }

    public Optional<DefinedStructure> get(MinecraftKey id) {
        return this.structureRepository.computeIfAbsent(id, (resourceLocation) -> {
            Optional<DefinedStructure> optional = this.loadFromGenerated(resourceLocation);
            return optional.isPresent() ? optional : this.loadFromResource(resourceLocation);
        });
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.structureRepository.clear();
    }

    private Optional<DefinedStructure> loadFromResource(MinecraftKey id) {
        MinecraftKey resourceLocation = new MinecraftKey(id.getNamespace(), "structures/" + id.getKey() + ".nbt");

        try {
            IResource resource = this.resourceManager.getResource(resourceLocation);

            Optional var4;
            try {
                var4 = Optional.of(this.readStructure(resource.getInputStream()));
            } catch (Throwable var7) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (resource != null) {
                resource.close();
            }

            return var4;
        } catch (FileNotFoundException var8) {
            return Optional.empty();
        } catch (Throwable var9) {
            LOGGER.error("Couldn't load structure {}: {}", id, var9.toString());
            return Optional.empty();
        }
    }

    private Optional<DefinedStructure> loadFromGenerated(MinecraftKey id) {
        if (!this.generatedDir.toFile().isDirectory()) {
            return Optional.empty();
        } else {
            Path path = this.createAndValidatePathToStructure(id, ".nbt");

            try {
                InputStream inputStream = new FileInputStream(path.toFile());

                Optional var4;
                try {
                    var4 = Optional.of(this.readStructure(inputStream));
                } catch (Throwable var7) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }

                    throw var7;
                }

                inputStream.close();
                return var4;
            } catch (FileNotFoundException var8) {
                return Optional.empty();
            } catch (IOException var9) {
                LOGGER.error("Couldn't load structure from {}", path, var9);
                return Optional.empty();
            }
        }
    }

    private DefinedStructure readStructure(InputStream structureInputStream) throws IOException {
        NBTTagCompound compoundTag = NBTCompressedStreamTools.readCompressed(structureInputStream);
        return this.readStructure(compoundTag);
    }

    public DefinedStructure readStructure(NBTTagCompound nbt) {
        if (!nbt.hasKeyOfType("DataVersion", 99)) {
            nbt.setInt("DataVersion", 500);
        }

        DefinedStructure structureTemplate = new DefinedStructure();
        structureTemplate.load(GameProfileSerializer.update(this.fixerUpper, DataFixTypes.STRUCTURE, nbt, nbt.getInt("DataVersion")));
        return structureTemplate;
    }

    public boolean save(MinecraftKey id) {
        Optional<DefinedStructure> optional = this.structureRepository.get(id);
        if (!optional.isPresent()) {
            return false;
        } else {
            DefinedStructure structureTemplate = optional.get();
            Path path = this.createAndValidatePathToStructure(id, ".nbt");
            Path path2 = path.getParent();
            if (path2 == null) {
                return false;
            } else {
                try {
                    Files.createDirectories(Files.exists(path2) ? path2.toRealPath() : path2);
                } catch (IOException var13) {
                    LOGGER.error("Failed to create parent directory: {}", (Object)path2);
                    return false;
                }

                NBTTagCompound compoundTag = structureTemplate.save(new NBTTagCompound());

                try {
                    OutputStream outputStream = new FileOutputStream(path.toFile());

                    try {
                        NBTCompressedStreamTools.writeCompressed(compoundTag, outputStream);
                    } catch (Throwable var11) {
                        try {
                            outputStream.close();
                        } catch (Throwable var10) {
                            var11.addSuppressed(var10);
                        }

                        throw var11;
                    }

                    outputStream.close();
                    return true;
                } catch (Throwable var12) {
                    return false;
                }
            }
        }
    }

    public Path createPathToStructure(MinecraftKey id, String extension) {
        try {
            Path path = this.generatedDir.resolve(id.getNamespace());
            Path path2 = path.resolve("structures");
            return FileUtils.createPathToResource(path2, id.getKey(), extension);
        } catch (InvalidPathException var5) {
            throw new ResourceKeyInvalidException("Invalid resource path: " + id, var5);
        }
    }

    private Path createAndValidatePathToStructure(MinecraftKey id, String extension) {
        if (id.getKey().contains("//")) {
            throw new ResourceKeyInvalidException("Invalid resource path: " + id);
        } else {
            Path path = this.createPathToStructure(id, extension);
            if (path.startsWith(this.generatedDir) && FileUtils.isPathNormalized(path) && FileUtils.isPathPortable(path)) {
                return path;
            } else {
                throw new ResourceKeyInvalidException("Invalid resource path: " + path);
            }
        }
    }

    public void remove(MinecraftKey id) {
        this.structureRepository.remove(id);
    }
}
