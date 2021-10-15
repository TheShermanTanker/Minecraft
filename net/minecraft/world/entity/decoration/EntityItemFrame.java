package net.minecraft.world.entity.decoration;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDiodeAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityItemFrame extends EntityHanging {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DataWatcherObject<ItemStack> DATA_ITEM = DataWatcher.defineId(EntityItemFrame.class, DataWatcherRegistry.ITEM_STACK);
    private static final DataWatcherObject<Integer> DATA_ROTATION = DataWatcher.defineId(EntityItemFrame.class, DataWatcherRegistry.INT);
    public static final int NUM_ROTATIONS = 8;
    public float dropChance = 1.0F;
    public boolean fixed;

    public EntityItemFrame(EntityTypes<? extends EntityItemFrame> type, World world) {
        super(type, world);
    }

    public EntityItemFrame(World world, BlockPosition pos, EnumDirection facing) {
        this(EntityTypes.ITEM_FRAME, world, pos, facing);
    }

    public EntityItemFrame(EntityTypes<? extends EntityItemFrame> type, World world, BlockPosition pos, EnumDirection facing) {
        super(type, world, pos);
        this.setDirection(facing);
    }

    @Override
    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return 0.0F;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_ITEM, ItemStack.EMPTY);
        this.getDataWatcher().register(DATA_ROTATION, 0);
    }

    @Override
    public void setDirection(EnumDirection facing) {
        Validate.notNull(facing);
        this.direction = facing;
        if (facing.getAxis().isHorizontal()) {
            this.setXRot(0.0F);
            this.setYRot((float)(this.direction.get2DRotationValue() * 90));
        } else {
            this.setXRot((float)(-90 * facing.getAxisDirection().getStep()));
            this.setYRot(0.0F);
        }

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.updateBoundingBox();
    }

    @Override
    protected void updateBoundingBox() {
        if (this.direction != null) {
            double d = 0.46875D;
            double e = (double)this.pos.getX() + 0.5D - (double)this.direction.getAdjacentX() * 0.46875D;
            double f = (double)this.pos.getY() + 0.5D - (double)this.direction.getAdjacentY() * 0.46875D;
            double g = (double)this.pos.getZ() + 0.5D - (double)this.direction.getAdjacentZ() * 0.46875D;
            this.setPositionRaw(e, f, g);
            double h = (double)this.getHangingWidth();
            double i = (double)this.getHangingHeight();
            double j = (double)this.getHangingWidth();
            EnumDirection.EnumAxis axis = this.direction.getAxis();
            switch(axis) {
            case X:
                h = 1.0D;
                break;
            case Y:
                i = 1.0D;
                break;
            case Z:
                j = 1.0D;
            }

            h = h / 32.0D;
            i = i / 32.0D;
            j = j / 32.0D;
            this.setBoundingBox(new AxisAlignedBB(e - h, f - i, g - j, e + h, f + i, g + j));
        }
    }

    @Override
    public boolean survives() {
        if (this.fixed) {
            return true;
        } else if (!this.level.getCubes(this)) {
            return false;
        } else {
            IBlockData blockState = this.level.getType(this.pos.relative(this.direction.opposite()));
            return blockState.getMaterial().isBuildable() || this.direction.getAxis().isHorizontal() && BlockDiodeAbstract.isDiode(blockState) ? this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty() : false;
        }
    }

    @Override
    public void move(EnumMoveType movementType, Vec3D movement) {
        if (!this.fixed) {
            super.move(movementType, movement);
        }

    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (!this.fixed) {
            super.push(deltaX, deltaY, deltaZ);
        }

    }

    @Override
    public float getPickRadius() {
        return 0.0F;
    }

    @Override
    public void killEntity() {
        this.removeFramedMap(this.getItem());
        super.killEntity();
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.fixed) {
            return source != DamageSource.OUT_OF_WORLD && !source.isCreativePlayer() ? false : super.damageEntity(source, amount);
        } else if (this.isInvulnerable(source)) {
            return false;
        } else if (!source.isExplosion() && !this.getItem().isEmpty()) {
            if (!this.level.isClientSide) {
                this.dropItem(source.getEntity(), false);
                this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.damageEntity(source, amount);
        }
    }

    public SoundEffect getRemoveItemSound() {
        return SoundEffects.ITEM_FRAME_REMOVE_ITEM;
    }

    @Override
    public int getHangingWidth() {
        return 12;
    }

    @Override
    public int getHangingHeight() {
        return 12;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 16.0D;
        d = d * 64.0D * getViewScale();
        return distance < d * d;
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        this.dropItem(entity, true);
    }

    public SoundEffect getBreakSound() {
        return SoundEffects.ITEM_FRAME_BREAK;
    }

    @Override
    public void playPlaceSound() {
        this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
    }

    public SoundEffect getPlaceSound() {
        return SoundEffects.ITEM_FRAME_PLACE;
    }

    private void dropItem(@Nullable Entity entity, boolean alwaysDrop) {
        if (!this.fixed) {
            ItemStack itemStack = this.getItem();
            this.setItem(ItemStack.EMPTY);
            if (!this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                if (entity == null) {
                    this.removeFramedMap(itemStack);
                }

            } else {
                if (entity instanceof EntityHuman) {
                    EntityHuman player = (EntityHuman)entity;
                    if (player.getAbilities().instabuild) {
                        this.removeFramedMap(itemStack);
                        return;
                    }
                }

                if (alwaysDrop) {
                    this.spawnAtLocation(this.getFrameItemStack());
                }

                if (!itemStack.isEmpty()) {
                    itemStack = itemStack.cloneItemStack();
                    this.removeFramedMap(itemStack);
                    if (this.random.nextFloat() < this.dropChance) {
                        this.spawnAtLocation(itemStack);
                    }
                }

            }
        }
    }

    private void removeFramedMap(ItemStack map) {
        if (map.is(Items.FILLED_MAP)) {
            WorldMap mapItemSavedData = ItemWorldMap.getSavedMap(map, this.level);
            if (mapItemSavedData != null) {
                mapItemSavedData.removedFromFrame(this.pos, this.getId());
                mapItemSavedData.setDirty(true);
            }
        }

        map.setEntityRepresentation((Entity)null);
    }

    public ItemStack getItem() {
        return this.getDataWatcher().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.setItem(stack, true);
    }

    public void setItem(ItemStack value, boolean update) {
        if (!value.isEmpty()) {
            value = value.cloneItemStack();
            value.setCount(1);
            value.setEntityRepresentation(this);
        }

        this.getDataWatcher().set(DATA_ITEM, value);
        if (!value.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }

        if (update && this.pos != null) {
            this.level.updateAdjacentComparators(this.pos, Blocks.AIR);
        }

    }

    public SoundEffect getAddItemSound() {
        return SoundEffects.ITEM_FRAME_ADD_ITEM;
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        return mappedIndex == 0 ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return EntityItemFrame.this.getItem();
            }

            @Override
            public boolean set(ItemStack stack) {
                EntityItemFrame.this.setItem(stack);
                return true;
            }
        } : super.getSlot(mappedIndex);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (data.equals(DATA_ITEM)) {
            ItemStack itemStack = this.getItem();
            if (!itemStack.isEmpty() && itemStack.getFrame() != this) {
                itemStack.setEntityRepresentation(this);
            }
        }

    }

    public int getRotation() {
        return this.getDataWatcher().get(DATA_ROTATION);
    }

    public void setRotation(int value) {
        this.setRotation(value, true);
    }

    private void setRotation(int value, boolean updateComparators) {
        this.getDataWatcher().set(DATA_ROTATION, value % 8);
        if (updateComparators && this.pos != null) {
            this.level.updateAdjacentComparators(this.pos, Blocks.AIR);
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (!this.getItem().isEmpty()) {
            nbt.set("Item", this.getItem().save(new NBTTagCompound()));
            nbt.setByte("ItemRotation", (byte)this.getRotation());
            nbt.setFloat("ItemDropChance", this.dropChance);
        }

        nbt.setByte("Facing", (byte)this.direction.get3DDataValue());
        nbt.setBoolean("Invisible", this.isInvisible());
        nbt.setBoolean("Fixed", this.fixed);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        NBTTagCompound compoundTag = nbt.getCompound("Item");
        if (compoundTag != null && !compoundTag.isEmpty()) {
            ItemStack itemStack = ItemStack.of(compoundTag);
            if (itemStack.isEmpty()) {
                LOGGER.warn("Unable to load item from: {}", (Object)compoundTag);
            }

            ItemStack itemStack2 = this.getItem();
            if (!itemStack2.isEmpty() && !ItemStack.matches(itemStack, itemStack2)) {
                this.removeFramedMap(itemStack2);
            }

            this.setItem(itemStack, false);
            this.setRotation(nbt.getByte("ItemRotation"), false);
            if (nbt.hasKeyOfType("ItemDropChance", 99)) {
                this.dropChance = nbt.getFloat("ItemDropChance");
            }
        }

        this.setDirection(EnumDirection.fromType1(nbt.getByte("Facing")));
        this.setInvisible(nbt.getBoolean("Invisible"));
        this.fixed = nbt.getBoolean("Fixed");
    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        boolean bl = !this.getItem().isEmpty();
        boolean bl2 = !itemStack.isEmpty();
        if (this.fixed) {
            return EnumInteractionResult.PASS;
        } else if (!this.level.isClientSide) {
            if (!bl) {
                if (bl2 && !this.isRemoved()) {
                    if (itemStack.is(Items.FILLED_MAP)) {
                        WorldMap mapItemSavedData = ItemWorldMap.getSavedMap(itemStack, this.level);
                        if (mapItemSavedData != null && mapItemSavedData.isTrackedCountOverLimit(256)) {
                            return EnumInteractionResult.FAIL;
                        }
                    }

                    this.setItem(itemStack);
                    if (!player.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }
                }
            } else {
                this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
                this.setRotation(this.getRotation() + 1);
            }

            return EnumInteractionResult.CONSUME;
        } else {
            return !bl && !bl2 ? EnumInteractionResult.PASS : EnumInteractionResult.SUCCESS;
        }
    }

    public SoundEffect getRotateItemSound() {
        return SoundEffects.ITEM_FRAME_ROTATE_ITEM;
    }

    public int getAnalogOutput() {
        return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this, this.getEntityType(), this.direction.get3DDataValue(), this.getBlockPosition());
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        this.setDirection(EnumDirection.fromType1(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack itemStack = this.getItem();
        return itemStack.isEmpty() ? this.getFrameItemStack() : itemStack.cloneItemStack();
    }

    protected ItemStack getFrameItemStack() {
        return new ItemStack(Items.ITEM_FRAME);
    }
}
