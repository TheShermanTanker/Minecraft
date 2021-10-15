package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class ItemTrident extends Item implements ItemVanishable {
    public static final int THROW_THRESHOLD_TIME = 10;
    public static final float BASE_DAMAGE = 8.0F;
    public static final float SHOOT_POWER = 2.5F;
    private final Multimap<AttributeBase, AttributeModifier> defaultModifiers;

    public ItemTrident(Item.Info settings) {
        super(settings);
        Builder<AttributeBase, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(GenericAttributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 8.0D, AttributeModifier.Operation.ADDITION));
        builder.put(GenericAttributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double)-2.9F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public boolean canAttackBlock(IBlockData state, World world, BlockPosition pos, EntityHuman miner) {
        return !miner.isCreative();
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, EntityLiving user, int remainingUseTicks) {
        if (user instanceof EntityHuman) {
            EntityHuman player = (EntityHuman)user;
            int i = this.getUseDuration(stack) - remainingUseTicks;
            if (i >= 10) {
                int j = EnchantmentManager.getRiptide(stack);
                if (j <= 0 || player.isInWaterOrRain()) {
                    if (!world.isClientSide) {
                        stack.damage(1, player, (p) -> {
                            p.broadcastItemBreak(user.getRaisedHand());
                        });
                        if (j == 0) {
                            EntityThrownTrident thrownTrident = new EntityThrownTrident(world, player, stack);
                            thrownTrident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F + (float)j * 0.5F, 1.0F);
                            if (player.getAbilities().instabuild) {
                                thrownTrident.pickup = EntityArrow.PickupStatus.CREATIVE_ONLY;
                            }

                            world.addEntity(thrownTrident);
                            world.playSound((EntityHuman)null, thrownTrident, SoundEffects.TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            if (!player.getAbilities().instabuild) {
                                player.getInventory().removeItem(stack);
                            }
                        }
                    }

                    player.awardStat(StatisticList.ITEM_USED.get(this));
                    if (j > 0) {
                        float f = player.getYRot();
                        float g = player.getXRot();
                        float h = -MathHelper.sin(f * ((float)Math.PI / 180F)) * MathHelper.cos(g * ((float)Math.PI / 180F));
                        float k = -MathHelper.sin(g * ((float)Math.PI / 180F));
                        float l = MathHelper.cos(f * ((float)Math.PI / 180F)) * MathHelper.cos(g * ((float)Math.PI / 180F));
                        float m = MathHelper.sqrt(h * h + k * k + l * l);
                        float n = 3.0F * ((1.0F + (float)j) / 4.0F);
                        h = h * (n / m);
                        k = k * (n / m);
                        l = l * (n / m);
                        player.push((double)h, (double)k, (double)l);
                        player.startAutoSpinAttack(20);
                        if (player.isOnGround()) {
                            float o = 1.1999999F;
                            player.move(EnumMoveType.SELF, new Vec3D(0.0D, (double)1.1999999F, 0.0D));
                        }

                        SoundEffect soundEvent;
                        if (j >= 3) {
                            soundEvent = SoundEffects.TRIDENT_RIPTIDE_3;
                        } else if (j == 2) {
                            soundEvent = SoundEffects.TRIDENT_RIPTIDE_2;
                        } else {
                            soundEvent = SoundEffects.TRIDENT_RIPTIDE_1;
                        }

                        world.playSound((EntityHuman)null, player, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    }

                }
            }
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return InteractionResultWrapper.fail(itemStack);
        } else if (EnchantmentManager.getRiptide(itemStack) > 0 && !user.isInWaterOrRain()) {
            return InteractionResultWrapper.fail(itemStack);
        } else {
            user.startUsingItem(hand);
            return InteractionResultWrapper.consume(itemStack);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.damage(1, attacker, (e) -> {
            e.broadcastItemBreak(EnumItemSlot.MAINHAND);
        });
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if ((double)state.getDestroySpeed(world, pos) != 0.0D) {
            stack.damage(2, miner, (e) -> {
                e.broadcastItemBreak(EnumItemSlot.MAINHAND);
            });
        }

        return true;
    }

    @Override
    public Multimap<AttributeBase, AttributeModifier> getDefaultAttributeModifiers(EnumItemSlot slot) {
        return slot == EnumItemSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
