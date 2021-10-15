package net.minecraft.world.level;

import net.minecraft.world.level.dimension.DimensionManager;

public interface IWorldTime extends IWorldReader {
    long dayTime();

    default float getMoonBrightness() {
        return DimensionManager.MOON_BRIGHTNESS_PER_PHASE[this.getDimensionManager().moonPhase(this.dayTime())];
    }

    default float getTimeOfDay(float tickDelta) {
        return this.getDimensionManager().timeOfDay(this.dayTime());
    }

    default int getMoonPhase() {
        return this.getDimensionManager().moonPhase(this.dayTime());
    }
}
