package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEnderPortal;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockEnderPortal extends BlockTileEntity {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    protected BlockEnderPortal(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityEnderPortal(pos, state);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (world instanceof WorldServer && !entity.isPassenger() && !entity.isVehicle() && entity.canPortal() && VoxelShapes.joinIsNotEmpty(VoxelShapes.create(entity.getBoundingBox().move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))), state.getShape(world, pos), OperatorBoolean.AND)) {
            ResourceKey<World> resourceKey = world.getDimensionKey() == World.END ? World.OVERWORLD : World.END;
            WorldServer serverLevel = ((WorldServer)world).getMinecraftServer().getWorldServer(resourceKey);
            if (serverLevel == null) {
                return;
            }

            entity.changeDimension(serverLevel);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        double d = (double)pos.getX() + random.nextDouble();
        double e = (double)pos.getY() + 0.8D;
        double f = (double)pos.getZ() + random.nextDouble();
        world.addParticle(Particles.SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeReplaced(IBlockData state, FluidType fluid) {
        return false;
    }
}
