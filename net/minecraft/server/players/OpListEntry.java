package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nullable;

public class OpListEntry extends JsonListEntry<GameProfile> {
    private final int level;
    private final boolean bypassesPlayerLimit;

    public OpListEntry(GameProfile profile, int permissionLevel, boolean bypassPlayerLimit) {
        super(profile);
        this.level = permissionLevel;
        this.bypassesPlayerLimit = bypassPlayerLimit;
    }

    public OpListEntry(JsonObject json) {
        super(createGameProfile(json));
        this.level = json.has("level") ? json.get("level").getAsInt() : 0;
        this.bypassesPlayerLimit = json.has("bypassesPlayerLimit") && json.get("bypassesPlayerLimit").getAsBoolean();
    }

    public int getLevel() {
        return this.level;
    }

    public boolean getBypassesPlayerLimit() {
        return this.bypassesPlayerLimit;
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getKey() != null) {
            json.addProperty("uuid", this.getKey().getId() == null ? "" : this.getKey().getId().toString());
            json.addProperty("name", this.getKey().getName());
            json.addProperty("level", this.level);
            json.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
        }
    }

    @Nullable
    private static GameProfile createGameProfile(JsonObject json) {
        if (json.has("uuid") && json.has("name")) {
            String string = json.get("uuid").getAsString();

            UUID uUID;
            try {
                uUID = UUID.fromString(string);
            } catch (Throwable var4) {
                return null;
            }

            return new GameProfile(uUID, json.get("name").getAsString());
        } else {
            return null;
        }
    }
}
