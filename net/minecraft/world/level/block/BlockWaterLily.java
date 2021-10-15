package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockWaterLily extends BlockPlant {
    protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);

    protected BlockWaterLily(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        super.entityInside(state, world, pos, entity);
        if (world instanceof WorldServer && entity instanceof EntityBoat) {
            world.destroyBlock(new BlockPosition(pos), true, entity);
        }

    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return AABB;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        Fluid fluidState = world.getFluid(pos);
        Fluid fluidState2 = world.getFluid(pos.above());
        return (fluidState.getType() == FluidTypes.WATER || floor.getMaterial() == Material.ICE) && fluidState2.getType() == FluidTypes.EMPTY;
    }
}
