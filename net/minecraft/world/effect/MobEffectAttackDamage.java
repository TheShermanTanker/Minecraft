package net.minecraft.world.effect;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class MobEffectAttackDamage extends MobEffectBase {
    protected final double multiplier;

    protected MobEffectAttackDamage(MobEffectInfo category, int color, double modifier) {
        super(category, color);
        this.multiplier = modifier;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return this.multiplier * (double)(amplifier + 1);
    }
}
