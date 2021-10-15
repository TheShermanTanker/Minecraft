package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ProcessorChunkProgressListener implements WorldLoadListener {
    private final WorldLoadListener delegate;
    private final ThreadedMailbox<Runnable> mailbox;

    private ProcessorChunkProgressListener(WorldLoadListener progressListener, Executor executor) {
        this.delegate = progressListener;
        this.mailbox = ThreadedMailbox.create(executor, "progressListener");
    }

    public static ProcessorChunkProgressListener createStarted(WorldLoadListener progressListener, Executor executor) {
        ProcessorChunkProgressListener processorChunkProgressListener = new ProcessorChunkProgressListener(progressListener, executor);
        processorChunkProgressListener.start();
        return processorChunkProgressListener;
    }

    @Override
    public void updateSpawnPos(ChunkCoordIntPair spawnPos) {
        this.mailbox.tell(() -> {
            this.delegate.updateSpawnPos(spawnPos);
        });
    }

    @Override
    public void onStatusChange(ChunkCoordIntPair pos, @Nullable ChunkStatus status) {
        this.mailbox.tell(() -> {
            this.delegate.onStatusChange(pos, status);
        });
    }

    @Override
    public void start() {
        this.mailbox.tell(this.delegate::start);
    }

    @Override
    public void stop() {
        this.mailbox.tell(this.delegate::stop);
    }
}
