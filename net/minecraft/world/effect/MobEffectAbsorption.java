package net.minecraft.world.effect;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;

public class MobEffectAbsorption extends MobEffectList {
    protected MobEffectAbsorption(MobEffectInfo type, int color) {
        super(type, color);
    }

    @Override
    public void removeAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        entity.setAbsorptionHearts(entity.getAbsorptionHearts() - (float)(4 * (amplifier + 1)));
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void addAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        entity.setAbsorptionHearts(entity.getAbsorptionHearts() + (float)(4 * (amplifier + 1)));
        super.addAttributeModifiers(entity, attributes, amplifier);
    }
}
