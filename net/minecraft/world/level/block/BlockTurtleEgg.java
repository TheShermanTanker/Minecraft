package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockTurtleEgg extends Block {
    public static final int MAX_HATCH_LEVEL = 2;
    public static final int MIN_EGGS = 1;
    public static final int MAX_EGGS = 4;
    private static final VoxelShape ONE_EGG_AABB = Block.box(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
    private static final VoxelShape MULTIPLE_EGGS_AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    public static final BlockStateInteger HATCH = BlockProperties.HATCH;
    public static final BlockStateInteger EGGS = BlockProperties.EGGS;

    public BlockTurtleEgg(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HATCH, Integer.valueOf(0)).set(EGGS, Integer.valueOf(1)));
    }

    @Override
    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
        this.destroyEgg(world, state, pos, entity, 100);
        super.stepOn(world, pos, state, entity);
    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        if (!(entity instanceof EntityZombie)) {
            this.destroyEgg(world, state, pos, entity, 3);
        }

        super.fallOn(world, state, pos, entity, fallDistance);
    }

    private void destroyEgg(World world, IBlockData state, BlockPosition pos, Entity entity, int inverseChance) {
        if (this.canDestroyEgg(world, entity)) {
            if (!world.isClientSide && world.random.nextInt(inverseChance) == 0 && state.is(Blocks.TURTLE_EGG)) {
                this.decreaseEggs(world, pos, state);
            }

        }
    }

    private void decreaseEggs(World world, BlockPosition pos, IBlockData state) {
        world.playSound((EntityHuman)null, pos, SoundEffects.TURTLE_EGG_BREAK, EnumSoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
        int i = state.get(EGGS);
        if (i <= 1) {
            world.destroyBlock(pos, false);
        } else {
            world.setTypeAndData(pos, state.set(EGGS, Integer.valueOf(i - 1)), 2);
            world.triggerEffect(2001, pos, Block.getCombinedId(state));
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (this.shouldUpdateHatchLevel(world) && onSand(world, pos)) {
            int i = state.get(HATCH);
            if (i < 2) {
                world.playSound((EntityHuman)null, pos, SoundEffects.TURTLE_EGG_CRACK, EnumSoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                world.setTypeAndData(pos, state.set(HATCH, Integer.valueOf(i + 1)), 2);
            } else {
                world.playSound((EntityHuman)null, pos, SoundEffects.TURTLE_EGG_HATCH, EnumSoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                world.removeBlock(pos, false);

                for(int j = 0; j < state.get(EGGS); ++j) {
                    world.triggerEffect(2001, pos, Block.getCombinedId(state));
                    EntityTurtle turtle = EntityTypes.TURTLE.create(world);
                    turtle.setAgeRaw(-24000);
                    turtle.setHomePos(pos);
                    turtle.setPositionRotation((double)pos.getX() + 0.3D + (double)j * 0.2D, (double)pos.getY(), (double)pos.getZ() + 0.3D, 0.0F, 0.0F);
                    world.addEntity(turtle);
                }
            }
        }

    }

    public static boolean onSand(IBlockAccess world, BlockPosition pos) {
        return isSand(world, pos.below());
    }

    public static boolean isSand(IBlockAccess world, BlockPosition pos) {
        return world.getType(pos).is(TagsBlock.SAND);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (onSand(world, pos) && !world.isClientSide) {
            world.triggerEffect(2005, pos, 0);
        }

    }

    private boolean shouldUpdateHatchLevel(World world) {
        float f = world.getTimeOfDay(1.0F);
        if ((double)f < 0.69D && (double)f > 0.65D) {
            return true;
        } else {
            return world.random.nextInt(500) == 0;
        }
    }

    @Override
    public void playerDestroy(World world, EntityHuman player, BlockPosition pos, IBlockData state, @Nullable TileEntity blockEntity, ItemStack stack) {
        super.playerDestroy(world, player, pos, state, blockEntity, stack);
        this.decreaseEggs(world, pos, state);
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return !context.isSneaking() && context.getItemStack().is(this.getItem()) && state.get(EGGS) < 4 ? true : super.canBeReplaced(state, context);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition());
        return blockState.is(this) ? blockState.set(EGGS, Integer.valueOf(Math.min(4, blockState.get(EGGS) + 1))) : super.getPlacedState(ctx);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return state.get(EGGS) > 1 ? MULTIPLE_EGGS_AABB : ONE_EGG_AABB;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HATCH, EGGS);
    }

    private boolean canDestroyEgg(World world, Entity entity) {
        if (!(entity instanceof EntityTurtle) && !(entity instanceof EntityBat)) {
            if (!(entity instanceof EntityLiving)) {
                return false;
            } else {
                return entity instanceof EntityHuman || world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            }
        } else {
            return false;
        }
    }
}
