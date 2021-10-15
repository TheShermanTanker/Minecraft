package net.minecraft.world.item;

import java.util.stream.Stream;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemLiquidUtil {
    public static InteractionResultWrapper<ItemStack> startUsingInstantly(World world, EntityHuman player, EnumHand hand) {
        player.startUsingItem(hand);
        return InteractionResultWrapper.consume(player.getItemInHand(hand));
    }

    public static ItemStack createFilledResult(ItemStack inputStack, EntityHuman player, ItemStack outputStack, boolean creativeOverride) {
        boolean bl = player.getAbilities().instabuild;
        if (creativeOverride && bl) {
            if (!player.getInventory().contains(outputStack)) {
                player.getInventory().pickup(outputStack);
            }

            return inputStack;
        } else {
            if (!bl) {
                inputStack.subtract(1);
            }

            if (inputStack.isEmpty()) {
                return outputStack;
            } else {
                if (!player.getInventory().pickup(outputStack)) {
                    player.drop(outputStack, false);
                }

                return inputStack;
            }
        }
    }

    public static ItemStack createFilledResult(ItemStack inputStack, EntityHuman player, ItemStack outputStack) {
        return createFilledResult(inputStack, player, outputStack, true);
    }

    public static void onContainerDestroyed(EntityItem itemEntity, Stream<ItemStack> contents) {
        World level = itemEntity.level;
        if (!level.isClientSide) {
            contents.forEach((stack) -> {
                level.addEntity(new EntityItem(level, itemEntity.locX(), itemEntity.locY(), itemEntity.locZ(), stack));
            });
        }
    }
}
