package net.minecraft.world.item;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityEnderSignal;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockEnderPortalFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetector;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class ItemEnderEye extends Item {
    public ItemEnderEye(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (blockState.is(Blocks.END_PORTAL_FRAME) && !blockState.get(BlockEnderPortalFrame.HAS_EYE)) {
            if (level.isClientSide) {
                return EnumInteractionResult.SUCCESS;
            } else {
                IBlockData blockState2 = blockState.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(true));
                Block.pushEntitiesUp(blockState, blockState2, level, blockPos);
                level.setTypeAndData(blockPos, blockState2, 2);
                level.updateAdjacentComparators(blockPos, Blocks.END_PORTAL_FRAME);
                context.getItemStack().subtract(1);
                level.triggerEffect(1503, blockPos, 0);
                ShapeDetector.ShapeDetectorCollection blockPatternMatch = BlockEnderPortalFrame.getOrCreatePortalShape().find(level, blockPos);
                if (blockPatternMatch != null) {
                    BlockPosition blockPos2 = blockPatternMatch.getFrontTopLeft().offset(-3, 0, -3);

                    for(int i = 0; i < 3; ++i) {
                        for(int j = 0; j < 3; ++j) {
                            level.setTypeAndData(blockPos2.offset(i, 0, j), Blocks.END_PORTAL.getBlockData(), 2);
                        }
                    }

                    level.broadcastWorldEvent(1038, blockPos2.offset(1, 0, 1), 0);
                }

                return EnumInteractionResult.CONSUME;
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        MovingObjectPosition hitResult = getPlayerPOVHitResult(world, user, RayTrace.FluidCollisionOption.NONE);
        if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK && world.getType(((MovingObjectPositionBlock)hitResult).getBlockPosition()).is(Blocks.END_PORTAL_FRAME)) {
            return InteractionResultWrapper.pass(itemStack);
        } else {
            user.startUsingItem(hand);
            if (world instanceof WorldServer) {
                BlockPosition blockPos = ((WorldServer)world).getChunkSource().getChunkGenerator().findNearestMapFeature((WorldServer)world, StructureGenerator.STRONGHOLD, user.getChunkCoordinates(), 100, false);
                if (blockPos != null) {
                    EntityEnderSignal eyeOfEnder = new EntityEnderSignal(world, user.locX(), user.getY(0.5D), user.locZ());
                    eyeOfEnder.setItem(itemStack);
                    eyeOfEnder.signalTo(blockPos);
                    world.addEntity(eyeOfEnder);
                    if (user instanceof EntityPlayer) {
                        CriterionTriggers.USED_ENDER_EYE.trigger((EntityPlayer)user, blockPos);
                    }

                    world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.ENDER_EYE_LAUNCH, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
                    world.triggerEffect((EntityHuman)null, 1003, user.getChunkCoordinates(), 0);
                    if (!user.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }

                    user.awardStat(StatisticList.ITEM_USED.get(this));
                    user.swingHand(hand, true);
                    return InteractionResultWrapper.success(itemStack);
                }
            }

            return InteractionResultWrapper.consume(itemStack);
        }
    }
}
