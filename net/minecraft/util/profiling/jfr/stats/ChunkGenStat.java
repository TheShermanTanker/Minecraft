package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.server.level.BlockPosition2D;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ChunkStatus;

public record ChunkGenStat(Duration duration, ChunkCoordIntPair chunkPos, BlockPosition2D worldPos, ChunkStatus status, String level) implements TimedStat {
    public ChunkGenStat(Duration duration, ChunkCoordIntPair chunkPos, BlockPosition2D columnPos, ChunkStatus chunkStatus, String string) {
        this.duration = duration;
        this.chunkPos = chunkPos;
        this.worldPos = columnPos;
        this.status = chunkStatus;
        this.level = string;
    }

    public static ChunkGenStat from(RecordedEvent event) {
        return new ChunkGenStat(event.getDuration(), new ChunkCoordIntPair(event.getInt("chunkPosX"), event.getInt("chunkPosX")), new BlockPosition2D(event.getInt("worldPosX"), event.getInt("worldPosZ")), ChunkStatus.byName(event.getString("status")), event.getString("level"));
    }

    @Override
    public Duration duration() {
        return this.duration;
    }

    public ChunkCoordIntPair chunkPos() {
        return this.chunkPos;
    }

    public BlockPosition2D worldPos() {
        return this.worldPos;
    }

    public ChunkStatus status() {
        return this.status;
    }

    public String level() {
        return this.level;
    }
}
