package net.minecraft.server.players;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;

public abstract class JsonListEntry<T> {
    @Nullable
    private final T user;

    public JsonListEntry(@Nullable T key) {
        this.user = key;
    }

    @Nullable
    public T getKey() {
        return this.user;
    }

    boolean hasExpired() {
        return false;
    }

    protected abstract void serialize(JsonObject json);
}
