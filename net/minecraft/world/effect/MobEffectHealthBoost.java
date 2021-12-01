package net.minecraft.world.effect;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;

public class MobEffectHealthBoost extends MobEffectBase {
    public MobEffectHealthBoost(MobEffectInfo category, int color) {
        super(category, color);
    }

    @Override
    public void removeAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }

    }
}
