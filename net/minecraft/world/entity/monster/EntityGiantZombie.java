package net.minecraft.world.entity.monster;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;

public class EntityGiantZombie extends EntityMonster {
    public EntityGiantZombie(EntityTypes<? extends EntityGiantZombie> type, World world) {
        super(type, world);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 10.440001F;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 100.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.5D).add(GenericAttributes.ATTACK_DAMAGE, 50.0D);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return world.getBrightness(pos) - 0.5F;
    }
}
