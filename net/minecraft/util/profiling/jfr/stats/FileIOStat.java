package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public record FileIOStat(Duration duration, @Nullable String path, long bytes) {
    public FileIOStat(Duration duration, @Nullable String string, long l) {
        this.duration = duration;
        this.path = string;
        this.bytes = l;
    }

    public static FileIOStat.Summary summary(Duration duration, List<FileIOStat> samples) {
        long l = samples.stream().mapToLong((sample) -> {
            return sample.bytes;
        }).sum();
        return new FileIOStat.Summary(l, (double)l / (double)duration.getSeconds(), (long)samples.size(), (double)samples.size() / (double)duration.getSeconds(), samples.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus), samples.stream().filter((sample) -> {
            return sample.path != null;
        }).collect(Collectors.groupingBy((sample) -> {
            return sample.path;
        }, Collectors.summingLong((sample) -> {
            return sample.bytes;
        }))).entrySet().stream().sorted(Entry.<String, Long>comparingByValue().reversed()).map((entry) -> {
            return Pair.of(entry.getKey(), entry.getValue());
        }).limit(10L).toList());
    }

    public Duration duration() {
        return this.duration;
    }

    @Nullable
    public String path() {
        return this.path;
    }

    public long bytes() {
        return this.bytes;
    }

    public static record Summary(long totalBytes, double bytesPerSecond, long counts, double countsPerSecond, Duration timeSpentInIO, List<Pair<String, Long>> topTenContributorsByTotalBytes) {
        public Summary(long l, double d, long m, double e, Duration duration, List<Pair<String, Long>> list) {
            this.totalBytes = l;
            this.bytesPerSecond = d;
            this.counts = m;
            this.countsPerSecond = e;
            this.timeSpentInIO = duration;
            this.topTenContributorsByTotalBytes = list;
        }

        public long totalBytes() {
            return this.totalBytes;
        }

        public double bytesPerSecond() {
            return this.bytesPerSecond;
        }

        public long counts() {
            return this.counts;
        }

        public double countsPerSecond() {
            return this.countsPerSecond;
        }

        public Duration timeSpentInIO() {
            return this.timeSpentInIO;
        }

        public List<Pair<String, Long>> topTenContributorsByTotalBytes() {
            return this.topTenContributorsByTotalBytes;
        }
    }
}
