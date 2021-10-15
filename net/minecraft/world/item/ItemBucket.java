package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IFluidContainer;
import net.minecraft.world.level.block.IFluidSource;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypeFlowing;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class ItemBucket extends Item implements DispensibleContainerItem {
    public final FluidType content;

    public ItemBucket(FluidType fluid, Item.Info settings) {
        super(settings);
        this.content = fluid;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        MovingObjectPositionBlock blockHitResult = getPlayerPOVHitResult(world, user, this.content == FluidTypes.EMPTY ? RayTrace.FluidCollisionOption.SOURCE_ONLY : RayTrace.FluidCollisionOption.NONE);
        if (blockHitResult.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
            return InteractionResultWrapper.pass(itemStack);
        } else if (blockHitResult.getType() != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            return InteractionResultWrapper.pass(itemStack);
        } else {
            BlockPosition blockPos = blockHitResult.getBlockPosition();
            EnumDirection direction = blockHitResult.getDirection();
            BlockPosition blockPos2 = blockPos.relative(direction);
            if (world.mayInteract(user, blockPos) && user.mayUseItemAt(blockPos2, direction, itemStack)) {
                if (this.content == FluidTypes.EMPTY) {
                    IBlockData blockState = world.getType(blockPos);
                    if (blockState.getBlock() instanceof IFluidSource) {
                        IFluidSource bucketPickup = (IFluidSource)blockState.getBlock();
                        ItemStack itemStack2 = bucketPickup.removeFluid(world, blockPos, blockState);
                        if (!itemStack2.isEmpty()) {
                            user.awardStat(StatisticList.ITEM_USED.get(this));
                            bucketPickup.getPickupSound().ifPresent((sound) -> {
                                user.playSound(sound, 1.0F, 1.0F);
                            });
                            world.gameEvent(user, GameEvent.FLUID_PICKUP, blockPos);
                            ItemStack itemStack3 = ItemLiquidUtil.createFilledResult(itemStack, user, itemStack2);
                            if (!world.isClientSide) {
                                CriterionTriggers.FILLED_BUCKET.trigger((EntityPlayer)user, itemStack2);
                            }

                            return InteractionResultWrapper.sidedSuccess(itemStack3, world.isClientSide());
                        }
                    }

                    return InteractionResultWrapper.fail(itemStack);
                } else {
                    IBlockData blockState2 = world.getType(blockPos);
                    BlockPosition blockPos3 = blockState2.getBlock() instanceof IFluidContainer && this.content == FluidTypes.WATER ? blockPos : blockPos2;
                    if (this.emptyContents(user, world, blockPos3, blockHitResult)) {
                        this.checkExtraContent(user, world, itemStack, blockPos3);
                        if (user instanceof EntityPlayer) {
                            CriterionTriggers.PLACED_BLOCK.trigger((EntityPlayer)user, blockPos3, itemStack);
                        }

                        user.awardStat(StatisticList.ITEM_USED.get(this));
                        return InteractionResultWrapper.sidedSuccess(getEmptySuccessItem(itemStack, user), world.isClientSide());
                    } else {
                        return InteractionResultWrapper.fail(itemStack);
                    }
                }
            } else {
                return InteractionResultWrapper.fail(itemStack);
            }
        }
    }

    public static ItemStack getEmptySuccessItem(ItemStack stack, EntityHuman player) {
        return !player.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : stack;
    }

    @Override
    public void checkExtraContent(@Nullable EntityHuman player, World world, ItemStack stack, BlockPosition pos) {
    }

    @Override
    public boolean emptyContents(@Nullable EntityHuman player, World world, BlockPosition pos, @Nullable MovingObjectPositionBlock hitResult) {
        if (!(this.content instanceof FluidTypeFlowing)) {
            return false;
        } else {
            IBlockData blockState = world.getType(pos);
            Block block = blockState.getBlock();
            Material material = blockState.getMaterial();
            boolean bl = blockState.canBeReplaced(this.content);
            boolean bl2 = blockState.isAir() || bl || block instanceof IFluidContainer && ((IFluidContainer)block).canPlace(world, pos, blockState, this.content);
            if (!bl2) {
                return hitResult != null && this.emptyContents(player, world, hitResult.getBlockPosition().relative(hitResult.getDirection()), (MovingObjectPositionBlock)null);
            } else if (world.getDimensionManager().isNether() && this.content.is(TagsFluid.WATER)) {
                int i = pos.getX();
                int j = pos.getY();
                int k = pos.getZ();
                world.playSound(player, pos, SoundEffects.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                for(int l = 0; l < 8; ++l) {
                    world.addParticle(Particles.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return true;
            } else if (block instanceof IFluidContainer && this.content == FluidTypes.WATER) {
                ((IFluidContainer)block).place(world, pos, blockState, ((FluidTypeFlowing)this.content).getSource(false));
                this.playEmptySound(player, world, pos);
                return true;
            } else {
                if (!world.isClientSide && bl && !material.isLiquid()) {
                    world.destroyBlock(pos, true);
                }

                if (!world.setTypeAndData(pos, this.content.defaultFluidState().getBlockData(), 11) && !blockState.getFluid().isSource()) {
                    return false;
                } else {
                    this.playEmptySound(player, world, pos);
                    return true;
                }
            }
        }
    }

    protected void playEmptySound(@Nullable EntityHuman player, GeneratorAccess world, BlockPosition pos) {
        SoundEffect soundEvent = this.content.is(TagsFluid.LAVA) ? SoundEffects.BUCKET_EMPTY_LAVA : SoundEffects.BUCKET_EMPTY;
        world.playSound(player, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
