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
        return entity.hasEffect(MobEffectList.DIG_SPEED) || entity.hasEffect(MobEffectList.CONDUIT_POWER);
    }

    public static int getDigSpeedAmplification(EntityLiving entity) {
        int i = 0;
        int j = 0;
        if (entity.hasEffect(MobEffectList.DIG_SPEED)) {
            i = entity.getEffect(MobEffectList.DIG_SPEED).getAmplifier();
        }

        if (entity.hasEffect(MobEffectList.CONDUIT_POWER)) {
            j = entity.getEffect(MobEffectList.CONDUIT_POWER).getAmplifier();
        }

        return Math.max(i, j);
    }

    public static boolean hasWaterBreathing(EntityLiving entity) {
        return entity.hasEffect(MobEffectList.WATER_BREATHING) || entity.hasEffect(MobEffectList.CONDUIT_POWER);
    }
}
