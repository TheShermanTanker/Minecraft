package net.minecraft.world.food;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;

public class Foods {
    public static final FoodInfo APPLE = (new FoodInfo.Builder()).nutrition(4).saturationMod(0.3F).build();
    public static final FoodInfo BAKED_POTATO = (new FoodInfo.Builder()).nutrition(5).saturationMod(0.6F).build();
    public static final FoodInfo BEEF = (new FoodInfo.Builder()).nutrition(3).saturationMod(0.3F).meat().build();
    public static final FoodInfo BEETROOT = (new FoodInfo.Builder()).nutrition(1).saturationMod(0.6F).build();
    public static final FoodInfo BEETROOT_SOUP = stew(6).build();
    public static final FoodInfo BREAD = (new FoodInfo.Builder()).nutrition(5).saturationMod(0.6F).build();
    public static final FoodInfo CARROT = (new FoodInfo.Builder()).nutrition(3).saturationMod(0.6F).build();
    public static final FoodInfo CHICKEN = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.3F).effect(new MobEffect(MobEffectList.HUNGER, 600, 0), 0.3F).meat().build();
    public static final FoodInfo CHORUS_FRUIT = (new FoodInfo.Builder()).nutrition(4).saturationMod(0.3F).alwaysEat().build();
    public static final FoodInfo COD = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodInfo COOKED_BEEF = (new FoodInfo.Builder()).nutrition(8).saturationMod(0.8F).meat().build();
    public static final FoodInfo COOKED_CHICKEN = (new FoodInfo.Builder()).nutrition(6).saturationMod(0.6F).meat().build();
    public static final FoodInfo COOKED_COD = (new FoodInfo.Builder()).nutrition(5).saturationMod(0.6F).build();
    public static final FoodInfo COOKED_MUTTON = (new FoodInfo.Builder()).nutrition(6).saturationMod(0.8F).meat().build();
    public static final FoodInfo COOKED_PORKCHOP = (new FoodInfo.Builder()).nutrition(8).saturationMod(0.8F).meat().build();
    public static final FoodInfo COOKED_RABBIT = (new FoodInfo.Builder()).nutrition(5).saturationMod(0.6F).meat().build();
    public static final FoodInfo COOKED_SALMON = (new FoodInfo.Builder()).nutrition(6).saturationMod(0.8F).build();
    public static final FoodInfo COOKIE = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodInfo DRIED_KELP = (new FoodInfo.Builder()).nutrition(1).saturationMod(0.3F).fast().build();
    public static final FoodInfo ENCHANTED_GOLDEN_APPLE = (new FoodInfo.Builder()).nutrition(4).saturationMod(1.2F).effect(new MobEffect(MobEffectList.REGENERATION, 400, 1), 1.0F).effect(new MobEffect(MobEffectList.DAMAGE_RESISTANCE, 6000, 0), 1.0F).effect(new MobEffect(MobEffectList.FIRE_RESISTANCE, 6000, 0), 1.0F).effect(new MobEffect(MobEffectList.ABSORPTION, 2400, 3), 1.0F).alwaysEat().build();
    public static final FoodInfo GOLDEN_APPLE = (new FoodInfo.Builder()).nutrition(4).saturationMod(1.2F).effect(new MobEffect(MobEffectList.REGENERATION, 100, 1), 1.0F).effect(new MobEffect(MobEffectList.ABSORPTION, 2400, 0), 1.0F).alwaysEat().build();
    public static final FoodInfo GOLDEN_CARROT = (new FoodInfo.Builder()).nutrition(6).saturationMod(1.2F).build();
    public static final FoodInfo HONEY_BOTTLE = (new FoodInfo.Builder()).nutrition(6).saturationMod(0.1F).build();
    public static final FoodInfo MELON_SLICE = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.3F).build();
    public static final FoodInfo MUSHROOM_STEW = stew(6).build();
    public static final FoodInfo MUTTON = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.3F).meat().build();
    public static final FoodInfo POISONOUS_POTATO = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.3F).effect(new MobEffect(MobEffectList.POISON, 100, 0), 0.6F).build();
    public static final FoodInfo PORKCHOP = (new FoodInfo.Builder()).nutrition(3).saturationMod(0.3F).meat().build();
    public static final FoodInfo POTATO = (new FoodInfo.Builder()).nutrition(1).saturationMod(0.3F).build();
    public static final FoodInfo PUFFERFISH = (new FoodInfo.Builder()).nutrition(1).saturationMod(0.1F).effect(new MobEffect(MobEffectList.POISON, 1200, 1), 1.0F).effect(new MobEffect(MobEffectList.HUNGER, 300, 2), 1.0F).effect(new MobEffect(MobEffectList.CONFUSION, 300, 0), 1.0F).build();
    public static final FoodInfo PUMPKIN_PIE = (new FoodInfo.Builder()).nutrition(8).saturationMod(0.3F).build();
    public static final FoodInfo RABBIT = (new FoodInfo.Builder()).nutrition(3).saturationMod(0.3F).meat().build();
    public static final FoodInfo RABBIT_STEW = stew(10).build();
    public static final FoodInfo ROTTEN_FLESH = (new FoodInfo.Builder()).nutrition(4).saturationMod(0.1F).effect(new MobEffect(MobEffectList.HUNGER, 600, 0), 0.8F).meat().build();
    public static final FoodInfo SALMON = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodInfo SPIDER_EYE = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.8F).effect(new MobEffect(MobEffectList.POISON, 100, 0), 1.0F).build();
    public static final FoodInfo SUSPICIOUS_STEW = stew(6).alwaysEat().build();
    public static final FoodInfo SWEET_BERRIES = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodInfo GLOW_BERRIES = (new FoodInfo.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodInfo TROPICAL_FISH = (new FoodInfo.Builder()).nutrition(1).saturationMod(0.1F).build();

    private static FoodInfo.Builder stew(int hunger) {
        return (new FoodInfo.Builder()).nutrition(hunger).saturationMod(0.6F);
    }
}
