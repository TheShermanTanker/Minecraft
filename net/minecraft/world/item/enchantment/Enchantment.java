package net.minecraft.world.item.enchantment;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.item.ItemStack;

public abstract class Enchantment {
    public final EnumItemSlot[] slots;
    private final Enchantment.Rarity rarity;
    public final EnchantmentSlotType category;
    @Nullable
    protected String descriptionId;

    @Nullable
    public static Enchantment byId(int id) {
        return IRegistry.ENCHANTMENT.fromId(id);
    }

    protected Enchantment(Enchantment.Rarity weight, EnchantmentSlotType type, EnumItemSlot[] slotTypes) {
        this.rarity = weight;
        this.category = type;
        this.slots = slotTypes;
    }

    public Map<EnumItemSlot, ItemStack> getSlotItems(EntityLiving entity) {
        Map<EnumItemSlot, ItemStack> map = Maps.newEnumMap(EnumItemSlot.class);

        for(EnumItemSlot equipmentSlot : this.slots) {
            ItemStack itemStack = entity.getEquipment(equipmentSlot);
            if (!itemStack.isEmpty()) {
                map.put(equipmentSlot, itemStack);
            }
        }

        return map;
    }

    public Enchantment.Rarity getRarity() {
        return this.rarity;
    }

    public int getStartLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 1;
    }

    public int getMinCost(int level) {
        return 1 + level * 10;
    }

    public int getMaxCost(int level) {
        return this.getMinCost(level) + 5;
    }

    public int getDamageProtection(int level, DamageSource source) {
        return 0;
    }

    public float getDamageBonus(int level, EnumMonsterType group) {
        return 0.0F;
    }

    public final boolean isCompatible(Enchantment other) {
        return this.checkCompatibility(other) && other.checkCompatibility(this);
    }

    protected boolean checkCompatibility(Enchantment other) {
        return this != other;
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.makeDescriptionId("enchantment", IRegistry.ENCHANTMENT.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public IChatBaseComponent getFullname(int level) {
        IChatMutableComponent mutableComponent = new ChatMessage(this.getDescriptionId());
        if (this.isCurse()) {
            mutableComponent.withStyle(EnumChatFormat.RED);
        } else {
            mutableComponent.withStyle(EnumChatFormat.GRAY);
        }

        if (level != 1 || this.getMaxLevel() != 1) {
            mutableComponent.append(" ").addSibling(new ChatMessage("enchantment.level." + level));
        }

        return mutableComponent;
    }

    public boolean canEnchant(ItemStack stack) {
        return this.category.canEnchant(stack.getItem());
    }

    public void doPostAttack(EntityLiving user, Entity target, int level) {
    }

    public void doPostHurt(EntityLiving user, Entity attacker, int level) {
    }

    public boolean isTreasure() {
        return false;
    }

    public boolean isCurse() {
        return false;
    }

    public boolean isTradeable() {
        return true;
    }

    public boolean isDiscoverable() {
        return true;
    }

    public static enum Rarity {
        COMMON(10),
        UNCOMMON(5),
        RARE(2),
        VERY_RARE(1);

        private final int weight;

        private Rarity(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight;
        }
    }
}
