package net.minecraft.server.packs;

import com.mojang.bridge.game.GameVersion;
import com.mojang.bridge.game.PackType;

public enum EnumResourcePackType {
    CLIENT_RESOURCES("assets", PackType.RESOURCE),
    SERVER_DATA("data", PackType.DATA);

    private final String directory;
    private final PackType bridgeType;

    private EnumResourcePackType(String name, PackType packType) {
        this.directory = name;
        this.bridgeType = packType;
    }

    public String getDirectory() {
        return this.directory;
    }

    public int getVersion(GameVersion gameVersion) {
        return gameVersion.getPackVersion(this.bridgeType);
    }
}
