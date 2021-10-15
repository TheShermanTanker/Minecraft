package net.minecraft.world.effect;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;

public class MobEffectHealthBoost extends MobEffectList {
    public MobEffectHealthBoost(MobEffectInfo type, int color) {
        super(type, color);
    }

    @Override
    public void removeAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }

    }
}
