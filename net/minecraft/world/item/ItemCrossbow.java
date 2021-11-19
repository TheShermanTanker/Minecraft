package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3fa;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.ICrossbow;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class ItemCrossbow extends ItemProjectileWeapon implements ItemVanishable {
    private static final String TAG_CHARGED = "Charged";
    private static final String TAG_CHARGED_PROJECTILES = "ChargedProjectiles";
    private static final int MAX_CHARGE_DURATION = 25;
    public static final int DEFAULT_RANGE = 8;
    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2F;
    private static final float MID_SOUND_PERCENT = 0.5F;
    private static final float ARROW_POWER = 3.15F;
    private static final float FIREWORK_POWER = 1.6F;

    public ItemCrossbow(Item.Info settings) {
        super(settings);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (isCharged(itemStack)) {
            performShooting(world, user, hand, itemStack, getShootingPower(itemStack), 1.0F);
            setCharged(itemStack, false);
            return InteractionResultWrapper.consume(itemStack);
        } else if (!user.getProjectile(itemStack).isEmpty()) {
            if (!isCharged(itemStack)) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
                user.startUsingItem(hand);
            }

            return InteractionResultWrapper.consume(itemStack);
        } else {
            return InteractionResultWrapper.fail(itemStack);
        }
    }

    private static float getShootingPower(ItemStack stack) {
        return containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, EntityLiving user, int remainingUseTicks) {
        int i = this.getUseDuration(stack) - remainingUseTicks;
        float f = getPowerForTime(i, stack);
        if (f >= 1.0F && !isCharged(stack) && tryLoadProjectiles(user, stack)) {
            setCharged(stack, true);
            EnumSoundCategory soundSource = user instanceof EntityHuman ? EnumSoundCategory.PLAYERS : EnumSoundCategory.HOSTILE;
            world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.CROSSBOW_LOADING_END, soundSource, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }

    }

    private static boolean tryLoadProjectiles(EntityLiving shooter, ItemStack projectile) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.MULTISHOT, projectile);
        int j = i == 0 ? 1 : 3;
        boolean bl = shooter instanceof EntityHuman && ((EntityHuman)shooter).getAbilities().instabuild;
        ItemStack itemStack = shooter.getProjectile(projectile);
        ItemStack itemStack2 = itemStack.cloneItemStack();

        for(int k = 0; k < j; ++k) {
            if (k > 0) {
                itemStack = itemStack2.cloneItemStack();
            }

            if (itemStack.isEmpty() && bl) {
                itemStack = new ItemStack(Items.ARROW);
                itemStack2 = itemStack.cloneItemStack();
            }

            if (!loadProjectile(shooter, projectile, itemStack, k > 0, bl)) {
                return false;
            }
        }

        return true;
    }

    private static boolean loadProjectile(EntityLiving shooter, ItemStack crossbow, ItemStack projectile, boolean simulated, boolean creative) {
        if (projectile.isEmpty()) {
            return false;
        } else {
            boolean bl = creative && projectile.getItem() instanceof ItemArrow;
            ItemStack itemStack;
            if (!bl && !creative && !simulated) {
                itemStack = projectile.cloneAndSubtract(1);
                if (projectile.isEmpty() && shooter instanceof EntityHuman) {
                    ((EntityHuman)shooter).getInventory().removeItem(projectile);
                }
            } else {
                itemStack = projectile.cloneItemStack();
            }

            addChargedProjectile(crossbow, itemStack);
            return true;
        }
    }

    public static boolean isCharged(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.getBoolean("Charged");
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        compoundTag.setBoolean("Charged", charged);
    }

    private static void addChargedProjectile(ItemStack crossbow, ItemStack projectile) {
        NBTTagCompound compoundTag = crossbow.getOrCreateTag();
        NBTTagList listTag;
        if (compoundTag.hasKeyOfType("ChargedProjectiles", 9)) {
            listTag = compoundTag.getList("ChargedProjectiles", 10);
        } else {
            listTag = new NBTTagList();
        }

        NBTTagCompound compoundTag2 = new NBTTagCompound();
        projectile.save(compoundTag2);
        listTag.add(compoundTag2);
        compoundTag.set("ChargedProjectiles", listTag);
    }

    private static List<ItemStack> getChargedProjectiles(ItemStack crossbow) {
        List<ItemStack> list = Lists.newArrayList();
        NBTTagCompound compoundTag = crossbow.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("ChargedProjectiles", 9)) {
            NBTTagList listTag = compoundTag.getList("ChargedProjectiles", 10);
            if (listTag != null) {
                for(int i = 0; i < listTag.size(); ++i) {
                    NBTTagCompound compoundTag2 = listTag.getCompound(i);
                    list.add(ItemStack.of(compoundTag2));
                }
            }
        }

        return list;
    }

    private static void clearChargedProjectiles(ItemStack crossbow) {
        NBTTagCompound compoundTag = crossbow.getTag();
        if (compoundTag != null) {
            NBTTagList listTag = compoundTag.getList("ChargedProjectiles", 9);
            listTag.clear();
            compoundTag.set("ChargedProjectiles", listTag);
        }

    }

    public static boolean containsChargedProjectile(ItemStack crossbow, Item projectile) {
        return getChargedProjectiles(crossbow).stream().anyMatch((s) -> {
            return s.is(projectile);
        });
    }

    private static void shootProjectile(World world, EntityLiving shooter, EnumHand hand, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean creative, float speed, float divergence, float simulated) {
        if (!world.isClientSide) {
            boolean bl = projectile.is(Items.FIREWORK_ROCKET);
            IProjectile projectile2;
            if (bl) {
                projectile2 = new EntityFireworks(world, projectile, shooter, shooter.locX(), shooter.getHeadY() - (double)0.15F, shooter.locZ(), true);
            } else {
                projectile2 = getArrow(world, shooter, crossbow, projectile);
                if (creative || simulated != 0.0F) {
                    ((EntityArrow)projectile2).pickup = EntityArrow.PickupStatus.CREATIVE_ONLY;
                }
            }

            if (shooter instanceof ICrossbow) {
                ICrossbow crossbowAttackMob = (ICrossbow)shooter;
                crossbowAttackMob.shootCrossbowProjectile(crossbowAttackMob.getGoalTarget(), crossbow, projectile2, simulated);
            } else {
                Vec3D vec3 = shooter.getUpVector(1.0F);
                Quaternion quaternion = new Quaternion(new Vector3fa(vec3), simulated, true);
                Vec3D vec32 = shooter.getViewVector(1.0F);
                Vector3fa vector3f = new Vector3fa(vec32);
                vector3f.transform(quaternion);
                projectile2.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, divergence);
            }

            crossbow.damage(bl ? 3 : 1, shooter, (e) -> {
                e.broadcastItemBreak(hand);
            });
            world.addEntity(projectile2);
            world.playSound((EntityHuman)null, shooter.locX(), shooter.locY(), shooter.locZ(), SoundEffects.CROSSBOW_SHOOT, EnumSoundCategory.PLAYERS, 1.0F, soundPitch);
        }
    }

    private static EntityArrow getArrow(World world, EntityLiving entity, ItemStack crossbow, ItemStack arrow) {
        ItemArrow arrowItem = (ItemArrow)(arrow.getItem() instanceof ItemArrow ? arrow.getItem() : Items.ARROW);
        EntityArrow abstractArrow = arrowItem.createArrow(world, arrow, entity);
        if (entity instanceof EntityHuman) {
            abstractArrow.setCritical(true);
        }

        abstractArrow.setSoundEvent(SoundEffects.CROSSBOW_HIT);
        abstractArrow.setShotFromCrossbow(true);
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.PIERCING, crossbow);
        if (i > 0) {
            abstractArrow.setPierceLevel((byte)i);
        }

        return abstractArrow;
    }

    public static void performShooting(World world, EntityLiving entity, EnumHand hand, ItemStack stack, float speed, float divergence) {
        List<ItemStack> list = getChargedProjectiles(stack);
        float[] fs = getShotPitches(entity.getRandom());

        for(int i = 0; i < list.size(); ++i) {
            ItemStack itemStack = list.get(i);
            boolean bl = entity instanceof EntityHuman && ((EntityHuman)entity).getAbilities().instabuild;
            if (!itemStack.isEmpty()) {
                if (i == 0) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, 0.0F);
                } else if (i == 1) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, -10.0F);
                } else if (i == 2) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, 10.0F);
                }
            }
        }

        onCrossbowShot(world, entity, stack);
    }

    private static float[] getShotPitches(Random random) {
        boolean bl = random.nextBoolean();
        return new float[]{1.0F, getRandomShotPitch(bl, random), getRandomShotPitch(!bl, random)};
    }

    private static float getRandomShotPitch(boolean flag, Random random) {
        float f = flag ? 0.63F : 0.43F;
        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
    }

    private static void onCrossbowShot(World world, EntityLiving entity, ItemStack stack) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)entity;
            if (!world.isClientSide) {
                CriterionTriggers.SHOT_CROSSBOW.trigger(serverPlayer, stack);
            }

            serverPlayer.awardStat(StatisticList.ITEM_USED.get(stack.getItem()));
        }

        clearChargedProjectiles(stack);
    }

    @Override
    public void onUseTick(World world, EntityLiving user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClientSide) {
            int i = EnchantmentManager.getEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
            SoundEffect soundEvent = this.getStartSound(i);
            SoundEffect soundEvent2 = i == 0 ? SoundEffects.CROSSBOW_LOADING_MIDDLE : null;
            float f = (float)(stack.getUseDuration() - remainingUseTicks) / (float)getChargeDuration(stack);
            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), soundEvent, EnumSoundCategory.PLAYERS, 0.5F, 1.0F);
            }

            if (f >= 0.5F && soundEvent2 != null && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), soundEvent2, EnumSoundCategory.PLAYERS, 0.5F, 1.0F);
            }
        }

    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return getChargeDuration(stack) + 3;
    }

    public static int getChargeDuration(ItemStack stack) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return i == 0 ? 25 : 25 - 5 * i;
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.CROSSBOW;
    }

    private SoundEffect getStartSound(int stage) {
        switch(stage) {
        case 1:
            return SoundEffects.CROSSBOW_QUICK_CHARGE_1;
        case 2:
            return SoundEffects.CROSSBOW_QUICK_CHARGE_2;
        case 3:
            return SoundEffects.CROSSBOW_QUICK_CHARGE_3;
        default:
            return SoundEffects.CROSSBOW_LOADING_START;
        }
    }

    private static float getPowerForTime(int useTicks, ItemStack stack) {
        float f = (float)useTicks / (float)getChargeDuration(stack);
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        List<ItemStack> list = getChargedProjectiles(stack);
        if (isCharged(stack) && !list.isEmpty()) {
            ItemStack itemStack = list.get(0);
            tooltip.add((new ChatMessage("item.minecraft.crossbow.projectile")).append(" ").addSibling(itemStack.getDisplayName()));
            if (context.isAdvanced() && itemStack.is(Items.FIREWORK_ROCKET)) {
                List<IChatBaseComponent> list2 = Lists.newArrayList();
                Items.FIREWORK_ROCKET.appendHoverText(itemStack, world, list2, context);
                if (!list2.isEmpty()) {
                    for(int i = 0; i < list2.size(); ++i) {
                        list2.set(i, (new ChatComponentText("  ")).addSibling(list2.get(i)).withStyle(EnumChatFormat.GRAY));
                    }

                    tooltip.addAll(list2);
                }
            }

        }
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }
}
