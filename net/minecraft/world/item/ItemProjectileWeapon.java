package net.minecraft.world.item;

import java.util.function.Predicate;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLiving;

public abstract class ItemProjectileWeapon extends Item {
    public static final Predicate<ItemStack> ARROW_ONLY = (stack) -> {
        return stack.is(TagsItem.ARROWS);
    };
    public static final Predicate<ItemStack> ARROW_OR_FIREWORK = ARROW_ONLY.or((stack) -> {
        return stack.is(Items.FIREWORK_ROCKET);
    });

    public ItemProjectileWeapon(Item.Info settings) {
        super(settings);
    }

    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return this.getAllSupportedProjectiles();
    }

    public abstract Predicate<ItemStack> getAllSupportedProjectiles();

    public static ItemStack getHeldProjectile(EntityLiving entity, Predicate<ItemStack> predicate) {
        if (predicate.test(entity.getItemInHand(EnumHand.OFF_HAND))) {
            return entity.getItemInHand(EnumHand.OFF_HAND);
        } else {
            return predicate.test(entity.getItemInHand(EnumHand.MAIN_HAND)) ? entity.getItemInHand(EnumHand.MAIN_HAND) : ItemStack.EMPTY;
        }
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    public abstract int getDefaultProjectileRange();
}
