package net.minecraft.world.item;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemSuspiciousStew extends Item {
    public static final String EFFECTS_TAG = "Effects";
    public static final String EFFECT_ID_TAG = "EffectId";
    public static final String EFFECT_DURATION_TAG = "EffectDuration";

    public ItemSuspiciousStew(Item.Info settings) {
        super(settings);
    }

    public static void saveMobEffect(ItemStack stew, MobEffectList effect, int duration) {
        NBTTagCompound compoundTag = stew.getOrCreateTag();
        NBTTagList listTag = compoundTag.getList("Effects", 9);
        NBTTagCompound compoundTag2 = new NBTTagCompound();
        compoundTag2.setByte("EffectId", (byte)MobEffectList.getId(effect));
        compoundTag2.setInt("EffectDuration", duration);
        listTag.add(compoundTag2);
        compoundTag.set("Effects", listTag);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        ItemStack itemStack = super.finishUsingItem(stack, world, user);
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("Effects", 9)) {
            NBTTagList listTag = compoundTag.getList("Effects", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                int j = 160;
                NBTTagCompound compoundTag2 = listTag.getCompound(i);
                if (compoundTag2.hasKeyOfType("EffectDuration", 3)) {
                    j = compoundTag2.getInt("EffectDuration");
                }

                MobEffectList mobEffect = MobEffectList.fromId(compoundTag2.getByte("EffectId"));
                if (mobEffect != null) {
                    user.addEffect(new MobEffect(mobEffect, j));
                }
            }
        }

        return user instanceof EntityHuman && ((EntityHuman)user).getAbilities().instabuild ? itemStack : new ItemStack(Items.BOWL);
    }
}
