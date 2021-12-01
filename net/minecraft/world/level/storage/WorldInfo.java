package net.minecraft.world.level.storage;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.util.UtilColor;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.WorldSettings;
import org.apache.commons.lang3.StringUtils;

public class WorldInfo implements Comparable<WorldInfo> {
    private final WorldSettings settings;
    private final LevelVersion levelVersion;
    private final String levelId;
    private final boolean requiresManualConversion;
    private final boolean locked;
    private final File icon;
    @Nullable
    private IChatBaseComponent info;

    public WorldInfo(WorldSettings levelInfo, LevelVersion versionInfo, String name, boolean requiresConversion, boolean locked, File file) {
        this.settings = levelInfo;
        this.levelVersion = versionInfo;
        this.levelId = name;
        this.locked = locked;
        this.icon = file;
        this.requiresManualConversion = requiresConversion;
    }

    public String getLevelId() {
        return this.levelId;
    }

    public String getLevelName() {
        return StringUtils.isEmpty(this.settings.getLevelName()) ? this.levelId : this.settings.getLevelName();
    }

    public File getIcon() {
        return this.icon;
    }

    public boolean requiresManualConversion() {
        return this.requiresManualConversion;
    }

    public long getLastPlayed() {
        return this.levelVersion.lastPlayed();
    }

    @Override
    public int compareTo(WorldInfo levelSummary) {
        if (this.levelVersion.lastPlayed() < levelSummary.levelVersion.lastPlayed()) {
            return 1;
        } else {
            return this.levelVersion.lastPlayed() > levelSummary.levelVersion.lastPlayed() ? -1 : this.levelId.compareTo(levelSummary.levelId);
        }
    }

    public WorldSettings getSettings() {
        return this.settings;
    }

    public EnumGamemode getGameMode() {
        return this.settings.getGameType();
    }

    public boolean isHardcore() {
        return this.settings.isHardcore();
    }

    public boolean hasCheats() {
        return this.settings.allowCommands();
    }

    public IChatMutableComponent getWorldVersionName() {
        return (IChatMutableComponent)(UtilColor.isNullOrEmpty(this.levelVersion.minecraftVersionName()) ? new ChatMessage("selectWorld.versionUnknown") : new ChatComponentText(this.levelVersion.minecraftVersionName()));
    }

    public LevelVersion levelVersion() {
        return this.levelVersion;
    }

    public boolean markVersionInList() {
        return this.askToOpenWorld() || !SharedConstants.getCurrentVersion().isStable() && !this.levelVersion.snapshot() || this.backupStatus().shouldBackup();
    }

    public boolean askToOpenWorld() {
        return this.levelVersion.minecraftVersion().getVersion() > SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    public WorldInfo.BackupStatus backupStatus() {
        WorldVersion worldVersion = SharedConstants.getCurrentVersion();
        int i = worldVersion.getDataVersion().getVersion();
        int j = this.levelVersion.minecraftVersion().getVersion();
        if (!worldVersion.isStable() && j < i) {
            return WorldInfo.BackupStatus.UPGRADE_TO_SNAPSHOT;
        } else {
            return j > i ? WorldInfo.BackupStatus.DOWNGRADE : WorldInfo.BackupStatus.NONE;
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isDisabled() {
        if (!this.isLocked() && !this.requiresManualConversion()) {
            return !this.isCompatible();
        } else {
            return true;
        }
    }

    public boolean isCompatible() {
        return SharedConstants.getCurrentVersion().getDataVersion().isCompatible(this.levelVersion.minecraftVersion());
    }

    public IChatBaseComponent getInfo() {
        if (this.info == null) {
            this.info = this.createInfo();
        }

        return this.info;
    }

    private IChatBaseComponent createInfo() {
        if (this.isLocked()) {
            return (new ChatMessage("selectWorld.locked")).withStyle(EnumChatFormat.RED);
        } else if (this.requiresManualConversion()) {
            return (new ChatMessage("selectWorld.conversion")).withStyle(EnumChatFormat.RED);
        } else if (!this.isCompatible()) {
            return (new ChatMessage("selectWorld.incompatible_series")).withStyle(EnumChatFormat.RED);
        } else {
            IChatMutableComponent mutableComponent = (IChatMutableComponent)(this.isHardcore() ? (new ChatComponentText("")).addSibling((new ChatMessage("gameMode.hardcore")).withStyle(EnumChatFormat.DARK_RED)) : new ChatMessage("gameMode." + this.getGameMode().getName()));
            if (this.hasCheats()) {
                mutableComponent.append(", ").addSibling(new ChatMessage("selectWorld.cheats"));
            }

            IChatMutableComponent mutableComponent2 = this.getWorldVersionName();
            IChatMutableComponent mutableComponent3 = (new ChatComponentText(", ")).addSibling(new ChatMessage("selectWorld.version")).append(" ");
            if (this.markVersionInList()) {
                mutableComponent3.addSibling(mutableComponent2.withStyle(this.askToOpenWorld() ? EnumChatFormat.RED : EnumChatFormat.ITALIC));
            } else {
                mutableComponent3.addSibling(mutableComponent2);
            }

            mutableComponent.addSibling(mutableComponent3);
            return mutableComponent;
        }
    }

    public static enum BackupStatus {
        NONE(false, false, ""),
        DOWNGRADE(true, true, "downgrade"),
        UPGRADE_TO_SNAPSHOT(true, false, "snapshot");

        private final boolean shouldBackup;
        private final boolean severe;
        private final String translationKey;

        private BackupStatus(boolean backup, boolean boldRedFormatting, String translationKeySuffix) {
            this.shouldBackup = backup;
            this.severe = boldRedFormatting;
            this.translationKey = translationKeySuffix;
        }

        public boolean shouldBackup() {
            return this.shouldBackup;
        }

        public boolean isSevere() {
            return this.severe;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }
}
