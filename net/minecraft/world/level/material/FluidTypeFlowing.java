package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IFluidContainer;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public abstract class FluidTypeFlowing extends FluidType {
    public static final BlockStateBoolean FALLING = BlockProperties.FALLING;
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL_FLOWING;
    private static final int CACHE_SIZE = 200;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(200) {
            @Override
            protected void rehash(int i) {
            }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
        return object2ByteLinkedOpenHashMap;
    });
    private final Map<Fluid, VoxelShape> shapes = Maps.newIdentityHashMap();

    @Override
    protected void createFluidStateDefinition(BlockStateList.Builder<FluidType, Fluid> builder) {
        builder.add(FALLING);
    }

    @Override
    public Vec3D getFlow(IBlockAccess world, BlockPosition pos, Fluid state) {
        double d = 0.0D;
        double e = 0.0D;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            mutableBlockPos.setWithOffset(pos, direction);
            Fluid fluidState = world.getFluid(mutableBlockPos);
            if (this.affectsFlow(fluidState)) {
                float f = fluidState.getOwnHeight();
                float g = 0.0F;
                if (f == 0.0F) {
                    if (!world.getType(mutableBlockPos).getMaterial().isSolid()) {
                        BlockPosition blockPos = mutableBlockPos.below();
                        Fluid fluidState2 = world.getFluid(blockPos);
                        if (this.affectsFlow(fluidState2)) {
                            f = fluidState2.getOwnHeight();
                            if (f > 0.0F) {
                                g = state.getOwnHeight() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    g = state.getOwnHeight() - f;
                }

                if (g != 0.0F) {
                    d += (double)((float)direction.getAdjacentX() * g);
                    e += (double)((float)direction.getAdjacentZ() * g);
                }
            }
        }

        Vec3D vec3 = new Vec3D(d, 0.0D, e);
        if (state.get(FALLING)) {
            for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                mutableBlockPos.setWithOffset(pos, direction2);
                if (this.isSolidFace(world, mutableBlockPos, direction2) || this.isSolidFace(world, mutableBlockPos.above(), direction2)) {
                    vec3 = vec3.normalize().add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return vec3.normalize();
    }

    private boolean affectsFlow(Fluid state) {
        return state.isEmpty() || state.getType().isSame(this);
    }

    protected boolean isSolidFace(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        IBlockData blockState = world.getType(pos);
        Fluid fluidState = world.getFluid(pos);
        if (fluidState.getType().isSame(this)) {
            return false;
        } else if (direction == EnumDirection.UP) {
            return true;
        } else {
            return blockState.getMaterial() == Material.ICE ? false : blockState.isFaceSturdy(world, pos, direction);
        }
    }

    protected void spread(GeneratorAccess world, BlockPosition fluidPos, Fluid state) {
        if (!state.isEmpty()) {
            IBlockData blockState = world.getType(fluidPos);
            BlockPosition blockPos = fluidPos.below();
            IBlockData blockState2 = world.getType(blockPos);
            Fluid fluidState = this.getNewLiquid(world, blockPos, blockState2);
            if (this.canSpreadTo(world, fluidPos, blockState, EnumDirection.DOWN, blockPos, blockState2, world.getFluid(blockPos), fluidState.getType())) {
                this.spreadTo(world, blockPos, blockState2, EnumDirection.DOWN, fluidState);
                if (this.sourceNeighborCount(world, fluidPos) >= 3) {
                    this.spreadToSides(world, fluidPos, state, blockState);
                }
            } else if (state.isSource() || !this.isWaterHole(world, fluidState.getType(), fluidPos, blockState, blockPos, blockState2)) {
                this.spreadToSides(world, fluidPos, state, blockState);
            }

        }
    }

    private void spreadToSides(GeneratorAccess world, BlockPosition pos, Fluid fluidState, IBlockData blockState) {
        int i = fluidState.getAmount() - this.getDropOff(world);
        if (fluidState.get(FALLING)) {
            i = 7;
        }

        if (i > 0) {
            Map<EnumDirection, Fluid> map = this.getSpread(world, pos, blockState);

            for(Entry<EnumDirection, Fluid> entry : map.entrySet()) {
                EnumDirection direction = entry.getKey();
                Fluid fluidState2 = entry.getValue();
                BlockPosition blockPos = pos.relative(direction);
                IBlockData blockState2 = world.getType(blockPos);
                if (this.canSpreadTo(world, pos, blockState, direction, blockPos, blockState2, world.getFluid(blockPos), fluidState2.getType())) {
                    this.spreadTo(world, blockPos, blockState2, direction, fluidState2);
                }
            }

        }
    }

    protected Fluid getNewLiquid(IWorldReader world, BlockPosition pos, IBlockData state) {
        int i = 0;
        int j = 0;

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            IBlockData blockState = world.getType(blockPos);
            Fluid fluidState = blockState.getFluid();
            if (fluidState.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockPos, blockState)) {
                if (fluidState.isSource()) {
                    ++j;
                }

                i = Math.max(i, fluidState.getAmount());
            }
        }

        if (this.canConvertToSource() && j >= 2) {
            IBlockData blockState2 = world.getType(pos.below());
            Fluid fluidState2 = blockState2.getFluid();
            if (blockState2.getMaterial().isBuildable() || this.isSourceBlockOfThisType(fluidState2)) {
                return this.getSource(false);
            }
        }

        BlockPosition blockPos2 = pos.above();
        IBlockData blockState3 = world.getType(blockPos2);
        Fluid fluidState3 = blockState3.getFluid();
        if (!fluidState3.isEmpty() && fluidState3.getType().isSame(this) && this.canPassThroughWall(EnumDirection.UP, world, pos, state, blockPos2, blockState3)) {
            return this.getFlowing(8, true);
        } else {
            int k = i - this.getDropOff(world);
            return k <= 0 ? FluidTypes.EMPTY.defaultFluidState() : this.getFlowing(k, false);
        }
    }

    private boolean canPassThroughWall(EnumDirection face, IBlockAccess world, BlockPosition pos, IBlockData state, BlockPosition fromPos, IBlockData fromState) {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap2;
        if (!state.getBlock().hasDynamicShape() && !fromState.getBlock().hasDynamicShape()) {
            object2ByteLinkedOpenHashMap2 = OCCLUSION_CACHE.get();
        } else {
            object2ByteLinkedOpenHashMap2 = null;
        }

        Block.BlockStatePairKey blockStatePairKey;
        if (object2ByteLinkedOpenHashMap2 != null) {
            blockStatePairKey = new Block.BlockStatePairKey(state, fromState, face);
            byte b = object2ByteLinkedOpenHashMap2.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            }
        } else {
            blockStatePairKey = null;
        }

        VoxelShape voxelShape = state.getCollisionShape(world, pos);
        VoxelShape voxelShape2 = fromState.getCollisionShape(world, fromPos);
        boolean bl = !VoxelShapes.mergedFaceOccludes(voxelShape, voxelShape2, face);
        if (object2ByteLinkedOpenHashMap2 != null) {
            if (object2ByteLinkedOpenHashMap2.size() == 200) {
                object2ByteLinkedOpenHashMap2.removeLastByte();
            }

            object2ByteLinkedOpenHashMap2.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
        }

        return bl;
    }

    public abstract FluidType getFlowing();

    public Fluid getFlowing(int level, boolean falling) {
        return this.getFlowing().defaultFluidState().set(LEVEL, Integer.valueOf(level)).set(FALLING, Boolean.valueOf(falling));
    }

    public abstract FluidType getSource();

    public Fluid getSource(boolean falling) {
        return this.getSource().defaultFluidState().set(FALLING, Boolean.valueOf(falling));
    }

    protected abstract boolean canConvertToSource();

    protected void spreadTo(GeneratorAccess world, BlockPosition pos, IBlockData state, EnumDirection direction, Fluid fluidState) {
        if (state.getBlock() instanceof IFluidContainer) {
            ((IFluidContainer)state.getBlock()).place(world, pos, state, fluidState);
        } else {
            if (!state.isAir()) {
                this.beforeDestroyingBlock(world, pos, state);
            }

            world.setTypeAndData(pos, fluidState.getBlockData(), 3);
        }

    }

    protected abstract void beforeDestroyingBlock(GeneratorAccess world, BlockPosition pos, IBlockData state);

    private static short getCacheKey(BlockPosition blockPos, BlockPosition blockPos2) {
        int i = blockPos2.getX() - blockPos.getX();
        int j = blockPos2.getZ() - blockPos.getZ();
        return (short)((i + 128 & 255) << 8 | j + 128 & 255);
    }

    protected int getSlopeDistance(IWorldReader world, BlockPosition blockPos, int i, EnumDirection direction, IBlockData blockState, BlockPosition blockPos2, Short2ObjectMap<Pair<IBlockData, Fluid>> short2ObjectMap, Short2BooleanMap short2BooleanMap) {
        int j = 1000;

        for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (direction2 != direction) {
                BlockPosition blockPos3 = blockPos.relative(direction2);
                short s = getCacheKey(blockPos2, blockPos3);
                Pair<IBlockData, Fluid> pair = short2ObjectMap.computeIfAbsent(s, (ix) -> {
                    IBlockData blockState = world.getType(blockPos3);
                    return Pair.of(blockState, blockState.getFluid());
                });
                IBlockData blockState2 = pair.getFirst();
                Fluid fluidState = pair.getSecond();
                if (this.canPassThrough(world, this.getFlowing(), blockPos, blockState, direction2, blockPos3, blockState2, fluidState)) {
                    boolean bl = short2BooleanMap.computeIfAbsent(s, (ix) -> {
                        BlockPosition blockPos2 = blockPos3.below();
                        IBlockData blockState2 = world.getType(blockPos2);
                        return this.isWaterHole(world, this.getFlowing(), blockPos3, blockState2, blockPos2, blockState2);
                    });
                    if (bl) {
                        return i;
                    }

                    if (i < this.getSlopeFindDistance(world)) {
                        int k = this.getSlopeDistance(world, blockPos3, i + 1, direction2.opposite(), blockState2, blockPos2, short2ObjectMap, short2BooleanMap);
                        if (k < j) {
                            j = k;
                        }
                    }
                }
            }
        }

        return j;
    }

    private boolean isWaterHole(IBlockAccess world, FluidType fluid, BlockPosition pos, IBlockData state, BlockPosition fromPos, IBlockData fromState) {
        if (!this.canPassThroughWall(EnumDirection.DOWN, world, pos, state, fromPos, fromState)) {
            return false;
        } else {
            return fromState.getFluid().getType().isSame(this) ? true : this.canHoldFluid(world, fromPos, fromState, fluid);
        }
    }

    private boolean canPassThrough(IBlockAccess world, FluidType fluid, BlockPosition pos, IBlockData state, EnumDirection face, BlockPosition fromPos, IBlockData fromState, Fluid fluidState) {
        return !this.isSourceBlockOfThisType(fluidState) && this.canPassThroughWall(face, world, pos, state, fromPos, fromState) && this.canHoldFluid(world, fromPos, fromState, fluid);
    }

    private boolean isSourceBlockOfThisType(Fluid state) {
        return state.getType().isSame(this) && state.isSource();
    }

    protected abstract int getSlopeFindDistance(IWorldReader world);

    private int sourceNeighborCount(IWorldReader world, BlockPosition pos) {
        int i = 0;

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            Fluid fluidState = world.getFluid(blockPos);
            if (this.isSourceBlockOfThisType(fluidState)) {
                ++i;
            }
        }

        return i;
    }

    protected Map<EnumDirection, Fluid> getSpread(IWorldReader world, BlockPosition pos, IBlockData state) {
        int i = 1000;
        Map<EnumDirection, Fluid> map = Maps.newEnumMap(EnumDirection.class);
        Short2ObjectMap<Pair<IBlockData, Fluid>> short2ObjectMap = new Short2ObjectOpenHashMap<>();
        Short2BooleanMap short2BooleanMap = new Short2BooleanOpenHashMap();

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            short s = getCacheKey(pos, blockPos);
            Pair<IBlockData, Fluid> pair = short2ObjectMap.computeIfAbsent(s, (ix) -> {
                IBlockData blockState = world.getType(blockPos);
                return Pair.of(blockState, blockState.getFluid());
            });
            IBlockData blockState = pair.getFirst();
            Fluid fluidState = pair.getSecond();
            Fluid fluidState2 = this.getNewLiquid(world, blockPos, blockState);
            if (this.canPassThrough(world, fluidState2.getType(), pos, state, direction, blockPos, blockState, fluidState)) {
                BlockPosition blockPos2 = blockPos.below();
                boolean bl = short2BooleanMap.computeIfAbsent(s, (ix) -> {
                    IBlockData blockState2 = world.getType(blockPos2);
                    return this.isWaterHole(world, this.getFlowing(), blockPos, blockState, blockPos2, blockState2);
                });
                int j;
                if (bl) {
                    j = 0;
                } else {
                    j = this.getSlopeDistance(world, blockPos, 1, direction.opposite(), blockState, pos, short2ObjectMap, short2BooleanMap);
                }

                if (j < i) {
                    map.clear();
                }

                if (j <= i) {
                    map.put(direction, fluidState2);
                    i = j;
                }
            }
        }

        return map;
    }

    private boolean canHoldFluid(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) {
        Block block = state.getBlock();
        if (block instanceof IFluidContainer) {
            return ((IFluidContainer)block).canPlace(world, pos, state, fluid);
        } else if (!(block instanceof BlockDoor) && !state.is(TagsBlock.SIGNS) && !state.is(Blocks.LADDER) && !state.is(Blocks.SUGAR_CANE) && !state.is(Blocks.BUBBLE_COLUMN)) {
            Material material = state.getMaterial();
            if (material != Material.PORTAL && material != Material.STRUCTURAL_AIR && material != Material.WATER_PLANT && material != Material.REPLACEABLE_WATER_PLANT) {
                return !material.isSolid();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean canSpreadTo(IBlockAccess world, BlockPosition fluidPos, IBlockData fluidBlockState, EnumDirection flowDirection, BlockPosition flowTo, IBlockData flowToBlockState, Fluid fluidState, FluidType fluid) {
        return fluidState.canBeReplacedWith(world, flowTo, fluid, flowDirection) && this.canPassThroughWall(flowDirection, world, fluidPos, fluidBlockState, flowTo, flowToBlockState) && this.canHoldFluid(world, flowTo, flowToBlockState, fluid);
    }

    protected abstract int getDropOff(IWorldReader world);

    protected int getSpreadDelay(World world, BlockPosition pos, Fluid oldState, Fluid newState) {
        return this.getTickDelay(world);
    }

    @Override
    public void tick(World world, BlockPosition pos, Fluid state) {
        if (!state.isSource()) {
            Fluid fluidState = this.getNewLiquid(world, pos, world.getType(pos));
            int i = this.getSpreadDelay(world, pos, state, fluidState);
            if (fluidState.isEmpty()) {
                state = fluidState;
                world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 3);
            } else if (!fluidState.equals(state)) {
                state = fluidState;
                IBlockData blockState = fluidState.getBlockData();
                world.setTypeAndData(pos, blockState, 2);
                world.getFluidTickList().scheduleTick(pos, fluidState.getType(), i);
                world.applyPhysics(pos, blockState.getBlock());
            }
        }

        this.spread(world, pos, state);
    }

    protected static int getLegacyLevel(Fluid state) {
        return state.isSource() ? 0 : 8 - Math.min(state.getAmount(), 8) + (state.get(FALLING) ? 8 : 0);
    }

    private static boolean hasSameAbove(Fluid state, IBlockAccess world, BlockPosition pos) {
        return state.getType().isSame(world.getFluid(pos.above()).getType());
    }

    @Override
    public float getHeight(Fluid state, IBlockAccess world, BlockPosition pos) {
        return hasSameAbove(state, world, pos) ? 1.0F : state.getOwnHeight();
    }

    @Override
    public float getOwnHeight(Fluid state) {
        return (float)state.getAmount() / 9.0F;
    }

    @Override
    public abstract int getAmount(Fluid state);

    @Override
    public VoxelShape getShape(Fluid state, IBlockAccess world, BlockPosition pos) {
        return state.getAmount() == 9 && hasSameAbove(state, world, pos) ? VoxelShapes.block() : this.shapes.computeIfAbsent(state, (fluidState) -> {
            return VoxelShapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)fluidState.getHeight(world, pos), 1.0D);
        });
    }
}
