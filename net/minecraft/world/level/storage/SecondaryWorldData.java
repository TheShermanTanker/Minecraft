package net.minecraft.world.level.storage;

import java.util.UUID;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimerQueue;

public class SecondaryWorldData implements IWorldDataServer {
    private final SaveData worldData;
    private final IWorldDataServer wrapped;

    public SecondaryWorldData(SaveData saveProperties, IWorldDataServer worldProperties) {
        this.worldData = saveProperties;
        this.wrapped = worldProperties;
    }

    @Override
    public int getXSpawn() {
        return this.wrapped.getXSpawn();
    }

    @Override
    public int getYSpawn() {
        return this.wrapped.getYSpawn();
    }

    @Override
    public int getZSpawn() {
        return this.wrapped.getZSpawn();
    }

    @Override
    public float getSpawnAngle() {
        return this.wrapped.getSpawnAngle();
    }

    @Override
    public long getTime() {
        return this.wrapped.getTime();
    }

    @Override
    public long getDayTime() {
        return this.wrapped.getDayTime();
    }

    @Override
    public String getName() {
        return this.worldData.getName();
    }

    @Override
    public int getClearWeatherTime() {
        return this.wrapped.getClearWeatherTime();
    }

    @Override
    public void setClearWeatherTime(int clearWeatherTime) {
    }

    @Override
    public boolean isThundering() {
        return this.wrapped.isThundering();
    }

    @Override
    public int getThunderDuration() {
        return this.wrapped.getThunderDuration();
    }

    @Override
    public boolean hasStorm() {
        return this.wrapped.hasStorm();
    }

    @Override
    public int getWeatherDuration() {
        return this.wrapped.getWeatherDuration();
    }

    @Override
    public EnumGamemode getGameType() {
        return this.worldData.getGameType();
    }

    @Override
    public void setXSpawn(int spawnX) {
    }

    @Override
    public void setYSpawn(int spawnY) {
    }

    @Override
    public void setZSpawn(int spawnZ) {
    }

    @Override
    public void setSpawnAngle(float angle) {
    }

    @Override
    public void setTime(long time) {
    }

    @Override
    public void setDayTime(long timeOfDay) {
    }

    @Override
    public void setSpawn(BlockPosition pos, float angle) {
    }

    @Override
    public void setThundering(boolean thundering) {
    }

    @Override
    public void setThunderDuration(int thunderTime) {
    }

    @Override
    public void setStorm(boolean raining) {
    }

    @Override
    public void setWeatherDuration(int rainTime) {
    }

    @Override
    public void setGameType(EnumGamemode gameMode) {
    }

    @Override
    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    @Override
    public boolean getAllowCommands() {
        return this.worldData.getAllowCommands();
    }

    @Override
    public boolean isInitialized() {
        return this.wrapped.isInitialized();
    }

    @Override
    public void setInitialized(boolean initialized) {
    }

    @Override
    public GameRules getGameRules() {
        return this.worldData.getGameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return this.wrapped.getWorldBorder();
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings properties) {
    }

    @Override
    public EnumDifficulty getDifficulty() {
        return this.worldData.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return this.worldData.isDifficultyLocked();
    }

    @Override
    public CustomFunctionCallbackTimerQueue<MinecraftServer> getScheduledEvents() {
        return this.wrapped.getScheduledEvents();
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return 0;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return 0;
    }

    @Override
    public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
    }

    @Override
    public UUID getWanderingTraderId() {
        return null;
    }

    @Override
    public void setWanderingTraderId(UUID uuid) {
    }

    @Override
    public void fillCrashReportCategory(CrashReportSystemDetails reportSection, IWorldHeightAccess world) {
        reportSection.setDetail("Derived", true);
        this.wrapped.fillCrashReportCategory(reportSection, world);
    }
}
