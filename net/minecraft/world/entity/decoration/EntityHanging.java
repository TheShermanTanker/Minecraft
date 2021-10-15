package net.minecraft.world.entity.decoration;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDiodeAbstract;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;

public abstract class EntityHanging extends Entity {
    protected static final Predicate<Entity> HANGING_ENTITY = (entity) -> {
        return entity instanceof EntityHanging;
    };
    private int checkInterval;
    public BlockPosition pos;
    protected EnumDirection direction = EnumDirection.SOUTH;

    protected EntityHanging(EntityTypes<? extends EntityHanging> type, World world) {
        super(type, world);
    }

    protected EntityHanging(EntityTypes<? extends EntityHanging> type, World world, BlockPosition pos) {
        this(type, world);
        this.pos = pos;
    }

    @Override
    protected void initDatawatcher() {
    }

    public void setDirection(EnumDirection facing) {
        Validate.notNull(facing);
        Validate.isTrue(facing.getAxis().isHorizontal());
        this.direction = facing;
        this.setYRot((float)(this.direction.get2DRotationValue() * 90));
        this.yRotO = this.getYRot();
        this.updateBoundingBox();
    }

    protected void updateBoundingBox() {
        if (this.direction != null) {
            double d = (double)this.pos.getX() + 0.5D;
            double e = (double)this.pos.getY() + 0.5D;
            double f = (double)this.pos.getZ() + 0.5D;
            double g = 0.46875D;
            double h = this.offs(this.getHangingWidth());
            double i = this.offs(this.getHangingHeight());
            d = d - (double)this.direction.getAdjacentX() * 0.46875D;
            f = f - (double)this.direction.getAdjacentZ() * 0.46875D;
            e = e + i;
            EnumDirection direction = this.direction.getCounterClockWise();
            d = d + h * (double)direction.getAdjacentX();
            f = f + h * (double)direction.getAdjacentZ();
            this.setPositionRaw(d, e, f);
            double j = (double)this.getHangingWidth();
            double k = (double)this.getHangingHeight();
            double l = (double)this.getHangingWidth();
            if (this.direction.getAxis() == EnumDirection.EnumAxis.Z) {
                l = 1.0D;
            } else {
                j = 1.0D;
            }

            j = j / 32.0D;
            k = k / 32.0D;
            l = l / 32.0D;
            this.setBoundingBox(new AxisAlignedBB(d - j, e - k, f - l, d + j, e + k, f + l));
        }
    }

    private double offs(int i) {
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            this.checkOutOfWorld();
            if (this.checkInterval++ == 100) {
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    this.die();
                    this.dropItem((Entity)null);
                }
            }
        }

    }

    public boolean survives() {
        if (!this.level.getCubes(this)) {
            return false;
        } else {
            int i = Math.max(1, this.getHangingWidth() / 16);
            int j = Math.max(1, this.getHangingHeight() / 16);
            BlockPosition blockPos = this.pos.relative(this.direction.opposite());
            EnumDirection direction = this.direction.getCounterClockWise();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int k = 0; k < i; ++k) {
                for(int l = 0; l < j; ++l) {
                    int m = (i - 1) / -2;
                    int n = (j - 1) / -2;
                    mutableBlockPos.set(blockPos).move(direction, k + m).move(EnumDirection.UP, l + n);
                    IBlockData blockState = this.level.getType(mutableBlockPos);
                    if (!blockState.getMaterial().isBuildable() && !BlockDiodeAbstract.isDiode(blockState)) {
                        return false;
                    }
                }
            }

            return this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof EntityHuman) {
            EntityHuman player = (EntityHuman)attacker;
            return !this.level.mayInteract(player, this.pos) ? true : this.damageEntity(DamageSource.playerAttack(player), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public EnumDirection getDirection() {
        return this.direction;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level.isClientSide) {
                this.killEntity();
                this.velocityChanged();
                this.dropItem(source.getEntity());
            }

            return true;
        }
    }

    @Override
    public void move(EnumMoveType movementType, Vec3D movement) {
        if (!this.level.isClientSide && !this.isRemoved() && movement.lengthSqr() > 0.0D) {
            this.killEntity();
            this.dropItem((Entity)null);
        }

    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (!this.level.isClientSide && !this.isRemoved() && deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0D) {
            this.killEntity();
            this.dropItem((Entity)null);
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        BlockPosition blockPos = this.getBlockPosition();
        nbt.setInt("TileX", blockPos.getX());
        nbt.setInt("TileY", blockPos.getY());
        nbt.setInt("TileZ", blockPos.getZ());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.pos = new BlockPosition(nbt.getInt("TileX"), nbt.getInt("TileY"), nbt.getInt("TileZ"));
    }

    public abstract int getHangingWidth();

    public abstract int getHangingHeight();

    public abstract void dropItem(@Nullable Entity entity);

    public abstract void playPlaceSound();

    @Override
    public EntityItem spawnAtLocation(ItemStack stack, float yOffset) {
        EntityItem itemEntity = new EntityItem(this.level, this.locX() + (double)((float)this.direction.getAdjacentX() * 0.15F), this.locY() + (double)yOffset, this.locZ() + (double)((float)this.direction.getAdjacentZ() * 0.15F), stack);
        itemEntity.defaultPickupDelay();
        this.level.addEntity(itemEntity);
        return itemEntity;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.pos = new BlockPosition(x, y, z);
        this.updateBoundingBox();
        this.hasImpulse = true;
    }

    public BlockPosition getBlockPosition() {
        return this.pos;
    }

    @Override
    public float rotate(EnumBlockRotation rotation) {
        if (this.direction.getAxis() != EnumDirection.EnumAxis.Y) {
            switch(rotation) {
            case CLOCKWISE_180:
                this.direction = this.direction.opposite();
                break;
            case COUNTERCLOCKWISE_90:
                this.direction = this.direction.getCounterClockWise();
                break;
            case CLOCKWISE_90:
                this.direction = this.direction.getClockWise();
            }
        }

        float f = MathHelper.wrapDegrees(this.getYRot());
        switch(rotation) {
        case CLOCKWISE_180:
            return f + 180.0F;
        case COUNTERCLOCKWISE_90:
            return f + 90.0F;
        case CLOCKWISE_90:
            return f + 270.0F;
        default:
            return f;
        }
    }

    @Override
    public float mirror(EnumBlockMirror mirror) {
        return this.rotate(mirror.getRotation(this.direction));
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
    }

    @Override
    public void updateSize() {
    }
}
