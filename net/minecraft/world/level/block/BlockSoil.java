package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.piston.BlockPistonMoving;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSoil extends Block {
    public static final BlockStateInteger MOISTURE = BlockProperties.MOISTURE;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    public static final int MAX_MOISTURE = 7;

    protected BlockSoil(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(MOISTURE, Integer.valueOf(0)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.UP && !state.canPlace(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.above());
        return !blockState.getMaterial().isBuildable() || blockState.getBlock() instanceof BlockFenceGate || blockState.getBlock() instanceof BlockPistonMoving;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return !this.getBlockData().canPlace(ctx.getWorld(), ctx.getClickPosition()) ? Blocks.DIRT.getBlockData() : super.getPlacedState(ctx);
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            fade(state, world, pos);
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        int i = state.get(MOISTURE);
        if (!isNearWater(world, pos) && !world.isRainingAt(pos.above())) {
            if (i > 0) {
                world.setTypeAndData(pos, state.set(MOISTURE, Integer.valueOf(i - 1)), 2);
            } else if (!isUnderCrops(world, pos)) {
                fade(state, world, pos);
            }
        } else if (i < 7) {
            world.setTypeAndData(pos, state.set(MOISTURE, Integer.valueOf(7)), 2);
        }

    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        if (!world.isClientSide && world.random.nextFloat() < fallDistance - 0.5F && entity instanceof EntityLiving && (entity instanceof EntityHuman || world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) && entity.getWidth() * entity.getWidth() * entity.getHeight() > 0.512F) {
            fade(state, world, pos);
        }

        super.fallOn(world, state, pos, entity, fallDistance);
    }

    public static void fade(IBlockData state, World world, BlockPosition pos) {
        world.setTypeUpdate(pos, pushEntitiesUp(state, Blocks.DIRT.getBlockData(), world, pos));
    }

    private static boolean isUnderCrops(IBlockAccess world, BlockPosition pos) {
        Block block = world.getType(pos.above()).getBlock();
        return block instanceof BlockCrops || block instanceof BlockStem || block instanceof BlockStemAttached;
    }

    private static boolean isNearWater(IWorldReader world, BlockPosition pos) {
        for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (world.getFluid(blockPos).is(TagsFluid.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(MOISTURE);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
