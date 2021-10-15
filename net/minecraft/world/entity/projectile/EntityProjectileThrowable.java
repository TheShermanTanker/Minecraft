package net.minecraft.world.entity.projectile;

import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public abstract class EntityProjectileThrowable extends EntityProjectile implements ItemSupplier {
    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK = DataWatcher.defineId(EntityProjectileThrowable.class, DataWatcherRegistry.ITEM_STACK);

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> type, World world) {
        super(type, world);
    }

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> type, double x, double y, double z, World world) {
        super(type, x, y, z, world);
    }

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> type, EntityLiving owner, World world) {
        super(type, owner, world);
    }

    public void setItem(ItemStack item) {
        if (!item.is(this.getDefaultItem()) || item.hasTag()) {
            this.getDataWatcher().set(DATA_ITEM_STACK, SystemUtils.make(item.cloneItemStack(), (stack) -> {
                stack.setCount(1);
            }));
        }

    }

    protected abstract Item getDefaultItem();

    public ItemStack getItem() {
        return this.getDataWatcher().get(DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getSuppliedItem() {
        ItemStack itemStack = this.getItem();
        return itemStack.isEmpty() ? new ItemStack(this.getDefaultItem()) : itemStack;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        ItemStack itemStack = this.getItem();
        if (!itemStack.isEmpty()) {
            nbt.set("Item", itemStack.save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        ItemStack itemStack = ItemStack.of(nbt.getCompound("Item"));
        this.setItem(itemStack);
    }
}
