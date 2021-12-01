package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDragonEgg extends BlockFalling {
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public BlockDragonEgg(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        this.teleport(state, world, pos);
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void attack(IBlockData state, World world, BlockPosition pos, EntityHuman player) {
        this.teleport(state, world, pos);
    }

    private void teleport(IBlockData state, World world, BlockPosition pos) {
        WorldBorder worldBorder = world.getWorldBorder();

        for(int i = 0; i < 1000; ++i) {
            BlockPosition blockPos = pos.offset(world.random.nextInt(16) - world.random.nextInt(16), world.random.nextInt(8) - world.random.nextInt(8), world.random.nextInt(16) - world.random.nextInt(16));
            if (world.getType(blockPos).isAir() && worldBorder.isWithinBounds(blockPos)) {
                if (world.isClientSide) {
                    for(int j = 0; j < 128; ++j) {
                        double d = world.random.nextDouble();
                        float f = (world.random.nextFloat() - 0.5F) * 0.2F;
                        float g = (world.random.nextFloat() - 0.5F) * 0.2F;
                        float h = (world.random.nextFloat() - 0.5F) * 0.2F;
                        double e = MathHelper.lerp(d, (double)blockPos.getX(), (double)pos.getX()) + (world.random.nextDouble() - 0.5D) + 0.5D;
                        double k = MathHelper.lerp(d, (double)blockPos.getY(), (double)pos.getY()) + world.random.nextDouble() - 0.5D;
                        double l = MathHelper.lerp(d, (double)blockPos.getZ(), (double)pos.getZ()) + (world.random.nextDouble() - 0.5D) + 0.5D;
                        world.addParticle(Particles.PORTAL, e, k, l, (double)f, (double)g, (double)h);
                    }
                } else {
                    world.setTypeAndData(blockPos, state, 2);
                    world.removeBlock(pos, false);
                }

                return;
            }
        }

    }

    @Override
    protected int getDelayAfterPlace() {
        return 5;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
