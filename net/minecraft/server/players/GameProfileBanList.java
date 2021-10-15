package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class GameProfileBanList extends JsonList<GameProfile, GameProfileBanEntry> {
    public GameProfileBanList(File file) {
        super(file);
    }

    @Override
    protected JsonListEntry<GameProfile> createEntry(JsonObject json) {
        return new GameProfileBanEntry(json);
    }

    public boolean isBanned(GameProfile profile) {
        return this.contains(profile);
    }

    @Override
    public String[] getEntries() {
        return this.getEntries().stream().map(JsonListEntry::getKey).filter(Objects::nonNull).map(GameProfile::getName).toArray((i) -> {
            return new String[i];
        });
    }

    @Override
    protected String getKeyForUser(GameProfile gameProfile) {
        return gameProfile.getId().toString();
    }
}
