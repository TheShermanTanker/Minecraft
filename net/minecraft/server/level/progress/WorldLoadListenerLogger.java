package net.minecraft.server.level.progress;

import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldLoadListenerLogger implements WorldLoadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int maxCount;
    private int count;
    private long startTime;
    private long nextTickTime = Long.MAX_VALUE;

    public WorldLoadListenerLogger(int radius) {
        int i = radius * 2 + 1;
        this.maxCount = i * i;
    }

    @Override
    public void updateSpawnPos(ChunkCoordIntPair spawnPos) {
        this.nextTickTime = SystemUtils.getMonotonicMillis();
        this.startTime = this.nextTickTime;
    }

    @Override
    public void onStatusChange(ChunkCoordIntPair pos, @Nullable ChunkStatus status) {
        if (status == ChunkStatus.FULL) {
            ++this.count;
        }

        int i = this.getProgress();
        if (SystemUtils.getMonotonicMillis() > this.nextTickTime) {
            this.nextTickTime += 500L;
            LOGGER.info((new ChatMessage("menu.preparingSpawn", MathHelper.clamp(i, 0, 100))).getString());
        }

    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        LOGGER.info("Time elapsed: {} ms", (long)(SystemUtils.getMonotonicMillis() - this.startTime));
        this.nextTickTime = Long.MAX_VALUE;
    }

    public int getProgress() {
        return MathHelper.floor((float)this.count * 100.0F / (float)this.maxCount);
    }
}
