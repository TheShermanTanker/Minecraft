package net.minecraft.world.entity.animal.horse;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public abstract class EntityHorseChestedAbstract extends EntityHorseAbstract {
    private static final DataWatcherObject<Boolean> DATA_ID_CHEST = DataWatcher.defineId(EntityHorseChestedAbstract.class, DataWatcherRegistry.BOOLEAN);
    public static final int INV_CHEST_COUNT = 15;

    protected EntityHorseChestedAbstract(EntityTypes<? extends EntityHorseChestedAbstract> type, World world) {
        super(type, world);
        this.canGallop = false;
    }

    @Override
    protected void randomizeAttributes() {
        this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue((double)this.generateRandomMaxHealth());
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_CHEST, false);
    }

    public static AttributeProvider.Builder createBaseChestedHorseAttributes() {
        return createBaseHorseAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.175F).add(GenericAttributes.JUMP_STRENGTH, 0.5D);
    }

    public boolean isCarryingChest() {
        return this.entityData.get(DATA_ID_CHEST);
    }

    public void setCarryingChest(boolean hasChest) {
        this.entityData.set(DATA_ID_CHEST, hasChest);
    }

    @Override
    protected int getChestSlots() {
        return this.isCarryingChest() ? 17 : super.getChestSlots();
    }

    @Override
    public double getPassengersRidingOffset() {
        return super.getPassengersRidingOffset() - 0.25D;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isCarryingChest()) {
            if (!this.level.isClientSide) {
                this.spawnAtLocation(Blocks.CHEST);
            }

            this.setCarryingChest(false);
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("ChestedHorse", this.isCarryingChest());
        if (this.isCarryingChest()) {
            NBTTagList listTag = new NBTTagList();

            for(int i = 2; i < this.inventory.getSize(); ++i) {
                ItemStack itemStack = this.inventory.getItem(i);
                if (!itemStack.isEmpty()) {
                    NBTTagCompound compoundTag = new NBTTagCompound();
                    compoundTag.setByte("Slot", (byte)i);
                    itemStack.save(compoundTag);
                    listTag.add(compoundTag);
                }
            }

            nbt.set("Items", listTag);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setCarryingChest(nbt.getBoolean("ChestedHorse"));
        this.loadChest();
        if (this.isCarryingChest()) {
            NBTTagList listTag = nbt.getList("Items", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                int j = compoundTag.getByte("Slot") & 255;
                if (j >= 2 && j < this.inventory.getSize()) {
                    this.inventory.setItem(j, ItemStack.of(compoundTag));
                }
            }
        }

        this.updateContainerEquipment();
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        return mappedIndex == 499 ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return EntityHorseChestedAbstract.this.isCarryingChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
            }

            @Override
            public boolean set(ItemStack stack) {
                if (stack.isEmpty()) {
                    if (EntityHorseChestedAbstract.this.isCarryingChest()) {
                        EntityHorseChestedAbstract.this.setCarryingChest(false);
                        EntityHorseChestedAbstract.this.loadChest();
                    }

                    return true;
                } else if (stack.is(Items.CHEST)) {
                    if (!EntityHorseChestedAbstract.this.isCarryingChest()) {
                        EntityHorseChestedAbstract.this.setCarryingChest(true);
                        EntityHorseChestedAbstract.this.loadChest();
                    }

                    return true;
                } else {
                    return false;
                }
            }
        } : super.getSlot(mappedIndex);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isBaby()) {
            if (this.isTamed() && player.isSecondaryUseActive()) {
                this.openInventory(player);
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }

            if (this.isVehicle()) {
                return super.mobInteract(player, hand);
            }
        }

        if (!itemStack.isEmpty()) {
            if (this.isBreedItem(itemStack)) {
                return this.fedFood(player, itemStack);
            }

            if (!this.isTamed()) {
                this.makeMad();
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }

            if (!this.isCarryingChest() && itemStack.is(Blocks.CHEST.getItem())) {
                this.setCarryingChest(true);
                this.playChestEquipsSound();
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                this.loadChest();
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }

            if (!this.isBaby() && !this.hasSaddle() && itemStack.is(Items.SADDLE)) {
                this.openInventory(player);
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        if (this.isBaby()) {
            return super.mobInteract(player, hand);
        } else {
            this.doPlayerRide(player);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        }
    }

    protected void playChestEquipsSound() {
        this.playSound(SoundEffects.DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    public int getInventoryColumns() {
        return 5;
    }
}
