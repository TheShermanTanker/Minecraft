package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.crafting.RecipeCampfire;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCampfire;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockCampfire extends BlockTileEntity implements IBlockWaterlogged {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    public static final BlockStateBoolean LIT = BlockProperties.LIT;
    public static final BlockStateBoolean SIGNAL_FIRE = BlockProperties.SIGNAL_FIRE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateDirection FACING = BlockProperties.HORIZONTAL_FACING;
    private static final VoxelShape VIRTUAL_FENCE_POST = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final int SMOKE_DISTANCE = 5;
    private final boolean spawnParticles;
    private final int fireDamage;

    public BlockCampfire(boolean emitsParticles, int fireDamage, BlockBase.Info settings) {
        super(settings);
        this.spawnParticles = emitsParticles;
        this.fireDamage = fireDamage;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LIT, Boolean.valueOf(true)).set(SIGNAL_FIRE, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)).set(FACING, EnumDirection.NORTH));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityCampfire) {
            TileEntityCampfire campfireBlockEntity = (TileEntityCampfire)blockEntity;
            ItemStack itemStack = player.getItemInHand(hand);
            Optional<RecipeCampfire> optional = campfireBlockEntity.getCookableRecipe(itemStack);
            if (optional.isPresent()) {
                if (!world.isClientSide && campfireBlockEntity.placeFood(player.getAbilities().instabuild ? itemStack.cloneItemStack() : itemStack, optional.get().getCookingTime())) {
                    player.awardStat(StatisticList.INTERACT_WITH_CAMPFIRE);
                    return EnumInteractionResult.SUCCESS;
                }

                return EnumInteractionResult.CONSUME;
            }
        }

        return EnumInteractionResult.PASS;
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!entity.isFireProof() && state.get(LIT) && entity instanceof EntityLiving && !EnchantmentManager.hasFrostWalker((EntityLiving)entity)) {
            entity.damageEntity(DamageSource.IN_FIRE, (float)this.fireDamage);
        }

        super.entityInside(state, world, pos, entity);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityCampfire) {
                InventoryUtils.dropContents(world, pos, ((TileEntityCampfire)blockEntity).getItems());
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        GeneratorAccess levelAccessor = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        boolean bl = levelAccessor.getFluid(blockPos).getType() == FluidTypes.WATER;
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(bl)).set(SIGNAL_FIRE, Boolean.valueOf(this.isSmokeSource(levelAccessor.getType(blockPos.below())))).set(LIT, Boolean.valueOf(!bl)).set(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction == EnumDirection.DOWN ? state.set(SIGNAL_FIRE, Boolean.valueOf(this.isSmokeSource(neighborState))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    private boolean isSmokeSource(IBlockData state) {
        return state.is(Blocks.HAY_BLOCK);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            if (random.nextInt(10) == 0) {
                world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.CAMPFIRE_CRACKLE, EnumSoundCategory.BLOCKS, 0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
            }

            if (this.spawnParticles && random.nextInt(5) == 0) {
                for(int i = 0; i < random.nextInt(1) + 1; ++i) {
                    world.addParticle(Particles.LAVA, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double)(random.nextFloat() / 2.0F), 5.0E-5D, (double)(random.nextFloat() / 2.0F));
                }
            }

        }
    }

    public static void dowse(@Nullable Entity entity, GeneratorAccess world, BlockPosition pos, IBlockData state) {
        if (world.isClientSide()) {
            for(int i = 0; i < 20; ++i) {
                makeParticles((World)world, pos, state.get(SIGNAL_FIRE), true);
            }
        }

        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityCampfire) {
            ((TileEntityCampfire)blockEntity).dowse();
        }

        world.gameEvent(entity, GameEvent.BLOCK_CHANGE, pos);
    }

    @Override
    public boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        if (!state.get(BlockProperties.WATERLOGGED) && fluidState.getType() == FluidTypes.WATER) {
            boolean bl = state.get(LIT);
            if (bl) {
                if (!world.isClientSide()) {
                    world.playSound((EntityHuman)null, pos, SoundEffects.GENERIC_EXTINGUISH_FIRE, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                }

                dowse((Entity)null, world, pos, state);
            }

            world.setTypeAndData(pos, state.set(WATERLOGGED, Boolean.valueOf(true)).set(LIT, Boolean.valueOf(false)), 3);
            world.getFluidTickList().scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(world));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        BlockPosition blockPos = hit.getBlockPosition();
        if (!world.isClientSide && projectile.isBurning() && projectile.mayInteract(world, blockPos) && !state.get(LIT) && !state.get(WATERLOGGED)) {
            world.setTypeAndData(blockPos, state.set(BlockProperties.LIT, Boolean.valueOf(true)), 11);
        }

    }

    public static void makeParticles(World world, BlockPosition pos, boolean isSignal, boolean lotsOfSmoke) {
        Random random = world.getRandom();
        ParticleType simpleParticleType = isSignal ? Particles.CAMPFIRE_SIGNAL_SMOKE : Particles.CAMPFIRE_COSY_SMOKE;
        world.addAlwaysVisibleParticle(simpleParticleType, true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
        if (lotsOfSmoke) {
            world.addParticle(Particles.SMOKE, (double)pos.getX() + 0.5D + random.nextDouble() / 4.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + 0.4D, (double)pos.getZ() + 0.5D + random.nextDouble() / 4.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);
        }

    }

    public static boolean isSmokeyPos(World world, BlockPosition pos) {
        for(int i = 1; i <= 5; ++i) {
            BlockPosition blockPos = pos.below(i);
            IBlockData blockState = world.getType(blockPos);
            if (isLitCampfire(blockState)) {
                return true;
            }

            boolean bl = VoxelShapes.joinIsNotEmpty(VIRTUAL_FENCE_POST, blockState.getCollisionShape(world, pos, VoxelShapeCollision.empty()), OperatorBoolean.AND);
            if (bl) {
                IBlockData blockState2 = world.getType(blockPos.below());
                return isLitCampfire(blockState2);
            }
        }

        return false;
    }

    public static boolean isLitCampfire(IBlockData state) {
        return state.hasProperty(LIT) && state.is(TagsBlock.CAMPFIRES) && state.get(LIT);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityCampfire(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        if (world.isClientSide) {
            return state.get(LIT) ? createTickerHelper(type, TileEntityTypes.CAMPFIRE, TileEntityCampfire::particleTick) : null;
        } else {
            return state.get(LIT) ? createTickerHelper(type, TileEntityTypes.CAMPFIRE, TileEntityCampfire::cookTick) : createTickerHelper(type, TileEntityTypes.CAMPFIRE, TileEntityCampfire::cooldownTick);
        }
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    public static boolean canLight(IBlockData state) {
        return state.is(TagsBlock.CAMPFIRES, (statex) -> {
            return statex.hasProperty(WATERLOGGED) && statex.hasProperty(LIT);
        }) && !state.get(WATERLOGGED) && !state.get(LIT);
    }
}
