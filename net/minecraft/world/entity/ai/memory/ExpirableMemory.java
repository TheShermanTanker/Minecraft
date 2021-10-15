package net.minecraft.world.entity.ai.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.VisibleForDebug;

public class ExpirableMemory<T> {
    private final T value;
    private long timeToLive;

    public ExpirableMemory(T value, long expiry) {
        this.value = value;
        this.timeToLive = expiry;
    }

    public void tick() {
        if (this.canExpire()) {
            --this.timeToLive;
        }

    }

    public static <T> ExpirableMemory<T> of(T value) {
        return new ExpirableMemory<>(value, Long.MAX_VALUE);
    }

    public static <T> ExpirableMemory<T> of(T value, long expiry) {
        return new ExpirableMemory<>(value, expiry);
    }

    public long getTimeToLive() {
        return this.timeToLive;
    }

    public T getValue() {
        return this.value;
    }

    public boolean hasExpired() {
        return this.timeToLive <= 0L;
    }

    @Override
    public String toString() {
        return this.value + (this.canExpire() ? " (ttl: " + this.timeToLive + ")" : "");
    }

    @VisibleForDebug
    public boolean canExpire() {
        return this.timeToLive != Long.MAX_VALUE;
    }

    public static <T> Codec<ExpirableMemory<T>> codec(Codec<T> codec) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(codec.fieldOf("value").forGetter((expirableValue) -> {
                return expirableValue.value;
            }), Codec.LONG.optionalFieldOf("ttl").forGetter((expirableValue) -> {
                return expirableValue.canExpire() ? Optional.of(expirableValue.timeToLive) : Optional.empty();
            })).apply(instance, (object, optional) -> {
                return new ExpirableMemory<>(object, optional.orElse(Long.MAX_VALUE));
            });
        });
    }
}
