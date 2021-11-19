package net.minecraft.world.item.alchemy;

import net.minecraft.core.IRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;

public class Potions {
    public static final PotionRegistry EMPTY = register("empty", new PotionRegistry());
    public static final PotionRegistry WATER = register("water", new PotionRegistry());
    public static final PotionRegistry MUNDANE = register("mundane", new PotionRegistry());
    public static final PotionRegistry THICK = register("thick", new PotionRegistry());
    public static final PotionRegistry AWKWARD = register("awkward", new PotionRegistry());
    public static final PotionRegistry NIGHT_VISION = register("night_vision", new PotionRegistry(new MobEffect(MobEffectList.NIGHT_VISION, 3600)));
    public static final PotionRegistry LONG_NIGHT_VISION = register("long_night_vision", new PotionRegistry("night_vision", new MobEffect(MobEffectList.NIGHT_VISION, 9600)));
    public static final PotionRegistry INVISIBILITY = register("invisibility", new PotionRegistry(new MobEffect(MobEffectList.INVISIBILITY, 3600)));
    public static final PotionRegistry LONG_INVISIBILITY = register("long_invisibility", new PotionRegistry("invisibility", new MobEffect(MobEffectList.INVISIBILITY, 9600)));
    public static final PotionRegistry LEAPING = register("leaping", new PotionRegistry(new MobEffect(MobEffectList.JUMP, 3600)));
    public static final PotionRegistry LONG_LEAPING = register("long_leaping", new PotionRegistry("leaping", new MobEffect(MobEffectList.JUMP, 9600)));
    public static final PotionRegistry STRONG_LEAPING = register("strong_leaping", new PotionRegistry("leaping", new MobEffect(MobEffectList.JUMP, 1800, 1)));
    public static final PotionRegistry FIRE_RESISTANCE = register("fire_resistance", new PotionRegistry(new MobEffect(MobEffectList.FIRE_RESISTANCE, 3600)));
    public static final PotionRegistry LONG_FIRE_RESISTANCE = register("long_fire_resistance", new PotionRegistry("fire_resistance", new MobEffect(MobEffectList.FIRE_RESISTANCE, 9600)));
    public static final PotionRegistry SWIFTNESS = register("swiftness", new PotionRegistry(new MobEffect(MobEffectList.MOVEMENT_SPEED, 3600)));
    public static final PotionRegistry LONG_SWIFTNESS = register("long_swiftness", new PotionRegistry("swiftness", new MobEffect(MobEffectList.MOVEMENT_SPEED, 9600)));
    public static final PotionRegistry STRONG_SWIFTNESS = register("strong_swiftness", new PotionRegistry("swiftness", new MobEffect(MobEffectList.MOVEMENT_SPEED, 1800, 1)));
    public static final PotionRegistry SLOWNESS = register("slowness", new PotionRegistry(new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 1800)));
    public static final PotionRegistry LONG_SLOWNESS = register("long_slowness", new PotionRegistry("slowness", new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 4800)));
    public static final PotionRegistry STRONG_SLOWNESS = register("strong_slowness", new PotionRegistry("slowness", new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 400, 3)));
    public static final PotionRegistry TURTLE_MASTER = register("turtle_master", new PotionRegistry("turtle_master", new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 400, 3), new MobEffect(MobEffectList.DAMAGE_RESISTANCE, 400, 2)));
    public static final PotionRegistry LONG_TURTLE_MASTER = register("long_turtle_master", new PotionRegistry("turtle_master", new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 800, 3), new MobEffect(MobEffectList.DAMAGE_RESISTANCE, 800, 2)));
    public static final PotionRegistry STRONG_TURTLE_MASTER = register("strong_turtle_master", new PotionRegistry("turtle_master", new MobEffect(MobEffectList.MOVEMENT_SLOWDOWN, 400, 5), new MobEffect(MobEffectList.DAMAGE_RESISTANCE, 400, 3)));
    public static final PotionRegistry WATER_BREATHING = register("water_breathing", new PotionRegistry(new MobEffect(MobEffectList.WATER_BREATHING, 3600)));
    public static final PotionRegistry LONG_WATER_BREATHING = register("long_water_breathing", new PotionRegistry("water_breathing", new MobEffect(MobEffectList.WATER_BREATHING, 9600)));
    public static final PotionRegistry HEALING = register("healing", new PotionRegistry(new MobEffect(MobEffectList.HEAL, 1)));
    public static final PotionRegistry STRONG_HEALING = register("strong_healing", new PotionRegistry("healing", new MobEffect(MobEffectList.HEAL, 1, 1)));
    public static final PotionRegistry HARMING = register("harming", new PotionRegistry(new MobEffect(MobEffectList.HARM, 1)));
    public static final PotionRegistry STRONG_HARMING = register("strong_harming", new PotionRegistry("harming", new MobEffect(MobEffectList.HARM, 1, 1)));
    public static final PotionRegistry POISON = register("poison", new PotionRegistry(new MobEffect(MobEffectList.POISON, 900)));
    public static final PotionRegistry LONG_POISON = register("long_poison", new PotionRegistry("poison", new MobEffect(MobEffectList.POISON, 1800)));
    public static final PotionRegistry STRONG_POISON = register("strong_poison", new PotionRegistry("poison", new MobEffect(MobEffectList.POISON, 432, 1)));
    public static final PotionRegistry REGENERATION = register("regeneration", new PotionRegistry(new MobEffect(MobEffectList.REGENERATION, 900)));
    public static final PotionRegistry LONG_REGENERATION = register("long_regeneration", new PotionRegistry("regeneration", new MobEffect(MobEffectList.REGENERATION, 1800)));
    public static final PotionRegistry STRONG_REGENERATION = register("strong_regeneration", new PotionRegistry("regeneration", new MobEffect(MobEffectList.REGENERATION, 450, 1)));
    public static final PotionRegistry STRENGTH = register("strength", new PotionRegistry(new MobEffect(MobEffectList.DAMAGE_BOOST, 3600)));
    public static final PotionRegistry LONG_STRENGTH = register("long_strength", new PotionRegistry("strength", new MobEffect(MobEffectList.DAMAGE_BOOST, 9600)));
    public static final PotionRegistry STRONG_STRENGTH = register("strong_strength", new PotionRegistry("strength", new MobEffect(MobEffectList.DAMAGE_BOOST, 1800, 1)));
    public static final PotionRegistry WEAKNESS = register("weakness", new PotionRegistry(new MobEffect(MobEffectList.WEAKNESS, 1800)));
    public static final PotionRegistry LONG_WEAKNESS = register("long_weakness", new PotionRegistry("weakness", new MobEffect(MobEffectList.WEAKNESS, 4800)));
    public static final PotionRegistry LUCK = register("luck", new PotionRegistry("luck", new MobEffect(MobEffectList.LUCK, 6000)));
    public static final PotionRegistry SLOW_FALLING = register("slow_falling", new PotionRegistry(new MobEffect(MobEffectList.SLOW_FALLING, 1800)));
    public static final PotionRegistry LONG_SLOW_FALLING = register("long_slow_falling", new PotionRegistry("slow_falling", new MobEffect(MobEffectList.SLOW_FALLING, 4800)));

    private static PotionRegistry register(String name, PotionRegistry potion) {
        return IRegistry.register(IRegistry.POTION, name, potion);
    }
}
