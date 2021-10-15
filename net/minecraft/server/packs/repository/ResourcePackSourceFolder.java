package net.minecraft.server.packs.repository;

import java.io.File;
import java.io.FileFilter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.packs.IResourcePack;
import net.minecraft.server.packs.ResourcePackFile;
import net.minecraft.server.packs.ResourcePackFolder;

public class ResourcePackSourceFolder implements ResourcePackSource {
    private static final FileFilter RESOURCEPACK_FILTER = (file) -> {
        boolean bl = file.isFile() && file.getName().endsWith(".zip");
        boolean bl2 = file.isDirectory() && (new File(file, "pack.mcmeta")).isFile();
        return bl || bl2;
    };
    private final File folder;
    private final PackSource packSource;

    public ResourcePackSourceFolder(File packsFolder, PackSource source) {
        this.folder = packsFolder;
        this.packSource = source;
    }

    @Override
    public void loadPacks(Consumer<ResourcePackLoader> profileAdder, ResourcePackLoader.PackConstructor factory) {
        if (!this.folder.isDirectory()) {
            this.folder.mkdirs();
        }

        File[] files = this.folder.listFiles(RESOURCEPACK_FILTER);
        if (files != null) {
            for(File file : files) {
                String string = "file/" + file.getName();
                ResourcePackLoader pack = ResourcePackLoader.create(string, false, this.createSupplier(file), factory, ResourcePackLoader.Position.TOP, this.packSource);
                if (pack != null) {
                    profileAdder.accept(pack);
                }
            }

        }
    }

    private Supplier<IResourcePack> createSupplier(File file) {
        return file.isDirectory() ? () -> {
            return new ResourcePackFolder(file);
        } : () -> {
            return new ResourcePackFile(file);
        };
    }
}
