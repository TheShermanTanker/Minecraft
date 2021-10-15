package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypeFlowing;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockFluids extends Block implements IFluidSource {
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL;
    protected final FluidTypeFlowing fluid;
    private final List<Fluid> stateCache;
    public static final VoxelShape STABLE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    public static final ImmutableList<EnumDirection> POSSIBLE_FLOW_DIRECTIONS = ImmutableList.of(EnumDirection.DOWN, EnumDirection.SOUTH, EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.WEST);

    protected BlockFluids(FluidTypeFlowing fluid, BlockBase.Info settings) {
        super(settings);
        this.fluid = fluid;
        this.stateCache = Lists.newArrayList();
        this.stateCache.add(fluid.getSource(false));

        for(int i = 1; i < 8; ++i) {
            this.stateCache.add(fluid.getFlowing(8 - i, false));
        }

        this.stateCache.add(fluid.getFlowing(8, true));
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LEVEL, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return context.isAbove(STABLE_SHAPE, pos, true) && state.get(LEVEL) == 0 && context.canStandOnFluid(world.getFluid(pos.above()), this.fluid) ? STABLE_SHAPE : VoxelShapes.empty();
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.getFluid().isRandomlyTicking();
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        state.getFluid().randomTick(world, pos, random);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return false;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return !this.fluid.is(TagsFluid.LAVA);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        int i = state.get(LEVEL);
        return this.stateCache.get(Math.min(i, 8));
    }

    @Override
    public boolean skipRendering(IBlockData state, IBlockData stateFrom, EnumDirection direction) {
        return stateFrom.getFluid().getType().isSame(this.fluid);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.INVISIBLE;
    }

    @Override
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.empty();
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (this.shouldSpreadLiquid(world, pos, state)) {
            world.getFluidTickList().scheduleTick(pos, state.getFluid().getType(), this.fluid.getTickDelay(world));
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.getFluid().isSource() || neighborState.getFluid().isSource()) {
            world.getFluidTickList().scheduleTick(pos, state.getFluid().getType(), this.fluid.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (this.shouldSpreadLiquid(world, pos, state)) {
            world.getFluidTickList().scheduleTick(pos, state.getFluid().getType(), this.fluid.getTickDelay(world));
        }

    }

    private boolean shouldSpreadLiquid(World world, BlockPosition pos, IBlockData state) {
        if (this.fluid.is(TagsFluid.LAVA)) {
            boolean bl = world.getType(pos.below()).is(Blocks.SOUL_SOIL);

            for(EnumDirection direction : POSSIBLE_FLOW_DIRECTIONS) {
                BlockPosition blockPos = pos.relative(direction.opposite());
                if (world.getFluid(blockPos).is(TagsFluid.WATER)) {
                    Block block = world.getFluid(pos).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                    world.setTypeUpdate(pos, block.getBlockData());
                    this.fizz(world, pos);
                    return false;
                }

                if (bl && world.getType(blockPos).is(Blocks.BLUE_ICE)) {
                    world.setTypeUpdate(pos, Blocks.BASALT.getBlockData());
                    this.fizz(world, pos);
                    return false;
                }
            }
        }

        return true;
    }

    private void fizz(GeneratorAccess world, BlockPosition pos) {
        world.triggerEffect(1501, pos, 0);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LEVEL);
    }

    @Override
    public ItemStack removeFluid(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        if (state.get(LEVEL) == 0) {
            world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 11);
            return new ItemStack(this.fluid.getBucket());
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return this.fluid.getPickupSound();
    }
}
