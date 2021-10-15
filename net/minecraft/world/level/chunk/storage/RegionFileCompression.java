package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nullable;

public class RegionFileCompression {
    private static final Int2ObjectMap<RegionFileCompression> VERSIONS = new Int2ObjectOpenHashMap<>();
    public static final RegionFileCompression VERSION_GZIP = register(new RegionFileCompression(1, GZIPInputStream::new, GZIPOutputStream::new));
    public static final RegionFileCompression VERSION_DEFLATE = register(new RegionFileCompression(2, InflaterInputStream::new, DeflaterOutputStream::new));
    public static final RegionFileCompression VERSION_NONE = register(new RegionFileCompression(3, (inputStream) -> {
        return inputStream;
    }, (outputStream) -> {
        return outputStream;
    }));
    private final int id;
    private final RegionFileCompression.StreamWrapper<InputStream> inputWrapper;
    private final RegionFileCompression.StreamWrapper<OutputStream> outputWrapper;

    private RegionFileCompression(int id, RegionFileCompression.StreamWrapper<InputStream> inputStreamWrapper, RegionFileCompression.StreamWrapper<OutputStream> outputStreamWrapper) {
        this.id = id;
        this.inputWrapper = inputStreamWrapper;
        this.outputWrapper = outputStreamWrapper;
    }

    private static RegionFileCompression register(RegionFileCompression version) {
        VERSIONS.put(version.id, version);
        return version;
    }

    @Nullable
    public static RegionFileCompression fromId(int id) {
        return VERSIONS.get(id);
    }

    public static boolean isValidVersion(int id) {
        return VERSIONS.containsKey(id);
    }

    public int getId() {
        return this.id;
    }

    public OutputStream wrap(OutputStream outputStream) throws IOException {
        return this.outputWrapper.wrap(outputStream);
    }

    public InputStream wrap(InputStream inputStream) throws IOException {
        return this.inputWrapper.wrap(inputStream);
    }

    @FunctionalInterface
    interface StreamWrapper<O> {
        O wrap(O object) throws IOException;
    }
}
