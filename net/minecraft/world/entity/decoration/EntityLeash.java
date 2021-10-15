package net.minecraft.world.entity.decoration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityLeash extends EntityHanging {
    public static final double OFFSET_Y = 0.375D;

    public EntityLeash(EntityTypes<? extends EntityLeash> type, World world) {
        super(type, world);
    }

    public EntityLeash(World world, BlockPosition pos) {
        super(EntityTypes.LEASH_KNOT, world, pos);
        this.setPosition((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
    }

    @Override
    protected void updateBoundingBox() {
        this.setPositionRaw((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.375D, (double)this.pos.getZ() + 0.5D);
        double d = (double)this.getEntityType().getWidth() / 2.0D;
        double e = (double)this.getEntityType().getHeight();
        this.setBoundingBox(new AxisAlignedBB(this.locX() - d, this.locY(), this.locZ() - d, this.locX() + d, this.locY() + e, this.locZ() + d));
    }

    @Override
    public void setDirection(EnumDirection facing) {
    }

    @Override
    public int getHangingWidth() {
        return 9;
    }

    @Override
    public int getHangingHeight() {
        return 9;
    }

    @Override
    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return 0.0625F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(SoundEffects.LEASH_KNOT_BREAK, 1.0F, 1.0F);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        if (this.level.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            boolean bl = false;
            double d = 7.0D;
            List<EntityInsentient> list = this.level.getEntitiesOfClass(EntityInsentient.class, new AxisAlignedBB(this.locX() - 7.0D, this.locY() - 7.0D, this.locZ() - 7.0D, this.locX() + 7.0D, this.locY() + 7.0D, this.locZ() + 7.0D));

            for(EntityInsentient mob : list) {
                if (mob.getLeashHolder() == player) {
                    mob.setLeashHolder(this, true);
                    bl = true;
                }
            }

            if (!bl) {
                this.die();
                if (player.getAbilities().instabuild) {
                    for(EntityInsentient mob2 : list) {
                        if (mob2.isLeashed() && mob2.getLeashHolder() == this) {
                            mob2.unleash(true, false);
                        }
                    }
                }
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public boolean survives() {
        return this.level.getType(this.pos).is(TagsBlock.FENCES);
    }

    public static EntityLeash getOrCreateKnot(World world, BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();

        for(EntityLeash leashFenceKnotEntity : world.getEntitiesOfClass(EntityLeash.class, new AxisAlignedBB((double)i - 1.0D, (double)j - 1.0D, (double)k - 1.0D, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D))) {
            if (leashFenceKnotEntity.getBlockPosition().equals(pos)) {
                return leashFenceKnotEntity;
            }
        }

        EntityLeash leashFenceKnotEntity2 = new EntityLeash(world, pos);
        world.addEntity(leashFenceKnotEntity2);
        return leashFenceKnotEntity2;
    }

    @Override
    public void playPlaceSound() {
        this.playSound(SoundEffects.LEASH_KNOT_PLACE, 1.0F, 1.0F);
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this, this.getEntityType(), 0, this.getBlockPosition());
    }

    @Override
    public Vec3D getRopeHoldPosition(float f) {
        return this.getPosition(f).add(0.0D, 0.2D, 0.0D);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.LEAD);
    }
}
