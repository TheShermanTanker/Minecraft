package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.EntityLiving;

public interface IRangedEntity {
    void performRangedAttack(EntityLiving target, float pullProgress);
}
