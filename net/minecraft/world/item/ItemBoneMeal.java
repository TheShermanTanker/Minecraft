package net.minecraft.world.item;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BlockCoralFanWallAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IBlockFragilePlantElement;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemBoneMeal extends Item {
    public static final int GRASS_SPREAD_WIDTH = 3;
    public static final int GRASS_SPREAD_HEIGHT = 1;
    public static final int GRASS_COUNT_MULTIPLIER = 3;

    public ItemBoneMeal(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        BlockPosition blockPos2 = blockPos.relative(context.getClickedFace());
        if (growCrop(context.getItemStack(), level, blockPos)) {
            if (!level.isClientSide) {
                level.triggerEffect(1505, blockPos, 0);
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            IBlockData blockState = level.getType(blockPos);
            boolean bl = blockState.isFaceSturdy(level, blockPos, context.getClickedFace());
            if (bl && growWaterPlant(context.getItemStack(), level, blockPos2, context.getClickedFace())) {
                if (!level.isClientSide) {
                    level.triggerEffect(1505, blockPos2, 0);
                }

                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    public static boolean growCrop(ItemStack stack, World world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (blockState.getBlock() instanceof IBlockFragilePlantElement) {
            IBlockFragilePlantElement bonemealableBlock = (IBlockFragilePlantElement)blockState.getBlock();
            if (bonemealableBlock.isValidBonemealTarget(world, pos, blockState, world.isClientSide)) {
                if (world instanceof WorldServer) {
                    if (bonemealableBlock.isBonemealSuccess(world, world.random, pos, blockState)) {
                        bonemealableBlock.performBonemeal((WorldServer)world, world.random, pos, blockState);
                    }

                    stack.subtract(1);
                }

                return true;
            }
        }

        return false;
    }

    public static boolean growWaterPlant(ItemStack stack, World world, BlockPosition blockPos, @Nullable EnumDirection facing) {
        if (world.getType(blockPos).is(Blocks.WATER) && world.getFluid(blockPos).getAmount() == 8) {
            if (!(world instanceof WorldServer)) {
                return true;
            } else {
                Random random = world.getRandom();

                label76:
                for(int i = 0; i < 128; ++i) {
                    BlockPosition blockPos2 = blockPos;
                    IBlockData blockState = Blocks.SEAGRASS.getBlockData();

                    for(int j = 0; j < i / 16; ++j) {
                        blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                        if (world.getType(blockPos2).isCollisionShapeFullBlock(world, blockPos2)) {
                            continue label76;
                        }
                    }

                    Optional<ResourceKey<BiomeBase>> optional = world.getBiomeName(blockPos2);
                    if (Objects.equals(optional, Optional.of(Biomes.WARM_OCEAN))) {
                        if (i == 0 && facing != null && facing.getAxis().isHorizontal()) {
                            blockState = TagsBlock.WALL_CORALS.getRandomElement(world.random).getBlockData().set(BlockCoralFanWallAbstract.FACING, facing);
                        } else if (random.nextInt(4) == 0) {
                            blockState = TagsBlock.UNDERWATER_BONEMEALS.getRandomElement(random).getBlockData();
                        }
                    }

                    if (blockState.is(TagsBlock.WALL_CORALS)) {
                        for(int k = 0; !blockState.canPlace(world, blockPos2) && k < 4; ++k) {
                            blockState = blockState.set(BlockCoralFanWallAbstract.FACING, EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random));
                        }
                    }

                    if (blockState.canPlace(world, blockPos2)) {
                        IBlockData blockState2 = world.getType(blockPos2);
                        if (blockState2.is(Blocks.WATER) && world.getFluid(blockPos2).getAmount() == 8) {
                            world.setTypeAndData(blockPos2, blockState, 3);
                        } else if (blockState2.is(Blocks.SEAGRASS) && random.nextInt(10) == 0) {
                            ((IBlockFragilePlantElement)Blocks.SEAGRASS).performBonemeal((WorldServer)world, random, blockPos2, blockState2);
                        }
                    }
                }

                stack.subtract(1);
                return true;
            }
        } else {
            return false;
        }
    }

    public static void addGrowthParticles(GeneratorAccess world, BlockPosition pos, int count) {
        if (count == 0) {
            count = 15;
        }

        IBlockData blockState = world.getType(pos);
        if (!blockState.isAir()) {
            double d = 0.5D;
            double e;
            if (blockState.is(Blocks.WATER)) {
                count *= 3;
                e = 1.0D;
                d = 3.0D;
            } else if (blockState.isSolidRender(world, pos)) {
                pos = pos.above();
                count *= 3;
                d = 3.0D;
                e = 1.0D;
            } else {
                e = blockState.getShape(world, pos).max(EnumDirection.EnumAxis.Y);
            }

            world.addParticle(Particles.HAPPY_VILLAGER, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
            Random random = world.getRandom();

            for(int i = 0; i < count; ++i) {
                double h = random.nextGaussian() * 0.02D;
                double j = random.nextGaussian() * 0.02D;
                double k = random.nextGaussian() * 0.02D;
                double l = 0.5D - d;
                double m = (double)pos.getX() + l + random.nextDouble() * d * 2.0D;
                double n = (double)pos.getY() + random.nextDouble() * e;
                double o = (double)pos.getZ() + l + random.nextDouble() * d * 2.0D;
                if (!world.getType((new BlockPosition(m, n, o)).below()).isAir()) {
                    world.addParticle(Particles.HAPPY_VILLAGER, m, n, o, h, j, k);
                }
            }

        }
    }
}
