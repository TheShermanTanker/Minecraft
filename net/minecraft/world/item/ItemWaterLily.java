package net.minecraft.world.item;

import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class ItemWaterLily extends ItemBlock {
    public ItemWaterLily(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        return EnumInteractionResult.PASS;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        MovingObjectPositionBlock blockHitResult = getPlayerPOVHitResult(world, user, RayTrace.FluidCollisionOption.SOURCE_ONLY);
        MovingObjectPositionBlock blockHitResult2 = blockHitResult.withPosition(blockHitResult.getBlockPosition().above());
        EnumInteractionResult interactionResult = super.useOn(new ItemActionContext(user, hand, blockHitResult2));
        return new InteractionResultWrapper<>(interactionResult, user.getItemInHand(hand));
    }
}
