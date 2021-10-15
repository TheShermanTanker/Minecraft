package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.math.Vector3fa;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyRedstoneSide;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockRedstoneWire extends Block {
    public static final BlockStateEnum<BlockPropertyRedstoneSide> NORTH = BlockProperties.NORTH_REDSTONE;
    public static final BlockStateEnum<BlockPropertyRedstoneSide> EAST = BlockProperties.EAST_REDSTONE;
    public static final BlockStateEnum<BlockPropertyRedstoneSide> SOUTH = BlockProperties.SOUTH_REDSTONE;
    public static final BlockStateEnum<BlockPropertyRedstoneSide> WEST = BlockProperties.WEST_REDSTONE;
    public static final BlockStateInteger POWER = BlockProperties.POWER;
    public static final Map<EnumDirection, BlockStateEnum<BlockPropertyRedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, NORTH, EnumDirection.EAST, EAST, EnumDirection.SOUTH, SOUTH, EnumDirection.WEST, WEST));
    protected static final int H = 1;
    protected static final int W = 3;
    protected static final int E = 13;
    protected static final int N = 3;
    protected static final int S = 13;
    private static final VoxelShape SHAPE_DOT = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);
    private static final Map<EnumDirection, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, Block.box(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), EnumDirection.SOUTH, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), EnumDirection.EAST, Block.box(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), EnumDirection.WEST, Block.box(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)));
    private static final Map<EnumDirection, VoxelShape> SHAPES_UP = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, VoxelShapes.or(SHAPES_FLOOR.get(EnumDirection.NORTH), Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)), EnumDirection.SOUTH, VoxelShapes.or(SHAPES_FLOOR.get(EnumDirection.SOUTH), Block.box(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)), EnumDirection.EAST, VoxelShapes.or(SHAPES_FLOOR.get(EnumDirection.EAST), Block.box(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)), EnumDirection.WEST, VoxelShapes.or(SHAPES_FLOOR.get(EnumDirection.WEST), Block.box(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D))));
    private static final Map<IBlockData, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
    private static final Vec3D[] COLORS = SystemUtils.make(new Vec3D[16], (vec3s) -> {
        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float g = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float h = MathHelper.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float j = MathHelper.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            vec3s[i] = new Vec3D((double)g, (double)h, (double)j);
        }

    });
    private static final float PARTICLE_DENSITY = 0.2F;
    private final IBlockData crossState;
    public boolean shouldSignal = true;

    public BlockRedstoneWire(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, BlockPropertyRedstoneSide.NONE).set(EAST, BlockPropertyRedstoneSide.NONE).set(SOUTH, BlockPropertyRedstoneSide.NONE).set(WEST, BlockPropertyRedstoneSide.NONE).set(POWER, Integer.valueOf(0)));
        this.crossState = this.getBlockData().set(NORTH, BlockPropertyRedstoneSide.SIDE).set(EAST, BlockPropertyRedstoneSide.SIDE).set(SOUTH, BlockPropertyRedstoneSide.SIDE).set(WEST, BlockPropertyRedstoneSide.SIDE);

        for(IBlockData blockState : this.getStates().getPossibleStates()) {
            if (blockState.get(POWER) == 0) {
                SHAPES_CACHE.put(blockState, this.calculateShape(blockState));
            }
        }

    }

    private VoxelShape calculateShape(IBlockData state) {
        VoxelShape voxelShape = SHAPE_DOT;

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPropertyRedstoneSide redstoneSide = state.get(PROPERTY_BY_DIRECTION.get(direction));
            if (redstoneSide == BlockPropertyRedstoneSide.SIDE) {
                voxelShape = VoxelShapes.or(voxelShape, SHAPES_FLOOR.get(direction));
            } else if (redstoneSide == BlockPropertyRedstoneSide.UP) {
                voxelShape = VoxelShapes.or(voxelShape, SHAPES_UP.get(direction));
            }
        }

        return voxelShape;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPES_CACHE.get(state.set(POWER, Integer.valueOf(0)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getConnectionState(ctx.getWorld(), this.crossState, ctx.getClickPosition());
    }

    private IBlockData getConnectionState(IBlockAccess world, IBlockData state, BlockPosition pos) {
        boolean bl = isDot(state);
        state = this.getMissingConnections(world, this.getBlockData().set(POWER, state.get(POWER)), pos);
        if (bl && isDot(state)) {
            return state;
        } else {
            boolean bl2 = state.get(NORTH).isConnected();
            boolean bl3 = state.get(SOUTH).isConnected();
            boolean bl4 = state.get(EAST).isConnected();
            boolean bl5 = state.get(WEST).isConnected();
            boolean bl6 = !bl2 && !bl3;
            boolean bl7 = !bl4 && !bl5;
            if (!bl5 && bl6) {
                state = state.set(WEST, BlockPropertyRedstoneSide.SIDE);
            }

            if (!bl4 && bl6) {
                state = state.set(EAST, BlockPropertyRedstoneSide.SIDE);
            }

            if (!bl2 && bl7) {
                state = state.set(NORTH, BlockPropertyRedstoneSide.SIDE);
            }

            if (!bl3 && bl7) {
                state = state.set(SOUTH, BlockPropertyRedstoneSide.SIDE);
            }

            return state;
        }
    }

    private IBlockData getMissingConnections(IBlockAccess world, IBlockData state, BlockPosition pos) {
        boolean bl = !world.getType(pos.above()).isOccluding(world, pos);

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (!state.get(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
                BlockPropertyRedstoneSide redstoneSide = this.getConnectingSide(world, pos, direction, bl);
                state = state.set(PROPERTY_BY_DIRECTION.get(direction), redstoneSide);
            }
        }

        return state;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.DOWN) {
            return state;
        } else if (direction == EnumDirection.UP) {
            return this.getConnectionState(world, state, pos);
        } else {
            BlockPropertyRedstoneSide redstoneSide = this.getConnectingSide(world, pos, direction);
            return redstoneSide.isConnected() == state.get(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && !isCross(state) ? state.set(PROPERTY_BY_DIRECTION.get(direction), redstoneSide) : this.getConnectionState(world, this.crossState.set(POWER, state.get(POWER)).set(PROPERTY_BY_DIRECTION.get(direction), redstoneSide), pos);
        }
    }

    private static boolean isCross(IBlockData state) {
        return state.get(NORTH).isConnected() && state.get(SOUTH).isConnected() && state.get(EAST).isConnected() && state.get(WEST).isConnected();
    }

    private static boolean isDot(IBlockData state) {
        return !state.get(NORTH).isConnected() && !state.get(SOUTH).isConnected() && !state.get(EAST).isConnected() && !state.get(WEST).isConnected();
    }

    @Override
    public void updateIndirectNeighbourShapes(IBlockData state, GeneratorAccess world, BlockPosition pos, int flags, int maxUpdateDepth) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPropertyRedstoneSide redstoneSide = state.get(PROPERTY_BY_DIRECTION.get(direction));
            if (redstoneSide != BlockPropertyRedstoneSide.NONE && !world.getType(mutableBlockPos.setWithOffset(pos, direction)).is(this)) {
                mutableBlockPos.move(EnumDirection.DOWN);
                IBlockData blockState = world.getType(mutableBlockPos);
                if (!blockState.is(Blocks.OBSERVER)) {
                    BlockPosition blockPos = mutableBlockPos.relative(direction.opposite());
                    IBlockData blockState2 = blockState.updateState(direction.opposite(), world.getType(blockPos), world, mutableBlockPos, blockPos);
                    updateOrDestroy(blockState, blockState2, world, mutableBlockPos, flags, maxUpdateDepth);
                }

                mutableBlockPos.setWithOffset(pos, direction).move(EnumDirection.UP);
                IBlockData blockState3 = world.getType(mutableBlockPos);
                if (!blockState3.is(Blocks.OBSERVER)) {
                    BlockPosition blockPos2 = mutableBlockPos.relative(direction.opposite());
                    IBlockData blockState4 = blockState3.updateState(direction.opposite(), world.getType(blockPos2), world, mutableBlockPos, blockPos2);
                    updateOrDestroy(blockState3, blockState4, world, mutableBlockPos, flags, maxUpdateDepth);
                }
            }
        }

    }

    private BlockPropertyRedstoneSide getConnectingSide(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return this.getConnectingSide(world, pos, direction, !world.getType(pos.above()).isOccluding(world, pos));
    }

    private BlockPropertyRedstoneSide getConnectingSide(IBlockAccess world, BlockPosition pos, EnumDirection direction, boolean bl) {
        BlockPosition blockPos = pos.relative(direction);
        IBlockData blockState = world.getType(blockPos);
        if (bl) {
            boolean bl2 = this.canSurviveOn(world, blockPos, blockState);
            if (bl2 && shouldConnectTo(world.getType(blockPos.above()))) {
                if (blockState.isFaceSturdy(world, blockPos, direction.opposite())) {
                    return BlockPropertyRedstoneSide.UP;
                }

                return BlockPropertyRedstoneSide.SIDE;
            }
        }

        return !shouldConnectTo(blockState, direction) && (blockState.isOccluding(world, blockPos) || !shouldConnectTo(world.getType(blockPos.below()))) ? BlockPropertyRedstoneSide.NONE : BlockPropertyRedstoneSide.SIDE;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        return this.canSurviveOn(world, blockPos, blockState);
    }

    private boolean canSurviveOn(IBlockAccess world, BlockPosition pos, IBlockData floor) {
        return floor.isFaceSturdy(world, pos, EnumDirection.UP) || floor.is(Blocks.HOPPER);
    }

    private void updatePowerStrength(World world, BlockPosition pos, IBlockData state) {
        int i = this.calculateTargetStrength(world, pos);
        if (state.get(POWER) != i) {
            if (world.getType(pos) == state) {
                world.setTypeAndData(pos, state.set(POWER, Integer.valueOf(i)), 2);
            }

            Set<BlockPosition> set = Sets.newHashSet();
            set.add(pos);

            for(EnumDirection direction : EnumDirection.values()) {
                set.add(pos.relative(direction));
            }

            for(BlockPosition blockPos : set) {
                world.applyPhysics(blockPos, this);
            }
        }

    }

    private int calculateTargetStrength(World world, BlockPosition pos) {
        this.shouldSignal = false;
        int i = world.getBestNeighborSignal(pos);
        this.shouldSignal = true;
        int j = 0;
        if (i < 15) {
            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                BlockPosition blockPos = pos.relative(direction);
                IBlockData blockState = world.getType(blockPos);
                j = Math.max(j, this.getWireSignal(blockState));
                BlockPosition blockPos2 = pos.above();
                if (blockState.isOccluding(world, blockPos) && !world.getType(blockPos2).isOccluding(world, blockPos2)) {
                    j = Math.max(j, this.getWireSignal(world.getType(blockPos.above())));
                } else if (!blockState.isOccluding(world, blockPos)) {
                    j = Math.max(j, this.getWireSignal(world.getType(blockPos.below())));
                }
            }
        }

        return Math.max(i, j - 1);
    }

    private int getWireSignal(IBlockData state) {
        return state.is(this) ? state.get(POWER) : 0;
    }

    private void checkCornerChangeAt(World world, BlockPosition pos) {
        if (world.getType(pos).is(this)) {
            world.applyPhysics(pos, this);

            for(EnumDirection direction : EnumDirection.values()) {
                world.applyPhysics(pos.relative(direction), this);
            }

        }
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide) {
            this.updatePowerStrength(world, pos, state);

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.VERTICAL) {
                world.applyPhysics(pos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(world, pos);
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            super.remove(state, world, pos, newState, moved);
            if (!world.isClientSide) {
                for(EnumDirection direction : EnumDirection.values()) {
                    world.applyPhysics(pos.relative(direction), this);
                }

                this.updatePowerStrength(world, pos, state);
                this.updateNeighborsOfNeighboringWires(world, pos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(World world, BlockPosition pos) {
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.checkCornerChangeAt(world, pos.relative(direction));
        }

        for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction2);
            if (world.getType(blockPos).isOccluding(world, blockPos)) {
                this.checkCornerChangeAt(world, blockPos.above());
            } else {
                this.checkCornerChangeAt(world, blockPos.below());
            }
        }

    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            if (state.canPlace(world, pos)) {
                this.updatePowerStrength(world, pos, state);
            } else {
                dropResources(state, world, pos);
                world.removeBlock(pos, false);
            }

        }
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return !this.shouldSignal ? 0 : state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        if (this.shouldSignal && direction != EnumDirection.DOWN) {
            int i = state.get(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != EnumDirection.UP && !this.getConnectionState(world, state, pos).get(PROPERTY_BY_DIRECTION.get(direction.opposite())).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    protected static boolean shouldConnectTo(IBlockData state) {
        return shouldConnectTo(state, (EnumDirection)null);
    }

    protected static boolean shouldConnectTo(IBlockData state, @Nullable EnumDirection dir) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (state.is(Blocks.REPEATER)) {
            EnumDirection direction = state.get(BlockRepeater.FACING);
            return direction == dir || direction.opposite() == dir;
        } else if (state.is(Blocks.OBSERVER)) {
            return dir == state.get(BlockObserver.FACING);
        } else {
            return state.isPowerSource() && dir != null;
        }
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return this.shouldSignal;
    }

    public static int getColorForPower(int powerLevel) {
        Vec3D vec3 = COLORS[powerLevel];
        return MathHelper.color((float)vec3.getX(), (float)vec3.getY(), (float)vec3.getZ());
    }

    private void spawnParticlesAlongLine(World world, Random random, BlockPosition pos, Vec3D color, EnumDirection direction, EnumDirection direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() >= 0.2F * h)) {
            float i = 0.4375F;
            float j = f + h * random.nextFloat();
            double d = 0.5D + (double)(0.4375F * (float)direction.getAdjacentX()) + (double)(j * (float)direction2.getAdjacentX());
            double e = 0.5D + (double)(0.4375F * (float)direction.getAdjacentY()) + (double)(j * (float)direction2.getAdjacentY());
            double k = 0.5D + (double)(0.4375F * (float)direction.getAdjacentZ()) + (double)(j * (float)direction2.getAdjacentZ());
            world.addParticle(new ParticleParamRedstone(new Vector3fa(color), 1.0F), (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + k, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        int i = state.get(POWER);
        if (i != 0) {
            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                BlockPropertyRedstoneSide redstoneSide = state.get(PROPERTY_BY_DIRECTION.get(direction));
                switch(redstoneSide) {
                case UP:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], direction, EnumDirection.UP, -0.5F, 0.5F);
                case SIDE:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], EnumDirection.DOWN, direction, 0.0F, 0.5F);
                    break;
                case NONE:
                default:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], EnumDirection.DOWN, direction, 0.0F, 0.3F);
                }
            }

        }
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.set(NORTH, state.get(SOUTH)).set(EAST, state.get(WEST)).set(SOUTH, state.get(NORTH)).set(WEST, state.get(EAST));
        case COUNTERCLOCKWISE_90:
            return state.set(NORTH, state.get(EAST)).set(EAST, state.get(SOUTH)).set(SOUTH, state.get(WEST)).set(WEST, state.get(NORTH));
        case CLOCKWISE_90:
            return state.set(NORTH, state.get(WEST)).set(EAST, state.get(NORTH)).set(SOUTH, state.get(EAST)).set(WEST, state.get(SOUTH));
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.set(NORTH, state.get(SOUTH)).set(SOUTH, state.get(NORTH));
        case FRONT_BACK:
            return state.set(EAST, state.get(WEST)).set(WEST, state.get(EAST));
        default:
            return super.mirror(state, mirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (!player.getAbilities().mayBuild) {
            return EnumInteractionResult.PASS;
        } else {
            if (isCross(state) || isDot(state)) {
                IBlockData blockState = isCross(state) ? this.getBlockData() : this.crossState;
                blockState = blockState.set(POWER, state.get(POWER));
                blockState = this.getConnectionState(world, blockState, pos);
                if (blockState != state) {
                    world.setTypeAndData(pos, blockState, 3);
                    this.updatesOnShapeChange(world, pos, state, blockState);
                    return EnumInteractionResult.SUCCESS;
                }
            }

            return EnumInteractionResult.PASS;
        }
    }

    private void updatesOnShapeChange(World world, BlockPosition pos, IBlockData oldState, IBlockData newState) {
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            if (oldState.get(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != newState.get(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && world.getType(blockPos).isOccluding(world, blockPos)) {
                world.updateNeighborsAtExceptFromFacing(blockPos, newState.getBlock(), direction.opposite());
            }
        }

    }
}
