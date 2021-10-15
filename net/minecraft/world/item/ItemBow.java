package net.minecraft.world.item;

import java.util.function.Predicate;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;

public class ItemBow extends ItemProjectileWeapon implements ItemVanishable {
    public static final int MAX_DRAW_DURATION = 20;
    public static final int DEFAULT_RANGE = 15;

    public ItemBow(Item.Info settings) {
        super(settings);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, EntityLiving user, int remainingUseTicks) {
        if (user instanceof EntityHuman) {
            EntityHuman player = (EntityHuman)user;
            boolean bl = player.getAbilities().instabuild || EnchantmentManager.getEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
            ItemStack itemStack = player.getProjectile(stack);
            if (!itemStack.isEmpty() || bl) {
                if (itemStack.isEmpty()) {
                    itemStack = new ItemStack(Items.ARROW);
                }

                int i = this.getUseDuration(stack) - remainingUseTicks;
                float f = getPowerForTime(i);
                if (!((double)f < 0.1D)) {
                    boolean bl2 = bl && itemStack.is(Items.ARROW);
                    if (!world.isClientSide) {
                        ItemArrow arrowItem = (ItemArrow)(itemStack.getItem() instanceof ItemArrow ? itemStack.getItem() : Items.ARROW);
                        EntityArrow abstractArrow = arrowItem.createArrow(world, itemStack, player);
                        abstractArrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F, 1.0F);
                        if (f == 1.0F) {
                            abstractArrow.setCritical(true);
                        }

                        int j = EnchantmentManager.getEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                        if (j > 0) {
                            abstractArrow.setDamage(abstractArrow.getDamage() + (double)j * 0.5D + 0.5D);
                        }

                        int k = EnchantmentManager.getEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                        if (k > 0) {
                            abstractArrow.setKnockbackStrength(k);
                        }

                        if (EnchantmentManager.getEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
                            abstractArrow.setOnFire(100);
                        }

                        stack.damage(1, player, (p) -> {
                            p.broadcastItemBreak(player.getRaisedHand());
                        });
                        if (bl2 || player.getAbilities().instabuild && (itemStack.is(Items.SPECTRAL_ARROW) || itemStack.is(Items.TIPPED_ARROW))) {
                            abstractArrow.pickup = EntityArrow.PickupStatus.CREATIVE_ONLY;
                        }

                        world.addEntity(abstractArrow);
                    }

                    world.playSound((EntityHuman)null, player.locX(), player.locY(), player.locZ(), SoundEffects.ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                    if (!bl2 && !player.getAbilities().instabuild) {
                        itemStack.subtract(1);
                        if (itemStack.isEmpty()) {
                            player.getInventory().removeItem(itemStack);
                        }
                    }

                    player.awardStat(StatisticList.ITEM_USED.get(this));
                }
            }
        }
    }

    public static float getPowerForTime(int useTicks) {
        float f = (float)useTicks / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.BOW;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        boolean bl = !user.getProjectile(itemStack).isEmpty();
        if (!user.getAbilities().instabuild && !bl) {
            return InteractionResultWrapper.fail(itemStack);
        } else {
            user.startUsingItem(hand);
            return InteractionResultWrapper.consume(itemStack);
        }
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }
}
