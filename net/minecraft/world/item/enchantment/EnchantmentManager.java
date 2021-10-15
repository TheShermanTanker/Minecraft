package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.MathHelper;
import net.minecraft.util.random.WeightedRandom2;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;

public class EnchantmentManager {
    private static final String TAG_ENCH_ID = "id";
    private static final String TAG_ENCH_LEVEL = "lvl";

    public static NBTTagCompound storeEnchantment(@Nullable MinecraftKey id, int lvl) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("id", String.valueOf((Object)id));
        compoundTag.setShort("lvl", (short)lvl);
        return compoundTag;
    }

    public static void setEnchantmentLevel(NBTTagCompound nbt, int lvl) {
        nbt.setShort("lvl", (short)lvl);
    }

    public static int getEnchantmentLevel(NBTTagCompound nbt) {
        return MathHelper.clamp(nbt.getInt("lvl"), 0, 255);
    }

    @Nullable
    public static MinecraftKey getEnchantmentId(NBTTagCompound nbt) {
        return MinecraftKey.tryParse(nbt.getString("id"));
    }

    @Nullable
    public static MinecraftKey getEnchantmentId(Enchantment enchantment) {
        return IRegistry.ENCHANTMENT.getKey(enchantment);
    }

    public static int getEnchantmentLevel(Enchantment enchantment, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        } else {
            MinecraftKey resourceLocation = getEnchantmentId(enchantment);
            NBTTagList listTag = stack.getEnchantments();

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                MinecraftKey resourceLocation2 = getEnchantmentId(compoundTag);
                if (resourceLocation2 != null && resourceLocation2.equals(resourceLocation)) {
                    return getEnchantmentLevel(compoundTag);
                }
            }

            return 0;
        }
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        NBTTagList listTag = stack.is(Items.ENCHANTED_BOOK) ? ItemEnchantedBook.getEnchantments(stack) : stack.getEnchantments();
        return deserializeEnchantments(listTag);
    }

    public static Map<Enchantment, Integer> deserializeEnchantments(NBTTagList list) {
        Map<Enchantment, Integer> map = Maps.newLinkedHashMap();

        for(int i = 0; i < list.size(); ++i) {
            NBTTagCompound compoundTag = list.getCompound(i);
            IRegistry.ENCHANTMENT.getOptional(getEnchantmentId(compoundTag)).ifPresent((enchantment) -> {
                map.put(enchantment, getEnchantmentLevel(compoundTag));
            });
        }

        return map;
    }

    public static void setEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
        NBTTagList listTag = new NBTTagList();

        for(Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                int i = entry.getValue();
                listTag.add(storeEnchantment(getEnchantmentId(enchantment), i));
                if (stack.is(Items.ENCHANTED_BOOK)) {
                    ItemEnchantedBook.addEnchantment(stack, new WeightedRandomEnchant(enchantment, i));
                }
            }
        }

        if (listTag.isEmpty()) {
            stack.removeTag("Enchantments");
        } else if (!stack.is(Items.ENCHANTED_BOOK)) {
            stack.addTagElement("Enchantments", listTag);
        }

    }

    private static void runIterationOnItem(EnchantmentManager.EnchantmentVisitor consumer, ItemStack stack) {
        if (!stack.isEmpty()) {
            NBTTagList listTag = stack.getEnchantments();

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                IRegistry.ENCHANTMENT.getOptional(getEnchantmentId(compoundTag)).ifPresent((enchantment) -> {
                    consumer.accept(enchantment, getEnchantmentLevel(compoundTag));
                });
            }

        }
    }

    private static void runIterationOnInventory(EnchantmentManager.EnchantmentVisitor consumer, Iterable<ItemStack> stacks) {
        for(ItemStack itemStack : stacks) {
            runIterationOnItem(consumer, itemStack);
        }

    }

    public static int getDamageProtection(Iterable<ItemStack> equipment, DamageSource source) {
        MutableInt mutableInt = new MutableInt();
        runIterationOnInventory((enchantment, level) -> {
            mutableInt.add(enchantment.getDamageProtection(level, source));
        }, equipment);
        return mutableInt.intValue();
    }

    public static float getDamageBonus(ItemStack stack, EnumMonsterType group) {
        MutableFloat mutableFloat = new MutableFloat();
        runIterationOnItem((enchantment, level) -> {
            mutableFloat.add(enchantment.getDamageBonus(level, group));
        }, stack);
        return mutableFloat.floatValue();
    }

    public static float getSweepingDamageRatio(EntityLiving entity) {
        int i = getEnchantmentLevel(Enchantments.SWEEPING_EDGE, entity);
        return i > 0 ? EnchantmentSweeping.getSweepingDamageRatio(i) : 0.0F;
    }

    public static void doPostHurtEffects(EntityLiving user, Entity attacker) {
        EnchantmentManager.EnchantmentVisitor enchantmentVisitor = (enchantment, level) -> {
            enchantment.doPostHurt(user, attacker, level);
        };
        if (user != null) {
            runIterationOnInventory(enchantmentVisitor, user.getAllSlots());
        }

        if (attacker instanceof EntityHuman) {
            runIterationOnItem(enchantmentVisitor, user.getItemInMainHand());
        }

    }

    public static void doPostDamageEffects(EntityLiving user, Entity target) {
        EnchantmentManager.EnchantmentVisitor enchantmentVisitor = (enchantment, level) -> {
            enchantment.doPostAttack(user, target, level);
        };
        if (user != null) {
            runIterationOnInventory(enchantmentVisitor, user.getAllSlots());
        }

        if (user instanceof EntityHuman) {
            runIterationOnItem(enchantmentVisitor, user.getItemInMainHand());
        }

    }

    public static int getEnchantmentLevel(Enchantment enchantment, EntityLiving entity) {
        Iterable<ItemStack> iterable = enchantment.getSlotItems(entity).values();
        if (iterable == null) {
            return 0;
        } else {
            int i = 0;

            for(ItemStack itemStack : iterable) {
                int j = getEnchantmentLevel(enchantment, itemStack);
                if (j > i) {
                    i = j;
                }
            }

            return i;
        }
    }

    public static int getKnockbackBonus(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.KNOCKBACK, entity);
    }

    public static int getFireAspectEnchantmentLevel(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.FIRE_ASPECT, entity);
    }

    public static int getOxygenEnchantmentLevel(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.RESPIRATION, entity);
    }

    public static int getDepthStrider(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.DEPTH_STRIDER, entity);
    }

    public static int getDigSpeedEnchantmentLevel(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, entity);
    }

    public static int getFishingLuckBonus(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.FISHING_LUCK, stack);
    }

    public static int getFishingSpeedBonus(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.FISHING_SPEED, stack);
    }

    public static int getMobLooting(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.MOB_LOOTING, entity);
    }

    public static boolean hasAquaAffinity(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.AQUA_AFFINITY, entity) > 0;
    }

    public static boolean hasFrostWalker(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.FROST_WALKER, entity) > 0;
    }

    public static boolean hasSoulSpeed(EntityLiving entity) {
        return getEnchantmentLevel(Enchantments.SOUL_SPEED, entity) > 0;
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.BINDING_CURSE, stack) > 0;
    }

    public static boolean shouldNotDrop(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack) > 0;
    }

    public static int getLoyalty(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.LOYALTY, stack);
    }

    public static int getRiptide(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.RIPTIDE, stack);
    }

    public static boolean hasChanneling(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.CHANNELING, stack) > 0;
    }

    @Nullable
    public static Entry<EnumItemSlot, ItemStack> getRandomItemWith(Enchantment enchantment, EntityLiving entity) {
        return getRandomItemWith(enchantment, entity, (stack) -> {
            return true;
        });
    }

    @Nullable
    public static Entry<EnumItemSlot, ItemStack> getRandomItemWith(Enchantment enchantment, EntityLiving entity, Predicate<ItemStack> condition) {
        Map<EnumItemSlot, ItemStack> map = enchantment.getSlotItems(entity);
        if (map.isEmpty()) {
            return null;
        } else {
            List<Entry<EnumItemSlot, ItemStack>> list = Lists.newArrayList();

            for(Entry<EnumItemSlot, ItemStack> entry : map.entrySet()) {
                ItemStack itemStack = entry.getValue();
                if (!itemStack.isEmpty() && getEnchantmentLevel(enchantment, itemStack) > 0 && condition.test(itemStack)) {
                    list.add(entry);
                }
            }

            return list.isEmpty() ? null : list.get(entity.getRandom().nextInt(list.size()));
        }
    }

    public static int getEnchantmentCost(Random random, int slotIndex, int bookshelfCount, ItemStack stack) {
        Item item = stack.getItem();
        int i = item.getEnchantmentValue();
        if (i <= 0) {
            return 0;
        } else {
            if (bookshelfCount > 15) {
                bookshelfCount = 15;
            }

            int j = random.nextInt(8) + 1 + (bookshelfCount >> 1) + random.nextInt(bookshelfCount + 1);
            if (slotIndex == 0) {
                return Math.max(j / 3, 1);
            } else {
                return slotIndex == 1 ? j * 2 / 3 + 1 : Math.max(j, bookshelfCount * 2);
            }
        }
    }

    public static ItemStack enchantItem(Random random, ItemStack target, int level, boolean treasureAllowed) {
        List<WeightedRandomEnchant> list = selectEnchantment(random, target, level, treasureAllowed);
        boolean bl = target.is(Items.BOOK);
        if (bl) {
            target = new ItemStack(Items.ENCHANTED_BOOK);
        }

        for(WeightedRandomEnchant enchantmentInstance : list) {
            if (bl) {
                ItemEnchantedBook.addEnchantment(target, enchantmentInstance);
            } else {
                target.addEnchantment(enchantmentInstance.enchantment, enchantmentInstance.level);
            }
        }

        return target;
    }

    public static List<WeightedRandomEnchant> selectEnchantment(Random random, ItemStack stack, int level, boolean treasureAllowed) {
        List<WeightedRandomEnchant> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantmentValue();
        if (i <= 0) {
            return list;
        } else {
            level = level + 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
            float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
            level = MathHelper.clamp(Math.round((float)level + (float)level * f), 1, Integer.MAX_VALUE);
            List<WeightedRandomEnchant> list2 = getAvailableEnchantmentResults(level, stack, treasureAllowed);
            if (!list2.isEmpty()) {
                WeightedRandom2.getRandomItem(random, list2).ifPresent(list::add);

                while(random.nextInt(50) <= level) {
                    if (!list.isEmpty()) {
                        filterCompatibleEnchantments(list2, SystemUtils.lastOf(list));
                    }

                    if (list2.isEmpty()) {
                        break;
                    }

                    WeightedRandom2.getRandomItem(random, list2).ifPresent(list::add);
                    level /= 2;
                }
            }

            return list;
        }
    }

    public static void filterCompatibleEnchantments(List<WeightedRandomEnchant> possibleEntries, WeightedRandomEnchant pickedEntry) {
        Iterator<WeightedRandomEnchant> iterator = possibleEntries.iterator();

        while(iterator.hasNext()) {
            if (!pickedEntry.enchantment.isCompatible((iterator.next()).enchantment)) {
                iterator.remove();
            }
        }

    }

    public static boolean isEnchantmentCompatible(Collection<Enchantment> existing, Enchantment candidate) {
        for(Enchantment enchantment : existing) {
            if (!enchantment.isCompatible(candidate)) {
                return false;
            }
        }

        return true;
    }

    public static List<WeightedRandomEnchant> getAvailableEnchantmentResults(int power, ItemStack stack, boolean treasureAllowed) {
        List<WeightedRandomEnchant> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean bl = stack.is(Items.BOOK);

        for(Enchantment enchantment : IRegistry.ENCHANTMENT) {
            if ((!enchantment.isTreasure() || treasureAllowed) && enchantment.isDiscoverable() && (enchantment.category.canEnchant(item) || bl)) {
                for(int i = enchantment.getMaxLevel(); i > enchantment.getStartLevel() - 1; --i) {
                    if (power >= enchantment.getMinCost(i) && power <= enchantment.getMaxCost(i)) {
                        list.add(new WeightedRandomEnchant(enchantment, i));
                        break;
                    }
                }
            }
        }

        return list;
    }

    @FunctionalInterface
    interface EnchantmentVisitor {
        void accept(Enchantment enchantment, int level);
    }
}
