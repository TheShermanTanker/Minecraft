package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class SolidBucketItem extends ItemBlock implements DispensibleContainerItem {
    private final SoundEffect placeSound;

    public SolidBucketItem(Block block, SoundEffect placeSound, Item.Info settings) {
        super(block, settings);
        this.placeSound = placeSound;
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        EnumInteractionResult interactionResult = super.useOn(context);
        EntityHuman player = context.getEntity();
        if (interactionResult.consumesAction() && player != null && !player.isCreative()) {
            EnumHand interactionHand = context.getHand();
            player.setItemInHand(interactionHand, Items.BUCKET.createItemStack());
        }

        return interactionResult;
    }

    @Override
    public String getName() {
        return this.getOrCreateDescriptionId();
    }

    @Override
    protected SoundEffect getPlaceSound(IBlockData state) {
        return this.placeSound;
    }

    @Override
    public boolean emptyContents(@Nullable EntityHuman player, World world, BlockPosition pos, @Nullable MovingObjectPositionBlock hitResult) {
        if (world.isValidLocation(pos) && world.isEmpty(pos)) {
            if (!world.isClientSide) {
                world.setTypeAndData(pos, this.getBlock().getBlockData(), 3);
            }

            world.playSound(player, pos, this.placeSound, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}
