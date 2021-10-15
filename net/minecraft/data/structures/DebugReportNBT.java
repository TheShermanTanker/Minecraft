package net.minecraft.data.structures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugReportNBT implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DebugReportGenerator generator;

    public DebugReportNBT(DebugReportGenerator root) {
        this.generator = root;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        Path path = this.generator.getOutputFolder();

        for(Path path2 : this.generator.getInputFolders()) {
            Files.walk(path2).filter((pathx) -> {
                return pathx.toString().endsWith(".nbt");
            }).forEach((path3) -> {
                convertStructure(path3, this.getName(path2, path3), path);
            });
        }

    }

    @Override
    public String getName() {
        return "NBT to SNBT";
    }

    private String getName(Path targetPath, Path rootPath) {
        String string = targetPath.relativize(rootPath).toString().replaceAll("\\\\", "/");
        return string.substring(0, string.length() - ".nbt".length());
    }

    @Nullable
    public static Path convertStructure(Path inputPath, String location, Path outputPath) {
        try {
            writeSnbt(outputPath.resolve(location + ".snbt"), GameProfileSerializer.structureToSnbt(NBTCompressedStreamTools.readCompressed(Files.newInputStream(inputPath))));
            LOGGER.info("Converted {} from NBT to SNBT", (Object)location);
            return outputPath.resolve(location + ".snbt");
        } catch (IOException var4) {
            LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", location, inputPath, var4);
            return null;
        }
    }

    public static void writeSnbt(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        BufferedWriter bufferedWriter = Files.newBufferedWriter(file);

        try {
            bufferedWriter.write(content);
            bufferedWriter.write(10);
        } catch (Throwable var6) {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (bufferedWriter != null) {
            bufferedWriter.close();
        }

    }
}
