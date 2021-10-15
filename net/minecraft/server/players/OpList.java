package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class OpList extends JsonList<GameProfile, OpListEntry> {
    public OpList(File file) {
        super(file);
    }

    @Override
    protected JsonListEntry<GameProfile> createEntry(JsonObject json) {
        return new OpListEntry(json);
    }

    @Override
    public String[] getEntries() {
        return this.getEntries().stream().map(JsonListEntry::getKey).filter(Objects::nonNull).map(GameProfile::getName).toArray((i) -> {
            return new String[i];
        });
    }

    public boolean canBypassPlayerLimit(GameProfile profile) {
        OpListEntry serverOpListEntry = this.get(profile);
        return serverOpListEntry != null ? serverOpListEntry.getBypassesPlayerLimit() : false;
    }

    @Override
    protected String getKeyForUser(GameProfile gameProfile) {
        return gameProfile.getId().toString();
    }
}
