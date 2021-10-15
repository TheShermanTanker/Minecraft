package net.minecraft.server.packs.repository;

import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.metadata.pack.ResourcePackInfo;

public enum EnumResourcePackVersion {
    TOO_OLD("old"),
    TOO_NEW("new"),
    COMPATIBLE("compatible");

    private final IChatBaseComponent description;
    private final IChatBaseComponent confirmation;

    private EnumResourcePackVersion(String translationSuffix) {
        this.description = (new ChatMessage("pack.incompatible." + translationSuffix)).withStyle(EnumChatFormat.GRAY);
        this.confirmation = new ChatMessage("pack.incompatible.confirm." + translationSuffix);
    }

    public boolean isCompatible() {
        return this == COMPATIBLE;
    }

    public static EnumResourcePackVersion forFormat(int packVersion, EnumResourcePackType type) {
        int i = type.getVersion(SharedConstants.getGameVersion());
        if (packVersion < i) {
            return TOO_OLD;
        } else {
            return packVersion > i ? TOO_NEW : COMPATIBLE;
        }
    }

    public static EnumResourcePackVersion forMetadata(ResourcePackInfo metadata, EnumResourcePackType type) {
        return forFormat(metadata.getPackFormat(), type);
    }

    public IChatBaseComponent getDescription() {
        return this.description;
    }

    public IChatBaseComponent getConfirmation() {
        return this.confirmation;
    }
}
