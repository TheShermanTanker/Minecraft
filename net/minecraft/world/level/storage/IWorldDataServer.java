package net.minecraft.world.level.storage;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimerQueue;

public interface IWorldDataServer extends WorldDataMutable {
    String getName();

    void setThundering(boolean thundering);

    int getWeatherDuration();

    void setWeatherDuration(int rainTime);

    void setThunderDuration(int thunderTime);

    int getThunderDuration();

    @Override
    default void fillCrashReportCategory(CrashReportSystemDetails reportSection, IWorldHeightAccess world) {
        WorldDataMutable.super.fillCrashReportCategory(reportSection, world);
        reportSection.setDetail("Level name", this::getName);
        reportSection.setDetail("Level game mode", () -> {
            return String.format("Game mode: %s (ID %d). Hardcore: %b. Cheats: %b", this.getGameType().getName(), this.getGameType().getId(), this.isHardcore(), this.getAllowCommands());
        });
        reportSection.setDetail("Level weather", () -> {
            return String.format("Rain time: %d (now: %b), thunder time: %d (now: %b)", this.getWeatherDuration(), this.hasStorm(), this.getThunderDuration(), this.isThundering());
        });
    }

    int getClearWeatherTime();

    void setClearWeatherTime(int clearWeatherTime);

    int getWanderingTraderSpawnDelay();

    void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay);

    int getWanderingTraderSpawnChance();

    void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance);

    @Nullable
    UUID getWanderingTraderId();

    void setWanderingTraderId(UUID uuid);

    EnumGamemode getGameType();

    void setWorldBorder(WorldBorder.Settings properties);

    WorldBorder.Settings getWorldBorder();

    boolean isInitialized();

    void setInitialized(boolean initialized);

    boolean getAllowCommands();

    void setGameType(EnumGamemode gameMode);

    CustomFunctionCallbackTimerQueue<MinecraftServer> getScheduledEvents();

    void setTime(long time);

    void setDayTime(long timeOfDay);
}
