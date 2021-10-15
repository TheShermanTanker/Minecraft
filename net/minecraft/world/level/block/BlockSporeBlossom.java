package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSporeBlossom extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0D, 13.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final int ADD_PARTICLE_ATTEMPTS = 14;
    private static final int PARTICLE_XZ_RADIUS = 10;
    private static final int PARTICLE_Y_MAX = 10;

    public BlockSporeBlossom(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return Block.canSupportCenter(world, pos.above(), EnumDirection.DOWN) && !world.isWaterAt(pos);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.UP && !this.canPlace(state, world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        double d = (double)i + random.nextDouble();
        double e = (double)j + 0.7D;
        double f = (double)k + random.nextDouble();
        world.addParticle(Particles.FALLING_SPORE_BLOSSOM, d, e, f, 0.0D, 0.0D, 0.0D);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int l = 0; l < 14; ++l) {
            mutableBlockPos.set(i + MathHelper.nextInt(random, -10, 10), j - random.nextInt(10), k + MathHelper.nextInt(random, -10, 10));
            IBlockData blockState = world.getType(mutableBlockPos);
            if (!blockState.isCollisionShapeFullBlock(world, mutableBlockPos)) {
                world.addParticle(Particles.SPORE_BLOSSOM_AIR, (double)mutableBlockPos.getX() + random.nextDouble(), (double)mutableBlockPos.getY() + random.nextDouble(), (double)mutableBlockPos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }
}
