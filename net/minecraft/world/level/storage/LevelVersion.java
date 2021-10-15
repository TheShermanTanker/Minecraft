package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SharedConstants;

public class LevelVersion {
    private final int levelDataVersion;
    private final long lastPlayed;
    private final String minecraftVersionName;
    private final int minecraftVersion;
    private final boolean snapshot;

    public LevelVersion(int levelFormatVersion, long lastPlayed, String versionName, int versionId, boolean stable) {
        this.levelDataVersion = levelFormatVersion;
        this.lastPlayed = lastPlayed;
        this.minecraftVersionName = versionName;
        this.minecraftVersion = versionId;
        this.snapshot = stable;
    }

    public static LevelVersion parse(Dynamic<?> dynamic) {
        int i = dynamic.get("version").asInt(0);
        long l = dynamic.get("LastPlayed").asLong(0L);
        OptionalDynamic<?> optionalDynamic = dynamic.get("Version");
        return optionalDynamic.result().isPresent() ? new LevelVersion(i, l, optionalDynamic.get("Name").asString(SharedConstants.getGameVersion().getName()), optionalDynamic.get("Id").asInt(SharedConstants.getGameVersion().getWorldVersion()), optionalDynamic.get("Snapshot").asBoolean(!SharedConstants.getGameVersion().isStable())) : new LevelVersion(i, l, "", 0, false);
    }

    public int levelDataVersion() {
        return this.levelDataVersion;
    }

    public long lastPlayed() {
        return this.lastPlayed;
    }

    public String minecraftVersionName() {
        return this.minecraftVersionName;
    }

    public int minecraftVersion() {
        return this.minecraftVersion;
    }

    public boolean snapshot() {
        return this.snapshot;
    }
}
