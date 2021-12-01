package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.level.ChunkCoordIntPair;

record SavedTick<T>(T type, BlockPosition pos, int delay, TickPriority priority) {
    private static final String TAG_ID = "i";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_DELAY = "t";
    private static final String TAG_PRIORITY = "p";
    public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
        @Override
        public int hashCode(SavedTick<?> savedTick) {
            return 31 * savedTick.pos().hashCode() + savedTick.type().hashCode();
        }

        @Override
        public boolean equals(@Nullable SavedTick<?> savedTick, @Nullable SavedTick<?> savedTick2) {
            if (savedTick == savedTick2) {
                return true;
            } else if (savedTick != null && savedTick2 != null) {
                return savedTick.type() == savedTick2.type() && savedTick.pos().equals(savedTick2.pos());
            } else {
                return false;
            }
        }
    };

    SavedTick(T object, BlockPosition blockPos, int i, TickPriority tickPriority) {
        this.type = object;
        this.pos = blockPos;
        this.delay = i;
        this.priority = tickPriority;
    }

    public static <T> void loadTickList(NBTTagList tickList, Function<String, Optional<T>> nameToTypeFunction, ChunkCoordIntPair pos, Consumer<SavedTick<T>> tickConsumer) {
        long l = pos.pair();

        for(int i = 0; i < tickList.size(); ++i) {
            NBTTagCompound compoundTag = tickList.getCompound(i);
            nameToTypeFunction.apply(compoundTag.getString("i")).ifPresent((type) -> {
                BlockPosition blockPos = new BlockPosition(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
                if (ChunkCoordIntPair.asLong(blockPos) == l) {
                    tickConsumer.accept(new SavedTick<>(type, blockPos, compoundTag.getInt("t"), TickPriority.byValue(compoundTag.getInt("p"))));
                }

            });
        }

    }

    private static NBTTagCompound saveTick(String type, BlockPosition pos, int delay, TickPriority priority) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("i", type);
        compoundTag.setInt("x", pos.getX());
        compoundTag.setInt("y", pos.getY());
        compoundTag.setInt("z", pos.getZ());
        compoundTag.setInt("t", delay);
        compoundTag.setInt("p", priority.getValue());
        return compoundTag;
    }

    public static <T> NBTTagCompound saveTick(ScheduledTick<T> orderedTick, Function<T, String> typeToNameFunction, long delay) {
        return saveTick(typeToNameFunction.apply(orderedTick.type()), orderedTick.pos(), (int)(orderedTick.triggerTick() - delay), orderedTick.priority());
    }

    public NBTTagCompound save(Function<T, String> typeToNameFunction) {
        return saveTick(typeToNameFunction.apply(this.type), this.pos, this.delay, this.priority);
    }

    public ScheduledTick<T> unpack(long time, long subTickOrder) {
        return new ScheduledTick<>(this.type, this.pos, time + (long)this.delay, this.priority, subTickOrder);
    }

    public static <T> SavedTick<T> probe(T type, BlockPosition pos) {
        return new SavedTick<>(type, pos, 0, TickPriority.NORMAL);
    }

    public T type() {
        return this.type;
    }

    public BlockPosition pos() {
        return this.pos;
    }

    public int delay() {
        return this.delay;
    }

    public TickPriority priority() {
        return this.priority;
    }
}
