package net.minecraft.world.entity.animal.horse;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemHorseArmor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.SoundEffectType;

public class EntityHorse extends EntityHorseAbstract {
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
    private static final DataWatcherObject<Integer> DATA_ID_TYPE_VARIANT = DataWatcher.defineId(EntityHorse.class, DataWatcherRegistry.INT);

    public EntityHorse(EntityTypes<? extends EntityHorse> type, World world) {
        super(type, world);
    }

    @Override
    protected void randomizeAttributes() {
        this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue((double)this.generateRandomMaxHealth());
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(this.generateRandomSpeed());
        this.getAttributeInstance(GenericAttributes.JUMP_STRENGTH).setValue(this.generateRandomJumpStrength());
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Variant", this.getVariantRaw());
        if (!this.inventory.getItem(1).isEmpty()) {
            nbt.set("ArmorItem", this.inventory.getItem(1).save(new NBTTagCompound()));
        }

    }

    public ItemStack getArmor() {
        return this.getEquipment(EnumItemSlot.CHEST);
    }

    private void setArmor(ItemStack stack) {
        this.setSlot(EnumItemSlot.CHEST, stack);
        this.setDropChance(EnumItemSlot.CHEST, 0.0F);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setVariantRaw(nbt.getInt("Variant"));
        if (nbt.hasKeyOfType("ArmorItem", 10)) {
            ItemStack itemStack = ItemStack.of(nbt.getCompound("ArmorItem"));
            if (!itemStack.isEmpty() && this.isArmor(itemStack)) {
                this.inventory.setItem(1, itemStack);
            }
        }

        this.updateContainerEquipment();
    }

    private void setVariantRaw(int variant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, variant);
    }

    private int getVariantRaw() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    public void setVariant(HorseColor color, HorseStyle marking) {
        this.setVariantRaw(color.getId() & 255 | marking.getId() << 8 & '\uff00');
    }

    public HorseColor getColor() {
        return HorseColor.byId(this.getVariantRaw() & 255);
    }

    public HorseStyle getStyle() {
        return HorseStyle.byId((this.getVariantRaw() & '\uff00') >> 8);
    }

    @Override
    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            super.updateContainerEquipment();
            this.setArmorEquipment(this.inventory.getItem(1));
            this.setDropChance(EnumItemSlot.CHEST, 0.0F);
        }
    }

    private void setArmorEquipment(ItemStack stack) {
        this.setArmor(stack);
        if (!this.level.isClientSide) {
            this.getAttributeInstance(GenericAttributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            if (this.isArmor(stack)) {
                int i = ((ItemHorseArmor)stack.getItem()).getProtection();
                if (i != 0) {
                    this.getAttributeInstance(GenericAttributes.ARMOR).addTransientModifier(new AttributeModifier(ARMOR_MODIFIER_UUID, "Horse armor bonus", (double)i, AttributeModifier.Operation.ADDITION));
                }
            }
        }

    }

    @Override
    public void containerChanged(IInventory sender) {
        ItemStack itemStack = this.getArmor();
        super.containerChanged(sender);
        ItemStack itemStack2 = this.getArmor();
        if (this.tickCount > 20 && this.isArmor(itemStack2) && itemStack != itemStack2) {
            this.playSound(SoundEffects.HORSE_ARMOR, 0.5F, 1.0F);
        }

    }

    @Override
    protected void playGallopSound(SoundEffectType group) {
        super.playGallopSound(group);
        if (this.random.nextInt(10) == 0) {
            this.playSound(SoundEffects.HORSE_BREATHE, group.getVolume() * 0.6F, group.getPitch());
        }

    }

    @Override
    protected SoundEffect getSoundAmbient() {
        super.getSoundAmbient();
        return SoundEffects.HORSE_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        super.getSoundDeath();
        return SoundEffects.HORSE_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getEatingSound() {
        return SoundEffects.HORSE_EAT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        super.getSoundHurt(source);
        return SoundEffects.HORSE_HURT;
    }

    @Override
    protected SoundEffect getSoundAngry() {
        super.getSoundAngry();
        return SoundEffects.HORSE_ANGRY;
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

            EnumInteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
            if (interactionResult.consumesAction()) {
                return interactionResult;
            }

            if (!this.isTamed()) {
                this.makeMad();
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }

            boolean bl = !this.isBaby() && !this.hasSaddle() && itemStack.is(Items.SADDLE);
            if (this.isArmor(itemStack) || bl) {
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

    @Override
    public boolean mate(EntityAnimal other) {
        if (other == this) {
            return false;
        } else if (!(other instanceof EntityHorseDonkey) && !(other instanceof EntityHorse)) {
            return false;
        } else {
            return this.canParent() && ((EntityHorseAbstract)other).canParent();
        }
    }

    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        EntityHorseAbstract abstractHorse;
        if (entity instanceof EntityHorseDonkey) {
            abstractHorse = EntityTypes.MULE.create(world);
        } else {
            EntityHorse horse = (EntityHorse)entity;
            abstractHorse = EntityTypes.HORSE.create(world);
            int i = this.random.nextInt(9);
            HorseColor variant;
            if (i < 4) {
                variant = this.getColor();
            } else if (i < 8) {
                variant = horse.getColor();
            } else {
                variant = SystemUtils.getRandom(HorseColor.values(), this.random);
            }

            int j = this.random.nextInt(5);
            HorseStyle markings;
            if (j < 2) {
                markings = this.getStyle();
            } else if (j < 4) {
                markings = horse.getStyle();
            } else {
                markings = SystemUtils.getRandom(HorseStyle.values(), this.random);
            }

            ((EntityHorse)abstractHorse).setVariant(variant, markings);
        }

        this.setOffspringAttributes(entity, abstractHorse);
        return abstractHorse;
    }

    @Override
    public boolean canWearArmor() {
        return true;
    }

    @Override
    public boolean isArmor(ItemStack item) {
        return item.getItem() instanceof ItemHorseArmor;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        HorseColor variant;
        if (entityData instanceof EntityHorse.HorseGroupData) {
            variant = ((EntityHorse.HorseGroupData)entityData).variant;
        } else {
            variant = SystemUtils.getRandom(HorseColor.values(), this.random);
            entityData = new EntityHorse.HorseGroupData(variant);
        }

        this.setVariant(variant, SystemUtils.getRandom(HorseStyle.values(), this.random));
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public static class HorseGroupData extends EntityAgeable.GroupDataAgeable {
        public final HorseColor variant;

        public HorseGroupData(HorseColor color) {
            super(true);
            this.variant = color;
        }
    }
}
