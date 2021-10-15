package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class WhiteList extends JsonList<GameProfile, WhiteListEntry> {
    public WhiteList(File file) {
        super(file);
    }

    @Override
    protected JsonListEntry<GameProfile> createEntry(JsonObject json) {
        return new WhiteListEntry(json);
    }

    public boolean isWhitelisted(GameProfile profile) {
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
