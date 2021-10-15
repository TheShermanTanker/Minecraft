package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EntityRavager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCrops extends BlockPlant implements IBlockFragilePlantElement {
    public static final int MAX_AGE = 7;
    public static final BlockStateInteger AGE = BlockProperties.AGE_7;
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};

    protected BlockCrops(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(this.getAgeProperty(), Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_AGE[state.get(this.getAgeProperty())];
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(Blocks.FARMLAND);
    }

    public BlockStateInteger getAgeProperty() {
        return AGE;
    }

    public int getMaxAge() {
        return 7;
    }

    protected int getAge(IBlockData state) {
        return state.get(this.getAgeProperty());
    }

    public IBlockData setAge(int age) {
        return this.getBlockData().set(this.getAgeProperty(), Integer.valueOf(age));
    }

    public boolean isRipe(IBlockData state) {
        return state.get(this.getAgeProperty()) >= this.getMaxAge();
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return !this.isRipe(state);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getLightLevel(pos, 0) >= 9) {
            int i = this.getAge(state);
            if (i < this.getMaxAge()) {
                float f = getGrowthSpeed(this, world, pos);
                if (random.nextInt((int)(25.0F / f) + 1) == 0) {
                    world.setTypeAndData(pos, this.setAge(i + 1), 2);
                }
            }
        }

    }

    public void growCrops(World world, BlockPosition pos, IBlockData state) {
        int i = this.getAge(state) + this.getBonemealAgeIncrease(world);
        int j = this.getMaxAge();
        if (i > j) {
            i = j;
        }

        world.setTypeAndData(pos, this.setAge(i), 2);
    }

    protected int getBonemealAgeIncrease(World world) {
        return MathHelper.nextInt(world.random, 2, 5);
    }

    protected static float getGrowthSpeed(Block block, IBlockAccess world, BlockPosition pos) {
        float f = 1.0F;
        BlockPosition blockPos = pos.below();

        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                float g = 0.0F;
                IBlockData blockState = world.getType(blockPos.offset(i, 0, j));
                if (blockState.is(Blocks.FARMLAND)) {
                    g = 1.0F;
                    if (blockState.get(BlockSoil.MOISTURE) > 0) {
                        g = 3.0F;
                    }
                }

                if (i != 0 || j != 0) {
                    g /= 4.0F;
                }

                f += g;
            }
        }

        BlockPosition blockPos2 = pos.north();
        BlockPosition blockPos3 = pos.south();
        BlockPosition blockPos4 = pos.west();
        BlockPosition blockPos5 = pos.east();
        boolean bl = world.getType(blockPos4).is(block) || world.getType(blockPos5).is(block);
        boolean bl2 = world.getType(blockPos2).is(block) || world.getType(blockPos3).is(block);
        if (bl && bl2) {
            f /= 2.0F;
        } else {
            boolean bl3 = world.getType(blockPos4.north()).is(block) || world.getType(blockPos5.north()).is(block) || world.getType(blockPos5.south()).is(block) || world.getType(blockPos4.south()).is(block);
            if (bl3) {
                f /= 2.0F;
            }
        }

        return f;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return (world.getLightLevel(pos, 0) >= 8 || world.canSeeSky(pos)) && super.canPlace(state, world, pos);
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (entity instanceof EntityRavager && world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            world.destroyBlock(pos, true, entity);
        }

        super.entityInside(state, world, pos, entity);
    }

    protected IMaterial getBaseSeedId() {
        return Items.WHEAT_SEEDS;
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(this.getBaseSeedId());
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return !this.isRipe(state);
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.growCrops(world, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }
}
