package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockTorchWall extends BlockTorch {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    protected static final float AABB_OFFSET = 2.5F;
    private static final Map<EnumDirection, VoxelShape> AABBS = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, Block.box(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D), EnumDirection.SOUTH, Block.box(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D), EnumDirection.WEST, Block.box(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D), EnumDirection.EAST, Block.box(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D)));

    protected BlockTorchWall(BlockBase.Info settings, ParticleParam particle) {
        super(settings, particle);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH));
    }

    @Override
    public String getDescriptionId() {
        return this.getItem().getName();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return getShape(state);
    }

    public static VoxelShape getShape(IBlockData state) {
        return AABBS.get(state.get(FACING));
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        IBlockData blockState = world.getType(blockPos);
        return blockState.isFaceSturdy(world, blockPos, direction);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = this.getBlockData();
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection[] directions = ctx.getNearestLookingDirections();

        for(EnumDirection direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                EnumDirection direction2 = direction.opposite();
                blockState = blockState.set(FACING, direction2);
                if (blockState.canPlace(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction.opposite() == state.get(FACING) && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : state;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        EnumDirection direction = state.get(FACING);
        double d = (double)pos.getX() + 0.5D;
        double e = (double)pos.getY() + 0.7D;
        double f = (double)pos.getZ() + 0.5D;
        double g = 0.22D;
        double h = 0.27D;
        EnumDirection direction2 = direction.opposite();
        world.addParticle(Particles.SMOKE, d + 0.27D * (double)direction2.getAdjacentX(), e + 0.22D, f + 0.27D * (double)direction2.getAdjacentZ(), 0.0D, 0.0D, 0.0D);
        world.addParticle(this.flameParticle, d + 0.27D * (double)direction2.getAdjacentX(), e + 0.22D, f + 0.27D * (double)direction2.getAdjacentZ(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }
}
