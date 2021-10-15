package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSweetBerryBush extends BlockPlant implements IBlockFragilePlantElement {
    private static final float HURT_SPEED_THRESHOLD = 0.003F;
    public static final int MAX_AGE = 3;
    public static final BlockStateInteger AGE = BlockProperties.AGE_3;
    private static final VoxelShape SAPLING_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
    private static final VoxelShape MID_GROWTH_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public BlockSweetBerryBush(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Items.SWEET_BERRIES);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (state.get(AGE) == 0) {
            return SAPLING_SHAPE;
        } else {
            return state.get(AGE) < 3 ? MID_GROWTH_SHAPE : super.getShape(state, world, pos, context);
        }
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(AGE) < 3;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        int i = state.get(AGE);
        if (i < 3 && random.nextInt(5) == 0 && world.getLightLevel(pos.above(), 0) >= 9) {
            world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(i + 1)), 2);
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (entity instanceof EntityLiving && entity.getEntityType() != EntityTypes.FOX && entity.getEntityType() != EntityTypes.BEE) {
            entity.makeStuckInBlock(state, new Vec3D((double)0.8F, 0.75D, (double)0.8F));
            if (!world.isClientSide && state.get(AGE) > 0 && (entity.xOld != entity.locX() || entity.zOld != entity.locZ())) {
                double d = Math.abs(entity.locX() - entity.xOld);
                double e = Math.abs(entity.locZ() - entity.zOld);
                if (d >= (double)0.003F || e >= (double)0.003F) {
                    entity.damageEntity(DamageSource.SWEET_BERRY_BUSH, 1.0F);
                }
            }

        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        int i = state.get(AGE);
        boolean bl = i == 3;
        if (!bl && player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            return EnumInteractionResult.PASS;
        } else if (i > 1) {
            int j = 1 + world.random.nextInt(2);
            popResource(world, pos, new ItemStack(Items.SWEET_BERRIES, j + (bl ? 1 : 0)));
            world.playSound((EntityHuman)null, pos, SoundEffects.SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
            world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(1)), 2);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return super.interact(state, world, pos, player, hand, hit);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return state.get(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        int i = Math.min(3, state.get(AGE) + 1);
        world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(i)), 2);
    }
}
