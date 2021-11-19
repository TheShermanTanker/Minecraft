package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.core.dispenser.DispenseBehaviorItem;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.phys.AxisAlignedBB;

public class ItemArmor extends Item implements ItemWearable {
    private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")};
    public static final IDispenseBehavior DISPENSE_ITEM_BEHAVIOR = new DispenseBehaviorItem() {
        @Override
        protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
            return ItemArmor.dispenseArmor(pointer, stack) ? stack : super.a(pointer, stack);
        }
    };
    protected final EnumItemSlot slot;
    private final int defense;
    private final float toughness;
    protected final float knockbackResistance;
    protected final ArmorMaterial material;
    private final Multimap<AttributeBase, AttributeModifier> defaultModifiers;

    public static boolean dispenseArmor(ISourceBlock pointer, ItemStack armor) {
        BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
        List<EntityLiving> list = pointer.getWorld().getEntitiesOfClass(EntityLiving.class, new AxisAlignedBB(blockPos), IEntitySelector.NO_SPECTATORS.and(new IEntitySelector.EntitySelectorEquipable(armor)));
        if (list.isEmpty()) {
            return false;
        } else {
            EntityLiving livingEntity = list.get(0);
            EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(armor);
            ItemStack itemStack = armor.cloneAndSubtract(1);
            livingEntity.setSlot(equipmentSlot, itemStack);
            if (livingEntity instanceof EntityInsentient) {
                ((EntityInsentient)livingEntity).setDropChance(equipmentSlot, 2.0F);
                ((EntityInsentient)livingEntity).setPersistent();
            }

            return true;
        }
    }

    public ItemArmor(ArmorMaterial material, EnumItemSlot slot, Item.Info settings) {
        super(settings.defaultDurability(material.getDurabilityForSlot(slot)));
        this.material = material;
        this.slot = slot;
        this.defense = material.getDefenseForSlot(slot);
        this.toughness = material.getToughness();
        this.knockbackResistance = material.getKnockbackResistance();
        BlockDispenser.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
        Builder<AttributeBase, AttributeModifier> builder = ImmutableMultimap.builder();
        UUID uUID = ARMOR_MODIFIER_UUID_PER_SLOT[slot.getIndex()];
        builder.put(GenericAttributes.ARMOR, new AttributeModifier(uUID, "Armor modifier", (double)this.defense, AttributeModifier.Operation.ADDITION));
        builder.put(GenericAttributes.ARMOR_TOUGHNESS, new AttributeModifier(uUID, "Armor toughness", (double)this.toughness, AttributeModifier.Operation.ADDITION));
        if (material == EnumArmorMaterial.NETHERITE) {
            builder.put(GenericAttributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uUID, "Armor knockback resistance", (double)this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }

        this.defaultModifiers = builder.build();
    }

    public EnumItemSlot getSlot() {
        return this.slot;
    }

    @Override
    public int getEnchantmentValue() {
        return this.material.getEnchantmentValue();
    }

    public ArmorMaterial getMaterial() {
        return this.material;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return this.material.getRepairIngredient().test(ingredient) || super.isValidRepairItem(stack, ingredient);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(itemStack);
        ItemStack itemStack2 = user.getEquipment(equipmentSlot);
        if (itemStack2.isEmpty()) {
            user.setSlot(equipmentSlot, itemStack.cloneItemStack());
            if (!world.isClientSide()) {
                user.awardStat(StatisticList.ITEM_USED.get(this));
            }

            itemStack.setCount(0);
            return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
        } else {
            return InteractionResultWrapper.fail(itemStack);
        }
    }

    @Override
    public Multimap<AttributeBase, AttributeModifier> getDefaultAttributeModifiers(EnumItemSlot slot) {
        return slot == this.slot ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public int getDefense() {
        return this.defense;
    }

    public float getToughness() {
        return this.toughness;
    }

    @Nullable
    @Override
    public SoundEffect getEquipSound() {
        return this.getMaterial().getEquipSound();
    }
}
