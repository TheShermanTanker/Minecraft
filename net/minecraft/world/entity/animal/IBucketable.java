package net.minecraft.world.entity.animal;

import java.util.Optional;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public interface IBucketable {
    boolean isFromBucket();

    void setFromBucket(boolean fromBucket);

    void setBucketName(ItemStack stack);

    void loadFromBucketTag(NBTTagCompound nbt);

    ItemStack getBucketItem();

    SoundEffect getPickupSound();

    @Deprecated
    static void saveDefaultDataToBucketTag(EntityInsentient entity, ItemStack stack) {
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        if (entity.hasCustomName()) {
            stack.setHoverName(entity.getCustomName());
        }

        if (entity.isNoAI()) {
            compoundTag.setBoolean("NoAI", entity.isNoAI());
        }

        if (entity.isSilent()) {
            compoundTag.setBoolean("Silent", entity.isSilent());
        }

        if (entity.isNoGravity()) {
            compoundTag.setBoolean("NoGravity", entity.isNoGravity());
        }

        if (entity.hasGlowingTag()) {
            compoundTag.setBoolean("Glowing", entity.hasGlowingTag());
        }

        if (entity.isInvulnerable()) {
            compoundTag.setBoolean("Invulnerable", entity.isInvulnerable());
        }

        compoundTag.setFloat("Health", entity.getHealth());
    }

    @Deprecated
    static void loadDefaultDataFromBucketTag(EntityInsentient entity, NBTTagCompound nbt) {
        if (nbt.hasKey("NoAI")) {
            entity.setNoAI(nbt.getBoolean("NoAI"));
        }

        if (nbt.hasKey("Silent")) {
            entity.setSilent(nbt.getBoolean("Silent"));
        }

        if (nbt.hasKey("NoGravity")) {
            entity.setNoGravity(nbt.getBoolean("NoGravity"));
        }

        if (nbt.hasKey("Glowing")) {
            entity.setGlowingTag(nbt.getBoolean("Glowing"));
        }

        if (nbt.hasKey("Invulnerable")) {
            entity.setInvulnerable(nbt.getBoolean("Invulnerable"));
        }

        if (nbt.hasKeyOfType("Health", 99)) {
            entity.setHealth(nbt.getFloat("Health"));
        }

    }

    static <T extends EntityLiving & IBucketable> Optional<EnumInteractionResult> bucketMobPickup(EntityHuman player, EnumHand hand, T entity) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.WATER_BUCKET && entity.isAlive()) {
            entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack itemStack2 = entity.getBucketItem();
            entity.setBucketName(itemStack2);
            ItemStack itemStack3 = ItemLiquidUtil.createFilledResult(itemStack, player, itemStack2, false);
            player.setItemInHand(hand, itemStack3);
            World level = entity.level;
            if (!level.isClientSide) {
                CriterionTriggers.FILLED_BUCKET.trigger((EntityPlayer)player, itemStack2);
            }

            entity.die();
            return Optional.of(EnumInteractionResult.sidedSuccess(level.isClientSide));
        } else {
            return Optional.empty();
        }
    }
}
