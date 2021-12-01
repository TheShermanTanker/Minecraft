package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsEntity;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockChorusFlower extends Block {
    public static final int DEAD_AGE = 5;
    public static final BlockStateInteger AGE = BlockProperties.AGE_5;
    private final BlockChorusFruit plant;

    protected BlockChorusFlower(BlockChorusFruit plantBlock, BlockBase.Info settings) {
        super(settings);
        this.plant = plantBlock;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(AGE) < 5;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos.above();
        if (world.isEmpty(blockPos) && blockPos.getY() < world.getMaxBuildHeight()) {
            int i = state.get(AGE);
            if (i < 5) {
                boolean bl = false;
                boolean bl2 = false;
                IBlockData blockState = world.getType(pos.below());
                if (blockState.is(Blocks.END_STONE)) {
                    bl = true;
                } else if (blockState.is(this.plant)) {
                    int j = 1;

                    for(int k = 0; k < 4; ++k) {
                        IBlockData blockState2 = world.getType(pos.below(j + 1));
                        if (!blockState2.is(this.plant)) {
                            if (blockState2.is(Blocks.END_STONE)) {
                                bl2 = true;
                            }
                            break;
                        }

                        ++j;
                    }

                    if (j < 2 || j <= random.nextInt(bl2 ? 5 : 4)) {
                        bl = true;
                    }
                } else if (blockState.isAir()) {
                    bl = true;
                }

                if (bl && allNeighborsEmpty(world, blockPos, (EnumDirection)null) && world.isEmpty(pos.above(2))) {
                    world.setTypeAndData(pos, this.plant.getStateForPlacement(world, pos), 2);
                    this.placeGrownFlower(world, blockPos, i);
                } else if (i < 4) {
                    int l = random.nextInt(4);
                    if (bl2) {
                        ++l;
                    }

                    boolean bl3 = false;

                    for(int m = 0; m < l; ++m) {
                        EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
                        BlockPosition blockPos2 = pos.relative(direction);
                        if (world.isEmpty(blockPos2) && world.isEmpty(blockPos2.below()) && allNeighborsEmpty(world, blockPos2, direction.opposite())) {
                            this.placeGrownFlower(world, blockPos2, i + 1);
                            bl3 = true;
                        }
                    }

                    if (bl3) {
                        world.setTypeAndData(pos, this.plant.getStateForPlacement(world, pos), 2);
                    } else {
                        this.placeDeadFlower(world, pos);
                    }
                } else {
                    this.placeDeadFlower(world, pos);
                }

            }
        }
    }

    private void placeGrownFlower(World world, BlockPosition pos, int age) {
        world.setTypeAndData(pos, this.getBlockData().set(AGE, Integer.valueOf(age)), 2);
        world.triggerEffect(1033, pos, 0);
    }

    private void placeDeadFlower(World world, BlockPosition pos) {
        world.setTypeAndData(pos, this.getBlockData().set(AGE, Integer.valueOf(5)), 2);
        world.triggerEffect(1034, pos, 0);
    }

    private static boolean allNeighborsEmpty(IWorldReader world, BlockPosition pos, @Nullable EnumDirection exceptDirection) {
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (direction != exceptDirection && !world.isEmpty(pos.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction != EnumDirection.UP && !state.canPlace(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        if (!blockState.is(this.plant) && !blockState.is(Blocks.END_STONE)) {
            if (!blockState.isAir()) {
                return false;
            } else {
                boolean bl = false;

                for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                    IBlockData blockState2 = world.getType(pos.relative(direction));
                    if (blockState2.is(this.plant)) {
                        if (bl) {
                            return false;
                        }

                        bl = true;
                    } else if (!blockState2.isAir()) {
                        return false;
                    }
                }

                return bl;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    public static void generatePlant(GeneratorAccess world, BlockPosition pos, Random random, int size) {
        world.setTypeAndData(pos, ((BlockChorusFruit)Blocks.CHORUS_PLANT).getStateForPlacement(world, pos), 2);
        growTreeRecursive(world, pos, random, pos, size, 0);
    }

    private static void growTreeRecursive(GeneratorAccess world, BlockPosition pos, Random random, BlockPosition rootPos, int size, int layer) {
        BlockChorusFruit chorusPlantBlock = (BlockChorusFruit)Blocks.CHORUS_PLANT;
        int i = random.nextInt(4) + 1;
        if (layer == 0) {
            ++i;
        }

        for(int j = 0; j < i; ++j) {
            BlockPosition blockPos = pos.above(j + 1);
            if (!allNeighborsEmpty(world, blockPos, (EnumDirection)null)) {
                return;
            }

            world.setTypeAndData(blockPos, chorusPlantBlock.getStateForPlacement(world, blockPos), 2);
            world.setTypeAndData(blockPos.below(), chorusPlantBlock.getStateForPlacement(world, blockPos.below()), 2);
        }

        boolean bl = false;
        if (layer < 4) {
            int k = random.nextInt(4);
            if (layer == 0) {
                ++k;
            }

            for(int l = 0; l < k; ++l) {
                EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
                BlockPosition blockPos2 = pos.above(i).relative(direction);
                if (Math.abs(blockPos2.getX() - rootPos.getX()) < size && Math.abs(blockPos2.getZ() - rootPos.getZ()) < size && world.isEmpty(blockPos2) && world.isEmpty(blockPos2.below()) && allNeighborsEmpty(world, blockPos2, direction.opposite())) {
                    bl = true;
                    world.setTypeAndData(blockPos2, chorusPlantBlock.getStateForPlacement(world, blockPos2), 2);
                    world.setTypeAndData(blockPos2.relative(direction.opposite()), chorusPlantBlock.getStateForPlacement(world, blockPos2.relative(direction.opposite())), 2);
                    growTreeRecursive(world, blockPos2, random, rootPos, size, layer + 1);
                }
            }
        }

        if (!bl) {
            world.setTypeAndData(pos.above(i), Blocks.CHORUS_FLOWER.getBlockData().set(AGE, Integer.valueOf(5)), 2);
        }

    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        BlockPosition blockPos = hit.getBlockPosition();
        if (!world.isClientSide && projectile.mayInteract(world, blockPos) && projectile.getEntityType().is(TagsEntity.IMPACT_PROJECTILES)) {
            world.destroyBlock(blockPos, true, projectile);
        }

    }
}
