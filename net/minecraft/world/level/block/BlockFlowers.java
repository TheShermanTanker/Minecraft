package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockFlowers extends BlockPlant {
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    private final MobEffectBase suspiciousStewEffect;
    private final int effectDuration;

    public BlockFlowers(MobEffectBase suspiciousStewEffect, int effectDuration, BlockBase.Info settings) {
        super(settings);
        this.suspiciousStewEffect = suspiciousStewEffect;
        if (suspiciousStewEffect.isInstant()) {
            this.effectDuration = effectDuration;
        } else {
            this.effectDuration = effectDuration * 20;
        }

    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        Vec3D vec3 = state.getOffset(world, pos);
        return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    public MobEffectBase getSuspiciousStewEffect() {
        return this.suspiciousStewEffect;
    }

    public int getEffectDuration() {
        return this.effectDuration;
    }
}
