package net.minecraft.world.item.context;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class BlockActionContext extends ItemActionContext {
    private final BlockPosition relativePos;
    protected boolean replaceClicked = true;

    public BlockActionContext(EntityHuman player, EnumHand hand, ItemStack stack, MovingObjectPositionBlock hitResult) {
        this(player.level, player, hand, stack, hitResult);
    }

    public BlockActionContext(ItemActionContext context) {
        this(context.getWorld(), context.getEntity(), context.getHand(), context.getItemStack(), context.getHitResult());
    }

    public BlockActionContext(World world, @Nullable EntityHuman player, EnumHand hand, ItemStack stack, MovingObjectPositionBlock hit) {
        super(world, player, hand, stack, hit);
        this.relativePos = hit.getBlockPosition().relative(hit.getDirection());
        this.replaceClicked = world.getType(hit.getBlockPosition()).canBeReplaced(this);
    }

    public static BlockActionContext at(BlockActionContext context, BlockPosition pos, EnumDirection side) {
        return new BlockActionContext(context.getWorld(), context.getEntity(), context.getHand(), context.getItemStack(), new MovingObjectPositionBlock(new Vec3D((double)pos.getX() + 0.5D + (double)side.getAdjacentX() * 0.5D, (double)pos.getY() + 0.5D + (double)side.getAdjacentY() * 0.5D, (double)pos.getZ() + 0.5D + (double)side.getAdjacentZ() * 0.5D), side, pos, false));
    }

    @Override
    public BlockPosition getClickPosition() {
        return this.replaceClicked ? super.getClickPosition() : this.relativePos;
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getWorld().getType(this.getClickPosition()).canBeReplaced(this);
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public EnumDirection getNearestLookingDirection() {
        return EnumDirection.orderedByNearest(this.getEntity())[0];
    }

    public EnumDirection getNearestLookingVerticalDirection() {
        return EnumDirection.getFacingAxis(this.getEntity(), EnumDirection.EnumAxis.Y);
    }

    public EnumDirection[] getNearestLookingDirections() {
        EnumDirection[] directions = EnumDirection.orderedByNearest(this.getEntity());
        if (this.replaceClicked) {
            return directions;
        } else {
            EnumDirection direction = this.getClickedFace();

            int i;
            for(i = 0; i < directions.length && directions[i] != direction.opposite(); ++i) {
            }

            if (i > 0) {
                System.arraycopy(directions, 0, directions, 1, i);
                directions[0] = direction.opposite();
            }

            return directions;
        }
    }
}
