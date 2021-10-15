package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.saveddata.PersistentBase;

public class ForcedChunk extends PersistentBase {
    public static final String FILE_ID = "chunks";
    private static final String TAG_FORCED = "Forced";
    private final LongSet chunks;

    private ForcedChunk(LongSet chunks) {
        this.chunks = chunks;
    }

    public ForcedChunk() {
        this(new LongOpenHashSet());
    }

    public static ForcedChunk load(NBTTagCompound nbt) {
        return new ForcedChunk(new LongOpenHashSet(nbt.getLongArray("Forced")));
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.putLongArray("Forced", this.chunks.toLongArray());
        return nbt;
    }

    public LongSet getChunks() {
        return this.chunks;
    }
}
