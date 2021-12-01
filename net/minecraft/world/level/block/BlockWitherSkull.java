package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetector;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.predicate.MaterialPredicate;
import net.minecraft.world.level.material.Material;

public class BlockWitherSkull extends BlockSkull {
    @Nullable
    private static ShapeDetector witherPatternFull;
    @Nullable
    private static ShapeDetector witherPatternBase;

    protected BlockWitherSkull(BlockBase.Info settings) {
        super(BlockSkull.Type.WITHER_SKELETON, settings);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        super.postPlace(world, pos, state, placer, itemStack);
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntitySkull) {
            checkSpawn(world, pos, (TileEntitySkull)blockEntity);
        }

    }

    public static void checkSpawn(World world, BlockPosition pos, TileEntitySkull blockEntity) {
        if (!world.isClientSide) {
            IBlockData blockState = blockEntity.getBlock();
            boolean bl = blockState.is(Blocks.WITHER_SKELETON_SKULL) || blockState.is(Blocks.WITHER_SKELETON_WALL_SKULL);
            if (bl && pos.getY() >= world.getMinBuildHeight() && world.getDifficulty() != EnumDifficulty.PEACEFUL) {
                ShapeDetector blockPattern = getOrCreateWitherFull();
                ShapeDetector.ShapeDetectorCollection blockPatternMatch = blockPattern.find(world, pos);
                if (blockPatternMatch != null) {
                    for(int i = 0; i < blockPattern.getWidth(); ++i) {
                        for(int j = 0; j < blockPattern.getHeight(); ++j) {
                            ShapeDetectorBlock blockInWorld = blockPatternMatch.getBlock(i, j, 0);
                            world.setTypeAndData(blockInWorld.getPosition(), Blocks.AIR.getBlockData(), 2);
                            world.triggerEffect(2001, blockInWorld.getPosition(), Block.getCombinedId(blockInWorld.getState()));
                        }
                    }

                    EntityWither witherBoss = EntityTypes.WITHER.create(world);
                    BlockPosition blockPos = blockPatternMatch.getBlock(1, 2, 0).getPosition();
                    witherBoss.setPositionRotation((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.55D, (double)blockPos.getZ() + 0.5D, blockPatternMatch.getFacing().getAxis() == EnumDirection.EnumAxis.X ? 0.0F : 90.0F, 0.0F);
                    witherBoss.yBodyRot = blockPatternMatch.getFacing().getAxis() == EnumDirection.EnumAxis.X ? 0.0F : 90.0F;
                    witherBoss.beginSpawnSequence();

                    for(EntityPlayer serverPlayer : world.getEntitiesOfClass(EntityPlayer.class, witherBoss.getBoundingBox().inflate(50.0D))) {
                        CriterionTriggers.SUMMONED_ENTITY.trigger(serverPlayer, witherBoss);
                    }

                    world.addEntity(witherBoss);

                    for(int k = 0; k < blockPattern.getWidth(); ++k) {
                        for(int l = 0; l < blockPattern.getHeight(); ++l) {
                            world.update(blockPatternMatch.getBlock(k, l, 0).getPosition(), Blocks.AIR);
                        }
                    }

                }
            }
        }
    }

    public static boolean canSpawnMob(World world, BlockPosition pos, ItemStack stack) {
        if (stack.is(Items.WITHER_SKELETON_SKULL) && pos.getY() >= world.getMinBuildHeight() + 2 && world.getDifficulty() != EnumDifficulty.PEACEFUL && !world.isClientSide) {
            return getOrCreateWitherBase().find(world, pos) != null;
        } else {
            return false;
        }
    }

    private static ShapeDetector getOrCreateWitherFull() {
        if (witherPatternFull == null) {
            witherPatternFull = ShapeDetectorBuilder.start().aisle("^^^", "###", "~#~").where('#', (pos) -> {
                return pos.getState().is(TagsBlock.WITHER_SUMMON_BASE_BLOCKS);
            }).where('^', ShapeDetectorBlock.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL)))).where('~', ShapeDetectorBlock.hasState(MaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return witherPatternFull;
    }

    private static ShapeDetector getOrCreateWitherBase() {
        if (witherPatternBase == null) {
            witherPatternBase = ShapeDetectorBuilder.start().aisle("   ", "###", "~#~").where('#', (pos) -> {
                return pos.getState().is(TagsBlock.WITHER_SUMMON_BASE_BLOCKS);
            }).where('~', ShapeDetectorBlock.hasState(MaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return witherPatternBase;
    }
}
