package net.minecraft.world.level.block;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntitySnowman;
import net.minecraft.world.item.ItemWearable;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetector;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.predicate.MaterialPredicate;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.material.Material;

public class BlockPumpkinCarved extends BlockFacingHorizontal implements ItemWearable {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    @Nullable
    private ShapeDetector snowGolemBase;
    @Nullable
    private ShapeDetector snowGolemFull;
    @Nullable
    private ShapeDetector ironGolemBase;
    @Nullable
    private ShapeDetector ironGolemFull;
    private static final Predicate<IBlockData> PUMPKINS_PREDICATE = (state) -> {
        return state != null && (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN));
    };

    protected BlockPumpkinCarved(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH));
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.trySpawnGolem(world, pos);
        }
    }

    public boolean canSpawnGolem(IWorldReader world, BlockPosition pos) {
        return this.getOrCreateSnowGolemBase().find(world, pos) != null || this.getOrCreateIronGolemBase().find(world, pos) != null;
    }

    private void trySpawnGolem(World world, BlockPosition pos) {
        ShapeDetector.ShapeDetectorCollection blockPatternMatch = this.getSnowmanShape().find(world, pos);
        if (blockPatternMatch != null) {
            for(int i = 0; i < this.getSnowmanShape().getHeight(); ++i) {
                ShapeDetectorBlock blockInWorld = blockPatternMatch.getBlock(0, i, 0);
                world.setTypeAndData(blockInWorld.getPosition(), Blocks.AIR.getBlockData(), 2);
                world.triggerEffect(2001, blockInWorld.getPosition(), Block.getCombinedId(blockInWorld.getState()));
            }

            EntitySnowman snowGolem = EntityTypes.SNOW_GOLEM.create(world);
            BlockPosition blockPos = blockPatternMatch.getBlock(0, 2, 0).getPosition();
            snowGolem.setPositionRotation((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.05D, (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
            world.addEntity(snowGolem);

            for(EntityPlayer serverPlayer : world.getEntitiesOfClass(EntityPlayer.class, snowGolem.getBoundingBox().inflate(5.0D))) {
                CriterionTriggers.SUMMONED_ENTITY.trigger(serverPlayer, snowGolem);
            }

            for(int j = 0; j < this.getSnowmanShape().getHeight(); ++j) {
                ShapeDetectorBlock blockInWorld2 = blockPatternMatch.getBlock(0, j, 0);
                world.update(blockInWorld2.getPosition(), Blocks.AIR);
            }
        } else {
            blockPatternMatch = this.getIronGolemShape().find(world, pos);
            if (blockPatternMatch != null) {
                for(int k = 0; k < this.getIronGolemShape().getWidth(); ++k) {
                    for(int l = 0; l < this.getIronGolemShape().getHeight(); ++l) {
                        ShapeDetectorBlock blockInWorld3 = blockPatternMatch.getBlock(k, l, 0);
                        world.setTypeAndData(blockInWorld3.getPosition(), Blocks.AIR.getBlockData(), 2);
                        world.triggerEffect(2001, blockInWorld3.getPosition(), Block.getCombinedId(blockInWorld3.getState()));
                    }
                }

                BlockPosition blockPos2 = blockPatternMatch.getBlock(1, 2, 0).getPosition();
                EntityIronGolem ironGolem = EntityTypes.IRON_GOLEM.create(world);
                ironGolem.setPlayerCreated(true);
                ironGolem.setPositionRotation((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.05D, (double)blockPos2.getZ() + 0.5D, 0.0F, 0.0F);
                world.addEntity(ironGolem);

                for(EntityPlayer serverPlayer2 : world.getEntitiesOfClass(EntityPlayer.class, ironGolem.getBoundingBox().inflate(5.0D))) {
                    CriterionTriggers.SUMMONED_ENTITY.trigger(serverPlayer2, ironGolem);
                }

                for(int m = 0; m < this.getIronGolemShape().getWidth(); ++m) {
                    for(int n = 0; n < this.getIronGolemShape().getHeight(); ++n) {
                        ShapeDetectorBlock blockInWorld4 = blockPatternMatch.getBlock(m, n, 0);
                        world.update(blockInWorld4.getPosition(), Blocks.AIR);
                    }
                }
            }
        }

    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }

    private ShapeDetector getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = ShapeDetectorBuilder.start().aisle(" ", "#", "#").where('#', ShapeDetectorBlock.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }

        return this.snowGolemBase;
    }

    private ShapeDetector getSnowmanShape() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = ShapeDetectorBuilder.start().aisle("^", "#", "#").where('^', ShapeDetectorBlock.hasState(PUMPKINS_PREDICATE)).where('#', ShapeDetectorBlock.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }

        return this.snowGolemFull;
    }

    private ShapeDetector getOrCreateIronGolemBase() {
        if (this.ironGolemBase == null) {
            this.ironGolemBase = ShapeDetectorBuilder.start().aisle("~ ~", "###", "~#~").where('#', ShapeDetectorBlock.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', ShapeDetectorBlock.hasState(MaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return this.ironGolemBase;
    }

    private ShapeDetector getIronGolemShape() {
        if (this.ironGolemFull == null) {
            this.ironGolemFull = ShapeDetectorBuilder.start().aisle("~^~", "###", "~#~").where('^', ShapeDetectorBlock.hasState(PUMPKINS_PREDICATE)).where('#', ShapeDetectorBlock.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', ShapeDetectorBlock.hasState(MaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return this.ironGolemFull;
    }
}
