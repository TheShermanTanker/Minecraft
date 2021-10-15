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

public class ItemActionContext {
    @Nullable
    private final EntityHuman player;
    private final EnumHand hand;
    private final MovingObjectPositionBlock hitResult;
    private final World level;
    private final ItemStack itemStack;

    public ItemActionContext(EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        this(player.level, player, hand, player.getItemInHand(hand), hit);
    }

    public ItemActionContext(World world, @Nullable EntityHuman player, EnumHand hand, ItemStack stack, MovingObjectPositionBlock hit) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hit;
        this.itemStack = stack;
        this.level = world;
    }

    protected final MovingObjectPositionBlock getHitResult() {
        return this.hitResult;
    }

    public BlockPosition getClickPosition() {
        return this.hitResult.getBlockPosition();
    }

    public EnumDirection getClickedFace() {
        return this.hitResult.getDirection();
    }

    public Vec3D getPos() {
        return this.hitResult.getPos();
    }

    public boolean isInside() {
        return this.hitResult.isInside();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Nullable
    public EntityHuman getEntity() {
        return this.player;
    }

    public EnumHand getHand() {
        return this.hand;
    }

    public World getWorld() {
        return this.level;
    }

    public EnumDirection getHorizontalDirection() {
        return this.player == null ? EnumDirection.NORTH : this.player.getDirection();
    }

    public boolean isSneaking() {
        return this.player != null && this.player.isSecondaryUseActive();
    }

    public float getRotation() {
        return this.player == null ? 0.0F : this.player.getYRot();
    }
}
