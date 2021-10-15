package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class WhiteListEntry extends JsonListEntry<GameProfile> {
    public WhiteListEntry(GameProfile profile) {
        super(profile);
    }

    public WhiteListEntry(JsonObject json) {
        super(createGameProfile(json));
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getKey() != null) {
            json.addProperty("uuid", this.getKey().getId() == null ? "" : this.getKey().getId().toString());
            json.addProperty("name", this.getKey().getName());
        }
    }

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
