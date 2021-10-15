package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.util.MathHelper;

@Immutable
public class DifficultyDamageScaler {
    private static final float DIFFICULTY_TIME_GLOBAL_OFFSET = -72000.0F;
    private static final float MAX_DIFFICULTY_TIME_GLOBAL = 1440000.0F;
    private static final float MAX_DIFFICULTY_TIME_LOCAL = 3600000.0F;
    private final EnumDifficulty base;
    private final float effectiveDifficulty;

    public DifficultyDamageScaler(EnumDifficulty difficulty, long timeOfDay, long inhabitedTime, float moonSize) {
        this.base = difficulty;
        this.effectiveDifficulty = this.calculateDifficulty(difficulty, timeOfDay, inhabitedTime, moonSize);
    }

    public EnumDifficulty getDifficulty() {
        return this.base;
    }

    public float getEffectiveDifficulty() {
        return this.effectiveDifficulty;
    }

    public boolean isHard() {
        return this.effectiveDifficulty >= (float)EnumDifficulty.HARD.ordinal();
    }

    public boolean isHarderThan(float difficulty) {
        return this.effectiveDifficulty > difficulty;
    }

    public float getSpecialMultiplier() {
        if (this.effectiveDifficulty < 2.0F) {
            return 0.0F;
        } else {
            return this.effectiveDifficulty > 4.0F ? 1.0F : (this.effectiveDifficulty - 2.0F) / 2.0F;
        }
    }

    private float calculateDifficulty(EnumDifficulty difficulty, long timeOfDay, long inhabitedTime, float moonSize) {
        if (difficulty == EnumDifficulty.PEACEFUL) {
            return 0.0F;
        } else {
            boolean bl = difficulty == EnumDifficulty.HARD;
            float f = 0.75F;
            float g = MathHelper.clamp(((float)timeOfDay + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
            f = f + g;
            float h = 0.0F;
            h = h + MathHelper.clamp((float)inhabitedTime / 3600000.0F, 0.0F, 1.0F) * (bl ? 1.0F : 0.75F);
            h = h + MathHelper.clamp(moonSize * 0.25F, 0.0F, g);
            if (difficulty == EnumDifficulty.EASY) {
                h *= 0.5F;
            }

            f = f + h;
            return (float)difficulty.getId() * f;
        }
    }
}
