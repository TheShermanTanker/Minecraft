package net.minecraft.world.level.storage;

import net.minecraft.CrashReportSystemDetails;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldHeightAccess;

public interface WorldData {
    int getXSpawn();

    int getYSpawn();

    int getZSpawn();

    float getSpawnAngle();

    long getTime();

    long getDayTime();

    boolean isThundering();

    boolean hasStorm();

    void setStorm(boolean raining);

    boolean isHardcore();

    GameRules getGameRules();

    EnumDifficulty getDifficulty();

    boolean isDifficultyLocked();

    default void fillCrashReportCategory(CrashReportSystemDetails reportSection, IWorldHeightAccess world) {
        reportSection.setDetail("Level spawn location", () -> {
            return CrashReportSystemDetails.formatLocation(world, this.getXSpawn(), this.getYSpawn(), this.getZSpawn());
        });
        reportSection.setDetail("Level time", () -> {
            return String.format("%d game time, %d day time", this.getTime(), this.getDayTime());
        });
    }
}
