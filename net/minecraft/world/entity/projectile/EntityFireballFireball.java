package net.minecraft.world.entity.projectile;

import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public abstract class EntityFireballFireball extends EntityFireball implements ItemSupplier {
    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK = DataWatcher.defineId(EntityFireballFireball.class, DataWatcherRegistry.ITEM_STACK);

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> type, World world) {
        super(type, world);
    }

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> type, double x, double y, double z, double directionX, double directionY, double directionZ, World world) {
        super(type, x, y, z, directionX, directionY, directionZ, world);
    }

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> type, EntityLiving owner, double directionX, double directionY, double directionZ, World world) {
        super(type, owner, directionX, directionY, directionZ, world);
    }

    public void setItem(ItemStack stack) {
        if (!stack.is(Items.FIRE_CHARGE) || stack.hasTag()) {
            this.getDataWatcher().set(DATA_ITEM_STACK, SystemUtils.make(stack.cloneItemStack(), (stackx) -> {
                stackx.setCount(1);
            }));
        }

    }

    public ItemStack getItem() {
        return this.getDataWatcher().get(DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getSuppliedItem() {
        ItemStack itemStack = this.getItem();
        return itemStack.isEmpty() ? new ItemStack(Items.FIRE_CHARGE) : itemStack;
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
