package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class PotionUtil {
    public static final String TAG_CUSTOM_POTION_EFFECTS = "CustomPotionEffects";
    public static final String TAG_CUSTOM_POTION_COLOR = "CustomPotionColor";
    public static final String TAG_POTION = "Potion";
    private static final int EMPTY_COLOR = 16253176;
    private static final IChatBaseComponent NO_EFFECT = (new ChatMessage("effect.none")).withStyle(EnumChatFormat.GRAY);

    public static List<MobEffect> getEffects(ItemStack stack) {
        return getAllEffects(stack.getTag());
    }

    public static List<MobEffect> getAllEffects(PotionRegistry potion, Collection<MobEffect> custom) {
        List<MobEffect> list = Lists.newArrayList();
        list.addAll(potion.getEffects());
        list.addAll(custom);
        return list;
    }

    public static List<MobEffect> getAllEffects(@Nullable NBTTagCompound nbt) {
        List<MobEffect> list = Lists.newArrayList();
        list.addAll(getPotion(nbt).getEffects());
        getCustomEffects(nbt, list);
        return list;
    }

    public static List<MobEffect> getCustomEffects(ItemStack stack) {
        return getCustomEffects(stack.getTag());
    }

    public static List<MobEffect> getCustomEffects(@Nullable NBTTagCompound nbt) {
        List<MobEffect> list = Lists.newArrayList();
        getCustomEffects(nbt, list);
        return list;
    }

    public static void getCustomEffects(@Nullable NBTTagCompound nbt, List<MobEffect> list) {
        if (nbt != null && nbt.hasKeyOfType("CustomPotionEffects", 9)) {
            NBTTagList listTag = nbt.getList("CustomPotionEffects", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                MobEffect mobEffectInstance = MobEffect.load(compoundTag);
                if (mobEffectInstance != null) {
                    list.add(mobEffectInstance);
                }
            }
        }

    }

    public static int getColor(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("CustomPotionColor", 99)) {
            return compoundTag.getInt("CustomPotionColor");
        } else {
            return getPotion(stack) == Potions.EMPTY ? 16253176 : getColor(getEffects(stack));
        }
    }

    public static int getColor(PotionRegistry potion) {
        return potion == Potions.EMPTY ? 16253176 : getColor(potion.getEffects());
    }

    public static int getColor(Collection<MobEffect> effects) {
        int i = 3694022;
        if (effects.isEmpty()) {
            return 3694022;
        } else {
            float f = 0.0F;
            float g = 0.0F;
            float h = 0.0F;
            int j = 0;

            for(MobEffect mobEffectInstance : effects) {
                if (mobEffectInstance.isShowParticles()) {
                    int k = mobEffectInstance.getMobEffect().getColor();
                    int l = mobEffectInstance.getAmplifier() + 1;
                    f += (float)(l * (k >> 16 & 255)) / 255.0F;
                    g += (float)(l * (k >> 8 & 255)) / 255.0F;
                    h += (float)(l * (k >> 0 & 255)) / 255.0F;
                    j += l;
                }
            }

            if (j == 0) {
                return 0;
            } else {
                f = f / (float)j * 255.0F;
                g = g / (float)j * 255.0F;
                h = h / (float)j * 255.0F;
                return (int)f << 16 | (int)g << 8 | (int)h;
            }
        }
    }

    public static PotionRegistry getPotion(ItemStack stack) {
        return getPotion(stack.getTag());
    }

    public static PotionRegistry getPotion(@Nullable NBTTagCompound compound) {
        return compound == null ? Potions.EMPTY : PotionRegistry.byName(compound.getString("Potion"));
    }

    public static ItemStack setPotion(ItemStack stack, PotionRegistry potion) {
        MinecraftKey resourceLocation = IRegistry.POTION.getKey(potion);
        if (potion == Potions.EMPTY) {
            stack.removeTag("Potion");
        } else {
            stack.getOrCreateTag().setString("Potion", resourceLocation.toString());
        }

        return stack;
    }

    public static ItemStack setCustomEffects(ItemStack stack, Collection<MobEffect> effects) {
        if (effects.isEmpty()) {
            return stack;
        } else {
            NBTTagCompound compoundTag = stack.getOrCreateTag();
            NBTTagList listTag = compoundTag.getList("CustomPotionEffects", 9);

            for(MobEffect mobEffectInstance : effects) {
                listTag.add(mobEffectInstance.save(new NBTTagCompound()));
            }

            compoundTag.set("CustomPotionEffects", listTag);
            return stack;
        }
    }

    public static void addPotionTooltip(ItemStack stack, List<IChatBaseComponent> list, float f) {
        List<MobEffect> list2 = getEffects(stack);
        List<Pair<AttributeBase, AttributeModifier>> list3 = Lists.newArrayList();
        if (list2.isEmpty()) {
            list.add(NO_EFFECT);
        } else {
            for(MobEffect mobEffectInstance : list2) {
                IChatMutableComponent mutableComponent = new ChatMessage(mobEffectInstance.getDescriptionId());
                MobEffectBase mobEffect = mobEffectInstance.getMobEffect();
                Map<AttributeBase, AttributeModifier> map = mobEffect.getAttributeModifiers();
                if (!map.isEmpty()) {
                    for(Entry<AttributeBase, AttributeModifier> entry : map.entrySet()) {
                        AttributeModifier attributeModifier = entry.getValue();
                        AttributeModifier attributeModifier2 = new AttributeModifier(attributeModifier.getName(), mobEffect.getAttributeModifierValue(mobEffectInstance.getAmplifier(), attributeModifier), attributeModifier.getOperation());
                        list3.add(new Pair<>(entry.getKey(), attributeModifier2));
                    }
                }

                if (mobEffectInstance.getAmplifier() > 0) {
                    mutableComponent = new ChatMessage("potion.withAmplifier", mutableComponent, new ChatMessage("potion.potency." + mobEffectInstance.getAmplifier()));
                }

                if (mobEffectInstance.getDuration() > 20) {
                    mutableComponent = new ChatMessage("potion.withDuration", mutableComponent, MobEffectUtil.formatDuration(mobEffectInstance, f));
                }

                list.add(mutableComponent.withStyle(mobEffect.getCategory().getTooltipFormatting()));
            }
        }

        if (!list3.isEmpty()) {
            list.add(ChatComponentText.EMPTY);
            list.add((new ChatMessage("potion.whenDrank")).withStyle(EnumChatFormat.DARK_PURPLE));

            for(Pair<AttributeBase, AttributeModifier> pair : list3) {
                AttributeModifier attributeModifier3 = pair.getSecond();
                double d = attributeModifier3.getAmount();
                double g;
                if (attributeModifier3.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributeModifier3.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    g = attributeModifier3.getAmount();
                } else {
                    g = attributeModifier3.getAmount() * 100.0D;
                }

                if (d > 0.0D) {
                    list.add((new ChatMessage("attribute.modifier.plus." + attributeModifier3.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(g), new ChatMessage(pair.getFirst().getName()))).withStyle(EnumChatFormat.BLUE));
                } else if (d < 0.0D) {
                    g = g * -1.0D;
                    list.add((new ChatMessage("attribute.modifier.take." + attributeModifier3.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(g), new ChatMessage(pair.getFirst().getName()))).withStyle(EnumChatFormat.RED));
                }
            }
        }

    }
}
