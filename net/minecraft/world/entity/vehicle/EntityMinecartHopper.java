package net.minecraft.world.entity.vehicle;

import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerHopper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.IHopper;
import net.minecraft.world.level.block.entity.TileEntityHopper;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityMinecartHopper extends EntityMinecartContainer implements IHopper {
    public static final int MOVE_ITEM_SPEED = 4;
    private boolean enabled = true;
    private int cooldownTime = -1;
    private final BlockPosition lastPosition = BlockPosition.ZERO;

    public EntityMinecartHopper(EntityTypes<? extends EntityMinecartHopper> type, World world) {
        super(type, world);
    }

    public EntityMinecartHopper(World world, double x, double y, double z) {
        super(EntityTypes.HOPPER_MINECART, x, y, z, world);
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.HOPPER;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.HOPPER.getBlockData();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 1;
    }

    @Override
    public int getSize() {
        return 5;
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        boolean bl = !powered;
        if (bl != this.isEnabled()) {
            this.setEnabled(bl);
        }

    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public double getWorldX() {
        return this.locX();
    }

    @Override
    public double getWorldY() {
        return this.locY() + 0.5D;
    }

    @Override
    public double getWorldZ() {
        return this.locZ();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && this.isAlive() && this.isEnabled()) {
            BlockPosition blockPos = this.getChunkCoordinates();
            if (blockPos.equals(this.lastPosition)) {
                --this.cooldownTime;
            } else {
                this.setCooldown(0);
            }

            if (!this.isOnCooldown()) {
                this.setCooldown(0);
                if (this.suckInItems()) {
                    this.setCooldown(4);
                    this.update();
                }
            }
        }

    }

    public boolean suckInItems() {
        if (TileEntityHopper.suckInItems(this.level, this)) {
            return true;
        } else {
            List<EntityItem> list = this.level.getEntitiesOfClass(EntityItem.class, this.getBoundingBox().grow(0.25D, 0.0D, 0.25D), IEntitySelector.ENTITY_STILL_ALIVE);
            if (!list.isEmpty()) {
                TileEntityHopper.addItem(this, list.get(0));
            }

            return false;
        }
    }

    @Override
    public void destroy(DamageSource damageSource) {
        super.destroy(damageSource);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(Blocks.HOPPER);
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("TransferCooldown", this.cooldownTime);
        nbt.setBoolean("Enabled", this.enabled);
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.cooldownTime = nbt.getInt("TransferCooldown");
        this.enabled = nbt.hasKey("Enabled") ? nbt.getBoolean("Enabled") : true;
    }

    public void setCooldown(int cooldown) {
        this.cooldownTime = cooldown;
    }

    public boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    @Override
    public Container createMenu(int syncId, PlayerInventory playerInventory) {
        return new ContainerHopper(syncId, playerInventory, this);
    }
}
