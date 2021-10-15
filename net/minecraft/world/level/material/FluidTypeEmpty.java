package net.minecraft.world.level.material;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class FluidTypeEmpty extends FluidType {
    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    public boolean canBeReplacedWith(Fluid state, IBlockAccess world, BlockPosition pos, FluidType fluid, EnumDirection direction) {
        return true;
    }

    @Override
    public Vec3D getFlow(IBlockAccess world, BlockPosition pos, Fluid state) {
        return Vec3D.ZERO;
    }

    @Override
    public int getTickDelay(IWorldReader world) {
        return 0;
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 0.0F;
    }

    @Override
    public float getHeight(Fluid state, IBlockAccess world, BlockPosition pos) {
        return 0.0F;
    }

    @Override
    public float getOwnHeight(Fluid state) {
        return 0.0F;
    }

    @Override
    protected IBlockData createLegacyBlock(Fluid state) {
        return Blocks.AIR.getBlockData();
    }

    @Override
    public boolean isSource(Fluid state) {
        return false;
    }

    @Override
    public int getAmount(Fluid state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(Fluid state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }
}
