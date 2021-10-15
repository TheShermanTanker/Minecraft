package net.minecraft.world.effect;

import net.minecraft.util.MathHelper;
import net.minecraft.util.UtilColor;
import net.minecraft.world.entity.EntityLiving;

public final class MobEffectUtil {
    public static String formatDuration(MobEffect effect, float multiplier) {
        if (effect.isNoCounter()) {
            return "**:**";
        } else {
            int i = MathHelper.floor((float)effect.getDuration() * multiplier);
            return UtilColor.formatTickDuration(i);
        }
    }

    public static boolean hasDigSpeed(EntityLiving entity) {
        return entity.hasEffect(MobEffects.DIG_SPEED) || entity.hasEffect(MobEffects.CONDUIT_POWER);
    }

    public static int getDigSpeedAmplification(EntityLiving entity) {
        int i = 0;
        int j = 0;
        if (entity.hasEffect(MobEffects.DIG_SPEED)) {
            i = entity.getEffect(MobEffects.DIG_SPEED).getAmplifier();
        }

        if (entity.hasEffect(MobEffects.CONDUIT_POWER)) {
            j = entity.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
        }

        return Math.max(i, j);
    }

    public static boolean hasWaterBreathing(EntityLiving entity) {
        return entity.hasEffect(MobEffects.WATER_BREATHING) || entity.hasEffect(MobEffects.CONDUIT_POWER);
    }
}
