package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class ItemGlassBottle extends Item {
    public ItemGlassBottle(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        List<EntityAreaEffectCloud> list = world.getEntitiesOfClass(EntityAreaEffectCloud.class, user.getBoundingBox().inflate(2.0D), (entity) -> {
            return entity != null && entity.isAlive() && entity.getSource() instanceof EntityEnderDragon;
        });
        ItemStack itemStack = user.getItemInHand(hand);
        if (!list.isEmpty()) {
            EntityAreaEffectCloud areaEffectCloud = list.get(0);
            areaEffectCloud.setRadius(areaEffectCloud.getRadius() - 0.5F);
            world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.BOTTLE_FILL_DRAGONBREATH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            world.gameEvent(user, GameEvent.FLUID_PICKUP, user.getChunkCoordinates());
            return InteractionResultWrapper.sidedSuccess(this.turnBottleIntoItem(itemStack, user, new ItemStack(Items.DRAGON_BREATH)), world.isClientSide());
        } else {
            MovingObjectPosition hitResult = getPlayerPOVHitResult(world, user, RayTrace.FluidCollisionOption.SOURCE_ONLY);
            if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
                return InteractionResultWrapper.pass(itemStack);
            } else {
                if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                    BlockPosition blockPos = ((MovingObjectPositionBlock)hitResult).getBlockPosition();
                    if (!world.mayInteract(user, blockPos)) {
                        return InteractionResultWrapper.pass(itemStack);
                    }

                    if (world.getFluid(blockPos).is(TagsFluid.WATER)) {
                        world.playSound(user, user.locX(), user.locY(), user.locZ(), SoundEffects.BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                        world.gameEvent(user, GameEvent.FLUID_PICKUP, blockPos);
                        return InteractionResultWrapper.sidedSuccess(this.turnBottleIntoItem(itemStack, user, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER)), world.isClientSide());
                    }
                }

                return InteractionResultWrapper.pass(itemStack);
            }
        }
    }

    protected ItemStack turnBottleIntoItem(ItemStack stack, EntityHuman player, ItemStack outputStack) {
        player.awardStat(StatisticList.ITEM_USED.get(this));
        return ItemLiquidUtil.createFilledResult(stack, player, outputStack);
    }
}
