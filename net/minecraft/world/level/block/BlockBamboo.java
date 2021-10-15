package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemSword;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBamboo extends Block implements IBlockFragilePlantElement {
    protected static final float SMALL_LEAVES_AABB_OFFSET = 3.0F;
    protected static final float LARGE_LEAVES_AABB_OFFSET = 5.0F;
    protected static final float COLLISION_AABB_OFFSET = 1.5F;
    protected static final VoxelShape SMALL_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    protected static final VoxelShape LARGE_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    protected static final VoxelShape COLLISION_SHAPE = Block.box(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
    public static final BlockStateInteger AGE = BlockProperties.AGE_1;
    public static final BlockStateEnum<BlockPropertyBambooSize> LEAVES = BlockProperties.BAMBOO_LEAVES;
    public static final BlockStateInteger STAGE = BlockProperties.STAGE;
    public static final int MAX_HEIGHT = 16;
    public static final int STAGE_GROWING = 0;
    public static final int STAGE_DONE_GROWING = 1;
    public static final int AGE_THIN_BAMBOO = 0;
    public static final int AGE_THICK_BAMBOO = 1;

    public BlockBamboo(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)).set(LEAVES, BlockPropertyBambooSize.NONE).set(STAGE, Integer.valueOf(0)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE, LEAVES, STAGE);
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        VoxelShape voxelShape = state.get(LEAVES) == BlockPropertyBambooSize.LARGE ? LARGE_SHAPE : SMALL_SHAPE;
        Vec3D vec3 = state.getOffset(world, pos);
        return voxelShape.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        Vec3D vec3 = state.getOffset(world, pos);
        return COLLISION_SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean isCollisionShapeFullBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return false;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        if (!fluidState.isEmpty()) {
            return null;
        } else {
            IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().below());
            if (blockState.is(TagsBlock.BAMBOO_PLANTABLE_ON)) {
                if (blockState.is(Blocks.BAMBOO_SAPLING)) {
                    return this.getBlockData().set(AGE, Integer.valueOf(0));
                } else if (blockState.is(Blocks.BAMBOO)) {
                    int i = blockState.get(AGE) > 0 ? 1 : 0;
                    return this.getBlockData().set(AGE, Integer.valueOf(i));
                } else {
                    IBlockData blockState2 = ctx.getWorld().getType(ctx.getClickPosition().above());
                    return blockState2.is(Blocks.BAMBOO) ? this.getBlockData().set(AGE, blockState2.get(AGE)) : Blocks.BAMBOO_SAPLING.getBlockData();
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(STAGE) == 0;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(STAGE) == 0) {
            if (random.nextInt(3) == 0 && world.isEmpty(pos.above()) && world.getLightLevel(pos.above(), 0) >= 9) {
                int i = this.getHeightBelowUpToMax(world, pos) + 1;
                if (i < 16) {
                    this.growBamboo(state, world, pos, random, i);
                }
            }

        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.below()).is(TagsBlock.BAMBOO_PLANTABLE_ON);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!state.canPlace(world, pos)) {
            world.getBlockTickList().scheduleTick(pos, this, 1);
        }

        if (direction == EnumDirection.UP && neighborState.is(Blocks.BAMBOO) && neighborState.get(AGE) > state.get(AGE)) {
            world.setTypeAndData(pos, state.cycle(AGE), 2);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        int i = this.getHeightAboveUpToMax(world, pos);
        int j = this.getHeightBelowUpToMax(world, pos);
        return i + j + 1 < 16 && world.getType(pos.above(i)).get(STAGE) != 1;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        int i = this.getHeightAboveUpToMax(world, pos);
        int j = this.getHeightBelowUpToMax(world, pos);
        int k = i + j + 1;
        int l = 1 + random.nextInt(2);

        for(int m = 0; m < l; ++m) {
            BlockPosition blockPos = pos.above(i);
            IBlockData blockState = world.getType(blockPos);
            if (k >= 16 || blockState.get(STAGE) == 1 || !world.isEmpty(blockPos.above())) {
                return;
            }

            this.growBamboo(blockState, world, blockPos, random, k);
            ++i;
            ++k;
        }

    }

    @Override
    public float getDamage(IBlockData state, EntityHuman player, IBlockAccess world, BlockPosition pos) {
        return player.getItemInMainHand().getItem() instanceof ItemSword ? 1.0F : super.getDamage(state, player, world, pos);
    }

    protected void growBamboo(IBlockData state, World world, BlockPosition pos, Random random, int height) {
        IBlockData blockState = world.getType(pos.below());
        BlockPosition blockPos = pos.below(2);
        IBlockData blockState2 = world.getType(blockPos);
        BlockPropertyBambooSize bambooLeaves = BlockPropertyBambooSize.NONE;
        if (height >= 1) {
            if (blockState.is(Blocks.BAMBOO) && blockState.get(LEAVES) != BlockPropertyBambooSize.NONE) {
                if (blockState.is(Blocks.BAMBOO) && blockState.get(LEAVES) != BlockPropertyBambooSize.NONE) {
                    bambooLeaves = BlockPropertyBambooSize.LARGE;
                    if (blockState2.is(Blocks.BAMBOO)) {
                        world.setTypeAndData(pos.below(), blockState.set(LEAVES, BlockPropertyBambooSize.SMALL), 3);
                        world.setTypeAndData(blockPos, blockState2.set(LEAVES, BlockPropertyBambooSize.NONE), 3);
                    }
                }
            } else {
                bambooLeaves = BlockPropertyBambooSize.SMALL;
            }
        }

        int i = state.get(AGE) != 1 && !blockState2.is(Blocks.BAMBOO) ? 0 : 1;
        int j = (height < 11 || !(random.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;
        world.setTypeAndData(pos.above(), this.getBlockData().set(AGE, Integer.valueOf(i)).set(LEAVES, bambooLeaves).set(STAGE, Integer.valueOf(j)), 3);
    }

    protected int getHeightAboveUpToMax(IBlockAccess world, BlockPosition pos) {
        int i;
        for(i = 0; i < 16 && world.getType(pos.above(i + 1)).is(Blocks.BAMBOO); ++i) {
        }

        return i;
    }

    protected int getHeightBelowUpToMax(IBlockAccess world, BlockPosition pos) {
        int i;
        for(i = 0; i < 16 && world.getType(pos.below(i + 1)).is(Blocks.BAMBOO); ++i) {
        }

        return i;
    }
}
