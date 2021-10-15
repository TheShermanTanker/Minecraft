package net.minecraft.world.level.storage;

import com.mojang.serialization.Lifecycle;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.levelgen.GeneratorSettings;

public interface SaveData {
    int ANVIL_VERSION_ID = 19133;
    int MCREGION_VERSION_ID = 19132;

    DataPackConfiguration getDataPackConfig();

    void setDataPackConfig(DataPackConfiguration dataPackSettings);

    boolean wasModded();

    Set<String> getKnownServerBrands();

    void setModdedInfo(String brand, boolean modded);

    default void fillCrashReportCategory(CrashReportSystemDetails crashReportCategory) {
        crashReportCategory.setDetail("Known server brands", () -> {
            return String.join(", ", this.getKnownServerBrands());
        });
        crashReportCategory.setDetail("Level was modded", () -> {
            return Boolean.toString(this.wasModded());
        });
        crashReportCategory.setDetail("Level storage version", () -> {
            int i = this.getVersion();
            return String.format("0x%05X - %s", i, this.getStorageVersionName(i));
        });
    }

    default String getStorageVersionName(int id) {
        switch(id) {
        case 19132:
            return "McRegion";
        case 19133:
            return "Anvil";
        default:
            return "Unknown?";
        }
    }

    @Nullable
    NBTTagCompound getCustomBossEvents();

    void setCustomBossEvents(@Nullable NBTTagCompound nbt);

    IWorldDataServer overworldData();

    WorldSettings getLevelSettings();

    NBTTagCompound createTag(IRegistryCustom registryManager, @Nullable NBTTagCompound playerNbt);

    boolean isHardcore();

    int getVersion();

    String getName();

    EnumGamemode getGameType();

    void setGameType(EnumGamemode gameMode);

    boolean getAllowCommands();

    EnumDifficulty getDifficulty();

    void setDifficulty(EnumDifficulty difficulty);

    boolean isDifficultyLocked();

    void setDifficultyLocked(boolean locked);

    GameRules getGameRules();

    NBTTagCompound getLoadedPlayerTag();

    NBTTagCompound endDragonFightData();

    void setEndDragonFightData(NBTTagCompound nbt);

    GeneratorSettings getGeneratorSettings();

    Lifecycle worldGenSettingsLifecycle();
}
