package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionSuppressor;
import net.minecraft.world.level.ChunkCoordIntPair;

public class RegionFileCache implements AutoCloseable {
    public static final String ANVIL_EXTENSION = ".mca";
    private static final int MAX_CACHE_SIZE = 256;
    public final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap<>();
    private final Path folder;
    private final boolean sync;

    RegionFileCache(Path directory, boolean dsync) {
        this.folder = directory;
        this.sync = dsync;
    }

    private RegionFile getFile(ChunkCoordIntPair pos) throws IOException {
        long l = ChunkCoordIntPair.pair(pos.getRegionX(), pos.getRegionZ());
        RegionFile regionFile = this.regionCache.getAndMoveToFirst(l);
        if (regionFile != null) {
            return regionFile;
        } else {
            if (this.regionCache.size() >= 256) {
                this.regionCache.removeLast().close();
            }

            Files.createDirectories(this.folder);
            Path path = this.folder.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
            RegionFile regionFile2 = new RegionFile(path, this.folder, this.sync);
            this.regionCache.putAndMoveToFirst(l, regionFile2);
            return regionFile2;
        }
    }

    @Nullable
    public NBTTagCompound read(ChunkCoordIntPair pos) throws IOException {
        RegionFile regionFile = this.getFile(pos);
        DataInputStream dataInputStream = regionFile.getChunkDataInputStream(pos);

        NBTTagCompound var8;
        label43: {
            try {
                if (dataInputStream == null) {
                    var8 = null;
                    break label43;
                }

                var8 = NBTCompressedStreamTools.read(dataInputStream);
            } catch (Throwable var7) {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (dataInputStream != null) {
                dataInputStream.close();
            }

            return var8;
        }

        if (dataInputStream != null) {
            dataInputStream.close();
        }

        return var8;
    }

    public void scanChunk(ChunkCoordIntPair chunkPos, StreamTagVisitor streamTagVisitor) throws IOException {
        RegionFile regionFile = this.getFile(chunkPos);
        DataInputStream dataInputStream = regionFile.getChunkDataInputStream(chunkPos);

        try {
            if (dataInputStream != null) {
                NBTCompressedStreamTools.parse(dataInputStream, streamTagVisitor);
            }
        } catch (Throwable var8) {
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (dataInputStream != null) {
            dataInputStream.close();
        }

    }

    protected void write(ChunkCoordIntPair pos, @Nullable NBTTagCompound nbt) throws IOException {
        RegionFile regionFile = this.getFile(pos);
        if (nbt == null) {
            regionFile.clear(pos);
        } else {
            DataOutputStream dataOutputStream = regionFile.getChunkDataOutputStream(pos);

            try {
                NBTCompressedStreamTools.write(nbt, dataOutputStream);
            } catch (Throwable var8) {
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                    }
                }

                throw var8;
            }

            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        }

    }

    @Override
    public void close() throws IOException {
        ExceptionSuppressor<IOException> exceptionCollector = new ExceptionSuppressor<>();

        for(RegionFile regionFile : this.regionCache.values()) {
            try {
                regionFile.close();
            } catch (IOException var5) {
                exceptionCollector.add(var5);
            }
        }

        exceptionCollector.throwIfPresent();
    }

    public void flush() throws IOException {
        for(RegionFile regionFile : this.regionCache.values()) {
            regionFile.flush();
        }

    }
}
