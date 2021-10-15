package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

public interface PairedQueue<T, F> {
    @Nullable
    F pop();

    boolean push(T message);

    boolean isEmpty();

    int size();

    public static final class FixedPriorityQueue implements PairedQueue<PairedQueue.IntRunnable, Runnable> {
        private final List<Queue<Runnable>> queueList;

        public FixedPriorityQueue(int priorityCount) {
            this.queueList = IntStream.range(0, priorityCount).mapToObj((i) -> {
                return Queues.newConcurrentLinkedQueue();
            }).collect(Collectors.toList());
        }

        @Nullable
        @Override
        public Runnable pop() {
            for(Queue<Runnable> queue : this.queueList) {
                Runnable runnable = queue.poll();
                if (runnable != null) {
                    return runnable;
                }
            }

            return null;
        }

        @Override
        public boolean push(PairedQueue.IntRunnable message) {
            int i = message.getPriority();
            this.queueList.get(i).add(message);
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.queueList.stream().allMatch(Collection::isEmpty);
        }

        @Override
        public int size() {
            int i = 0;

            for(Queue<Runnable> queue : this.queueList) {
                i += queue.size();
            }

            return i;
        }
    }

    public static final class IntRunnable implements Runnable {
        private final int priority;
        private final Runnable task;

        public IntRunnable(int priority, Runnable runnable) {
            this.priority = priority;
            this.task = runnable;
        }

        @Override
        public void run() {
            this.task.run();
        }

        public int getPriority() {
            return this.priority;
        }
    }

    public static final class QueueStrictQueue<T> implements PairedQueue<T, T> {
        private final Queue<T> queue;

        public QueueStrictQueue(Queue<T> queue) {
            this.queue = queue;
        }

        @Nullable
        @Override
        public T pop() {
            return this.queue.poll();
        }

        @Override
        public boolean push(T message) {
            return this.queue.add(message);
        }

        @Override
        public boolean isEmpty() {
            return this.queue.isEmpty();
        }

        @Override
        public int size() {
            return this.queue.size();
        }
    }
}
