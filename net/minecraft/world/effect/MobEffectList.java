package net.minecraft.world.effect;

import net.minecraft.core.IRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;

public class MobEffectList {
    public static final MobEffectBase MOVEMENT_SPEED = register(1, "speed", (new MobEffectBase(MobEffectInfo.BENEFICIAL, 8171462)).addAttributeModifier(GenericAttributes.MOVEMENT_SPEED, "91AEAA56-376B-4498-935B-2F7F68070635", (double)0.2F, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final MobEffectBase MOVEMENT_SLOWDOWN = register(2, "slowness", (new MobEffectBase(MobEffectInfo.HARMFUL, 5926017)).addAttributeModifier(GenericAttributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", (double)-0.15F, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final MobEffectBase DIG_SPEED = register(3, "haste", (new MobEffectBase(MobEffectInfo.BENEFICIAL, 14270531)).addAttributeModifier(GenericAttributes.ATTACK_SPEED, "AF8B6E3F-3328-4C0A-AA36-5BA2BB9DBEF3", (double)0.1F, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final MobEffectBase DIG_SLOWDOWN = register(4, "mining_fatigue", (new MobEffectBase(MobEffectInfo.HARMFUL, 4866583)).addAttributeModifier(GenericAttributes.ATTACK_SPEED, "55FCED67-E92A-486E-9800-B47F202C4386", (double)-0.1F, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final MobEffectBase DAMAGE_BOOST = register(5, "strength", (new MobEffectAttackDamage(MobEffectInfo.BENEFICIAL, 9643043, 3.0D)).addAttributeModifier(GenericAttributes.ATTACK_DAMAGE, "648D7064-6A60-4F59-8ABE-C2C23A6DD7A9", 0.0D, AttributeModifier.Operation.ADDITION));
    public static final MobEffectBase HEAL = register(6, "instant_health", new MobEffectInstant(MobEffectInfo.BENEFICIAL, 16262179));
    public static final MobEffectBase HARM = register(7, "instant_damage", new MobEffectInstant(MobEffectInfo.HARMFUL, 4393481));
    public static final MobEffectBase JUMP = register(8, "jump_boost", new MobEffectBase(MobEffectInfo.BENEFICIAL, 2293580));
    public static final MobEffectBase CONFUSION = register(9, "nausea", new MobEffectBase(MobEffectInfo.HARMFUL, 5578058));
    public static final MobEffectBase REGENERATION = register(10, "regeneration", new MobEffectBase(MobEffectInfo.BENEFICIAL, 13458603));
    public static final MobEffectBase DAMAGE_RESISTANCE = register(11, "resistance", new MobEffectBase(MobEffectInfo.BENEFICIAL, 10044730));
    public static final MobEffectBase FIRE_RESISTANCE = register(12, "fire_resistance", new MobEffectBase(MobEffectInfo.BENEFICIAL, 14981690));
    public static final MobEffectBase WATER_BREATHING = register(13, "water_breathing", new MobEffectBase(MobEffectInfo.BENEFICIAL, 3035801));
    public static final MobEffectBase INVISIBILITY = register(14, "invisibility", new MobEffectBase(MobEffectInfo.BENEFICIAL, 8356754));
    public static final MobEffectBase BLINDNESS = register(15, "blindness", new MobEffectBase(MobEffectInfo.HARMFUL, 2039587));
    public static final MobEffectBase NIGHT_VISION = register(16, "night_vision", new MobEffectBase(MobEffectInfo.BENEFICIAL, 2039713));
    public static final MobEffectBase HUNGER = register(17, "hunger", new MobEffectBase(MobEffectInfo.HARMFUL, 5797459));
    public static final MobEffectBase WEAKNESS = register(18, "weakness", (new MobEffectAttackDamage(MobEffectInfo.HARMFUL, 4738376, -4.0D)).addAttributeModifier(GenericAttributes.ATTACK_DAMAGE, "22653B89-116E-49DC-9B6B-9971489B5BE5", 0.0D, AttributeModifier.Operation.ADDITION));
    public static final MobEffectBase POISON = register(19, "poison", new MobEffectBase(MobEffectInfo.HARMFUL, 5149489));
    public static final MobEffectBase WITHER = register(20, "wither", new MobEffectBase(MobEffectInfo.HARMFUL, 3484199));
    public static final MobEffectBase HEALTH_BOOST = register(21, "health_boost", (new MobEffectHealthBoost(MobEffectInfo.BENEFICIAL, 16284963)).addAttributeModifier(GenericAttributes.MAX_HEALTH, "5D6F0BA2-1186-46AC-B896-C61C5CEE99CC", 4.0D, AttributeModifier.Operation.ADDITION));
    public static final MobEffectBase ABSORPTION = register(22, "absorption", new MobEffectAbsorption(MobEffectInfo.BENEFICIAL, 2445989));
    public static final MobEffectBase SATURATION = register(23, "saturation", new MobEffectInstant(MobEffectInfo.BENEFICIAL, 16262179));
    public static final MobEffectBase GLOWING = register(24, "glowing", new MobEffectBase(MobEffectInfo.NEUTRAL, 9740385));
    public static final MobEffectBase LEVITATION = register(25, "levitation", new MobEffectBase(MobEffectInfo.HARMFUL, 13565951));
    public static final MobEffectBase LUCK = register(26, "luck", (new MobEffectBase(MobEffectInfo.BENEFICIAL, 3381504)).addAttributeModifier(GenericAttributes.LUCK, "03C3C89D-7037-4B42-869F-B146BCB64D2E", 1.0D, AttributeModifier.Operation.ADDITION));
    public static final MobEffectBase UNLUCK = register(27, "unluck", (new MobEffectBase(MobEffectInfo.HARMFUL, 12624973)).addAttributeModifier(GenericAttributes.LUCK, "CC5AF142-2BD2-4215-B636-2605AED11727", -1.0D, AttributeModifier.Operation.ADDITION));
    public static final MobEffectBase SLOW_FALLING = register(28, "slow_falling", new MobEffectBase(MobEffectInfo.BENEFICIAL, 16773073));
    public static final MobEffectBase CONDUIT_POWER = register(29, "conduit_power", new MobEffectBase(MobEffectInfo.BENEFICIAL, 1950417));
    public static final MobEffectBase DOLPHINS_GRACE = register(30, "dolphins_grace", new MobEffectBase(MobEffectInfo.BENEFICIAL, 8954814));
    public static final MobEffectBase BAD_OMEN = register(31, "bad_omen", new MobEffectBase(MobEffectInfo.NEUTRAL, 745784) {
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public void tick(EntityLiving entity, int amplifier) {
            if (entity instanceof EntityPlayer && !entity.isSpectator()) {
                EntityPlayer serverPlayer = (EntityPlayer)entity;
                WorldServer serverLevel = serverPlayer.getWorldServer();
                if (serverLevel.getDifficulty() == EnumDifficulty.PEACEFUL) {
                    return;
                }

                if (serverLevel.isVillage(entity.getChunkCoordinates())) {
                    serverLevel.getPersistentRaid().createOrExtendRaid(serverPlayer);
                }
            }

        }
    });
    public static final MobEffectBase HERO_OF_THE_VILLAGE = register(32, "hero_of_the_village", new MobEffectBase(MobEffectInfo.BENEFICIAL, 4521796));

    private static MobEffectBase register(int rawId, String id, MobEffectBase entry) {
        return IRegistry.registerMapping(IRegistry.MOB_EFFECT, rawId, id, entry);
    }
}
