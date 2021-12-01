package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.LivingEntity$Fallsounds;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapeCollisionEntity;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockPowderSnow extends Block implements IFluidSource {
    private static final float HORIZONTAL_PARTICLE_MOMENTUM_FACTOR = 0.083333336F;
    private static final float IN_BLOCK_HORIZONTAL_SPEED_MULTIPLIER = 0.9F;
    private static final float IN_BLOCK_VERTICAL_SPEED_MULTIPLIER = 1.5F;
    private static final float NUM_BLOCKS_TO_FALL_INTO_BLOCK = 2.5F;
    private static final VoxelShape FALLING_COLLISION_SHAPE = VoxelShapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)0.9F, 1.0D);
    private static final double MINIMUM_FALL_DISTANCE_FOR_SOUND = 4.0D;
    private static final double MINIMUM_FALL_DISTANCE_FOR_BIG_SOUND = 7.0D;

    public BlockPowderSnow(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean skipRendering(IBlockData state, IBlockData stateFrom, EnumDirection direction) {
        return stateFrom.is(this) ? true : super.skipRendering(state, stateFrom, direction);
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!(entity instanceof EntityLiving) || entity.getFeetBlockState().is(this)) {
            entity.makeStuckInBlock(state, new Vec3D((double)0.9F, 1.5D, (double)0.9F));
            if (world.isClientSide) {
                Random random = world.getRandom();
                boolean bl = entity.xOld != entity.locX() || entity.zOld != entity.locZ();
                if (bl && random.nextBoolean()) {
                    world.addParticle(Particles.SNOWFLAKE, entity.locX(), (double)(pos.getY() + 1), entity.locZ(), (double)(MathHelper.randomBetween(random, -1.0F, 1.0F) * 0.083333336F), (double)0.05F, (double)(MathHelper.randomBetween(random, -1.0F, 1.0F) * 0.083333336F));
                }
            }
        }

        entity.setIsInPowderSnow(true);
        if (!world.isClientSide) {
            if (entity.isBurning() && (world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || entity instanceof EntityHuman) && entity.mayInteract(world, pos)) {
                world.destroyBlock(pos, false);
            }

            entity.setSharedFlagOnFire(false);
        }

    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        if (!((double)fallDistance < 4.0D) && entity instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)entity;
            LivingEntity$Fallsounds fallsounds = livingEntity.getFallSounds();
            SoundEffect soundEvent = (double)fallDistance < 7.0D ? fallsounds.small() : fallsounds.big();
            entity.playSound(soundEvent, 1.0F, 1.0F);
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (context instanceof VoxelShapeCollisionEntity) {
            VoxelShapeCollisionEntity entityCollisionContext = (VoxelShapeCollisionEntity)context;
            Entity entity = entityCollisionContext.getEntity();
            if (entity != null) {
                if (entity.fallDistance > 2.5F) {
                    return FALLING_COLLISION_SHAPE;
                }

                boolean bl = entity instanceof EntityFallingBlock;
                if (bl || canEntityWalkOnPowderSnow(entity) && context.isAbove(VoxelShapes.block(), pos, false) && !context.isDescending()) {
                    return super.getCollisionShape(state, world, pos, context);
                }
            }
        }

        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.empty();
    }

    public static boolean canEntityWalkOnPowderSnow(Entity entity) {
        if (entity.getEntityType().is(TagsEntity.POWDER_SNOW_WALKABLE_MOBS)) {
            return true;
        } else {
            return entity instanceof EntityLiving ? ((EntityLiving)entity).getEquipment(EnumItemSlot.FEET).is(Items.LEATHER_BOOTS) : false;
        }
    }

    @Override
    public ItemStack removeFluid(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 11);
        if (!world.isClientSide()) {
            world.triggerEffect(2001, pos, Block.getCombinedId(state));
        }

        return new ItemStack(Items.POWDER_SNOW_BUCKET);
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return Optional.of(SoundEffects.BUCKET_FILL_POWDER_SNOW);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return true;
    }
}
