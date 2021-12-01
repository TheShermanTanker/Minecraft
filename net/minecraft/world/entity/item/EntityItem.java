package net.minecraft.world.entity.item;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntityItem extends Entity {
    private static final DataWatcherObject<ItemStack> DATA_ITEM = DataWatcher.defineId(EntityItem.class, DataWatcherRegistry.ITEM_STACK);
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = 32767;
    private static final int INFINITE_LIFETIME = -32768;
    public int age;
    public int pickupDelay;
    private int health = 5;
    @Nullable
    private UUID thrower;
    @Nullable
    private UUID owner;
    public final float bobOffs;

    public EntityItem(EntityTypes<? extends EntityItem> type, World world) {
        super(type, world);
        this.bobOffs = this.random.nextFloat() * (float)Math.PI * 2.0F;
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        this(world, x, y, z, stack, world.random.nextDouble() * 0.2D - 0.1D, 0.2D, world.random.nextDouble() * 0.2D - 0.1D);
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack, double velocityX, double velocityY, double velocityZ) {
        this(EntityTypes.ITEM, world);
        this.setPosition(x, y, z);
        this.setMot(velocityX, velocityY, velocityZ);
        this.setItemStack(stack);
    }

    private EntityItem(EntityItem entity) {
        super(entity.getEntityType(), entity.level);
        this.setItemStack(entity.getItemStack().cloneItemStack());
        this.copyPosition(entity);
        this.age = entity.age;
        this.bobOffs = entity.bobOffs;
    }

    @Override
    public boolean occludesVibrations() {
        return TagsItem.OCCLUDES_VIBRATION_SIGNALS.isTagged(this.getItemStack().getItem());
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        if (this.getItemStack().isEmpty()) {
            this.die();
        } else {
            super.tick();
            if (this.pickupDelay > 0 && this.pickupDelay != 32767) {
                --this.pickupDelay;
            }

            this.xo = this.locX();
            this.yo = this.locY();
            this.zo = this.locZ();
            Vec3D vec3 = this.getMot();
            float f = this.getHeadHeight() - 0.11111111F;
            if (this.isInWater() && this.getFluidHeight(TagsFluid.WATER) > (double)f) {
                this.setUnderwaterMovement();
            } else if (this.isInLava() && this.getFluidHeight(TagsFluid.LAVA) > (double)f) {
                this.setUnderLavaMovement();
            } else if (!this.isNoGravity()) {
                this.setMot(this.getMot().add(0.0D, -0.04D, 0.0D));
            }

            if (this.level.isClientSide) {
                this.noPhysics = false;
            } else {
                this.noPhysics = !this.level.getCubes(this, this.getBoundingBox().shrink(1.0E-7D));
                if (this.noPhysics) {
                    this.moveTowardsClosestSpace(this.locX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.locZ());
                }
            }

            if (!this.onGround || this.getMot().horizontalDistanceSqr() > (double)1.0E-5F || (this.tickCount + this.getId()) % 4 == 0) {
                this.move(EnumMoveType.SELF, this.getMot());
                float g = 0.98F;
                if (this.onGround) {
                    g = this.level.getType(new BlockPosition(this.locX(), this.locY() - 1.0D, this.locZ())).getBlock().getFrictionFactor() * 0.98F;
                }

                this.setMot(this.getMot().multiply((double)g, 0.98D, (double)g));
                if (this.onGround) {
                    Vec3D vec32 = this.getMot();
                    if (vec32.y < 0.0D) {
                        this.setMot(vec32.multiply(1.0D, -0.5D, 1.0D));
                    }
                }
            }

            boolean bl = MathHelper.floor(this.xo) != MathHelper.floor(this.locX()) || MathHelper.floor(this.yo) != MathHelper.floor(this.locY()) || MathHelper.floor(this.zo) != MathHelper.floor(this.locZ());
            int i = bl ? 2 : 40;
            if (this.tickCount % i == 0 && !this.level.isClientSide && this.isMergable()) {
                this.mergeNearby();
            }

            if (this.age != -32768) {
                ++this.age;
            }

            this.hasImpulse |= this.updateInWaterStateAndDoFluidPushing();
            if (!this.level.isClientSide) {
                double d = this.getMot().subtract(vec3).lengthSqr();
                if (d > 0.01D) {
                    this.hasImpulse = true;
                }
            }

            if (!this.level.isClientSide && this.age >= 6000) {
                this.die();
            }

        }
    }

    private void setUnderwaterMovement() {
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x * (double)0.99F, vec3.y + (double)(vec3.y < (double)0.06F ? 5.0E-4F : 0.0F), vec3.z * (double)0.99F);
    }

    private void setUnderLavaMovement() {
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x * (double)0.95F, vec3.y + (double)(vec3.y < (double)0.06F ? 5.0E-4F : 0.0F), vec3.z * (double)0.95F);
    }

    private void mergeNearby() {
        if (this.isMergable()) {
            for(EntityItem itemEntity : this.level.getEntitiesOfClass(EntityItem.class, this.getBoundingBox().grow(0.5D, 0.0D, 0.5D), (itemEntityx) -> {
                return itemEntityx != this && itemEntityx.isMergable();
            })) {
                if (itemEntity.isMergable()) {
                    this.tryToMerge(itemEntity);
                    if (this.isRemoved()) {
                        break;
                    }
                }
            }

        }
    }

    private boolean isMergable() {
        ItemStack itemStack = this.getItemStack();
        return this.isAlive() && this.pickupDelay != 32767 && this.age != -32768 && this.age < 6000 && itemStack.getCount() < itemStack.getMaxStackSize();
    }

    private void tryToMerge(EntityItem other) {
        ItemStack itemStack = this.getItemStack();
        ItemStack itemStack2 = other.getItemStack();
        if (Objects.equals(this.getOwner(), other.getOwner()) && areMergable(itemStack, itemStack2)) {
            if (itemStack2.getCount() < itemStack.getCount()) {
                merge(this, itemStack, other, itemStack2);
            } else {
                merge(other, itemStack2, this, itemStack);
            }

        }
    }

    public static boolean areMergable(ItemStack stack1, ItemStack stack2) {
        if (!stack2.is(stack1.getItem())) {
            return false;
        } else if (stack2.getCount() + stack1.getCount() > stack2.getMaxStackSize()) {
            return false;
        } else if (stack2.hasTag() ^ stack1.hasTag()) {
            return false;
        } else {
            return !stack2.hasTag() || stack2.getTag().equals(stack1.getTag());
        }
    }

    public static ItemStack merge(ItemStack stack1, ItemStack stack2, int maxCount) {
        int i = Math.min(Math.min(stack1.getMaxStackSize(), maxCount) - stack1.getCount(), stack2.getCount());
        ItemStack itemStack = stack1.cloneItemStack();
        itemStack.add(i);
        stack2.subtract(i);
        return itemStack;
    }

    private static void merge(EntityItem targetEntity, ItemStack stack1, ItemStack stack2) {
        ItemStack itemStack = merge(stack1, stack2, 64);
        targetEntity.setItemStack(itemStack);
    }

    private static void merge(EntityItem targetEntity, ItemStack targetStack, EntityItem sourceEntity, ItemStack sourceStack) {
        merge(targetEntity, targetStack, sourceStack);
        targetEntity.pickupDelay = Math.max(targetEntity.pickupDelay, sourceEntity.pickupDelay);
        targetEntity.age = Math.min(targetEntity.age, sourceEntity.age);
        if (sourceStack.isEmpty()) {
            sourceEntity.die();
        }

    }

    @Override
    public boolean isFireProof() {
        return this.getItemStack().getItem().isFireResistant() || super.isFireProof();
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (!this.getItemStack().isEmpty() && this.getItemStack().is(Items.NETHER_STAR) && source.isExplosion()) {
            return false;
        } else if (!this.getItemStack().getItem().canBeHurtBy(source)) {
            return false;
        } else {
            this.velocityChanged();
            this.health = (int)((float)this.health - amount);
            this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
            if (this.health <= 0) {
                this.getItemStack().onDestroyed(this);
                this.die();
            }

            return true;
        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        nbt.setShort("Health", (short)this.health);
        nbt.setShort("Age", (short)this.age);
        nbt.setShort("PickupDelay", (short)this.pickupDelay);
        if (this.getThrower() != null) {
            nbt.putUUID("Thrower", this.getThrower());
        }

        if (this.getOwner() != null) {
            nbt.putUUID("Owner", this.getOwner());
        }

        if (!this.getItemStack().isEmpty()) {
            nbt.set("Item", this.getItemStack().save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.health = nbt.getShort("Health");
        this.age = nbt.getShort("Age");
        if (nbt.hasKey("PickupDelay")) {
            this.pickupDelay = nbt.getShort("PickupDelay");
        }

        if (nbt.hasUUID("Owner")) {
            this.owner = nbt.getUUID("Owner");
        }

        if (nbt.hasUUID("Thrower")) {
            this.thrower = nbt.getUUID("Thrower");
        }

        NBTTagCompound compoundTag = nbt.getCompound("Item");
        this.setItemStack(ItemStack.of(compoundTag));
        if (this.getItemStack().isEmpty()) {
            this.die();
        }

    }

    @Override
    public void pickup(EntityHuman player) {
        if (!this.level.isClientSide) {
            ItemStack itemStack = this.getItemStack();
            Item item = itemStack.getItem();
            int i = itemStack.getCount();
            if (this.pickupDelay == 0 && (this.owner == null || this.owner.equals(player.getUniqueID())) && player.getInventory().pickup(itemStack)) {
                player.receive(this, i);
                if (itemStack.isEmpty()) {
                    this.die();
                    itemStack.setCount(i);
                }

                player.awardStat(StatisticList.ITEM_PICKED_UP.get(item), i);
                player.onItemPickup(this);
            }

        }
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        IChatBaseComponent component = this.getCustomName();
        return (IChatBaseComponent)(component != null ? component : new ChatMessage(this.getItemStack().getDescriptionId()));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(WorldServer destination) {
        Entity entity = super.changeDimension(destination);
        if (!this.level.isClientSide && entity instanceof EntityItem) {
            ((EntityItem)entity).mergeNearby();
        }

        return entity;
    }

    public ItemStack getItemStack() {
        return this.getDataWatcher().get(DATA_ITEM);
    }

    public void setItemStack(ItemStack stack) {
        this.getDataWatcher().set(DATA_ITEM, stack);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        super.onSyncedDataUpdated(data);
        if (DATA_ITEM.equals(data)) {
            this.getItemStack().setEntityRepresentation(this);
        }

    }

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID uuid) {
        this.owner = uuid;
    }

    @Nullable
    public UUID getThrower() {
        return this.thrower;
    }

    public void setThrower(@Nullable UUID uuid) {
        this.thrower = uuid;
    }

    public int getAge() {
        return this.age;
    }

    public void defaultPickupDelay() {
        this.pickupDelay = 10;
    }

    public void setNoPickUpDelay() {
        this.pickupDelay = 0;
    }

    public void setNeverPickUp() {
        this.pickupDelay = 32767;
    }

    public void setPickupDelay(int pickupDelay) {
        this.pickupDelay = pickupDelay;
    }

    public boolean hasPickUpDelay() {
        return this.pickupDelay > 0;
    }

    public void setUnlimitedLifetime() {
        this.age = -32768;
    }

    public void setExtendedLifetime() {
        this.age = -6000;
    }

    public void makeFakeItem() {
        this.setNeverPickUp();
        this.age = 5999;
    }

    public float getSpin(float tickDelta) {
        return ((float)this.getAge() + tickDelta) / 20.0F + this.bobOffs;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    public EntityItem copy() {
        return new EntityItem(this);
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.AMBIENT;
    }
}
