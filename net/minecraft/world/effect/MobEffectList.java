package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.EntityHuman;

public class MobEffectList {
    private final Map<AttributeBase, AttributeModifier> attributeModifiers = Maps.newHashMap();
    private final MobEffectInfo category;
    private final int color;
    @Nullable
    private String descriptionId;

    @Nullable
    public static MobEffectList fromId(int rawId) {
        return IRegistry.MOB_EFFECT.fromId(rawId);
    }

    public static int getId(MobEffectList type) {
        return IRegistry.MOB_EFFECT.getId(type);
    }

    protected MobEffectList(MobEffectInfo type, int color) {
        this.category = type;
        this.color = color;
    }

    public void tick(EntityLiving entity, int amplifier) {
        if (this == MobEffects.REGENERATION) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
        } else if (this == MobEffects.POISON) {
            if (entity.getHealth() > 1.0F) {
                entity.damageEntity(DamageSource.MAGIC, 1.0F);
            }
        } else if (this == MobEffects.WITHER) {
            entity.damageEntity(DamageSource.WITHER, 1.0F);
        } else if (this == MobEffects.HUNGER && entity instanceof EntityHuman) {
            ((EntityHuman)entity).applyExhaustion(0.005F * (float)(amplifier + 1));
        } else if (this == MobEffects.SATURATION && entity instanceof EntityHuman) {
            if (!entity.level.isClientSide) {
                ((EntityHuman)entity).getFoodData().eat(amplifier + 1, 1.0F);
            }
        } else if ((this != MobEffects.HEAL || entity.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !entity.isInvertedHealAndHarm())) {
            if (this == MobEffects.HARM && !entity.isInvertedHealAndHarm() || this == MobEffects.HEAL && entity.isInvertedHealAndHarm()) {
                entity.damageEntity(DamageSource.MAGIC, (float)(6 << amplifier));
            }
        } else {
            entity.heal((float)Math.max(4 << amplifier, 0));
        }

    }

    public void applyInstantEffect(@Nullable Entity source, @Nullable Entity attacker, EntityLiving target, int amplifier, double proximity) {
        if ((this != MobEffects.HEAL || target.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !target.isInvertedHealAndHarm())) {
            if (this == MobEffects.HARM && !target.isInvertedHealAndHarm() || this == MobEffects.HEAL && target.isInvertedHealAndHarm()) {
                int j = (int)(proximity * (double)(6 << amplifier) + 0.5D);
                if (source == null) {
                    target.damageEntity(DamageSource.MAGIC, (float)j);
                } else {
                    target.damageEntity(DamageSource.indirectMagic(source, attacker), (float)j);
                }
            } else {
                this.tick(target, amplifier);
            }
        } else {
            int i = (int)(proximity * (double)(4 << amplifier) + 0.5D);
            target.heal((float)i);
        }

    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        if (this == MobEffects.REGENERATION) {
            int i = 50 >> amplifier;
            if (i > 0) {
                return duration % i == 0;
            } else {
                return true;
            }
        } else if (this == MobEffects.POISON) {
            int j = 25 >> amplifier;
            if (j > 0) {
                return duration % j == 0;
            } else {
                return true;
            }
        } else if (this == MobEffects.WITHER) {
            int k = 40 >> amplifier;
            if (k > 0) {
                return duration % k == 0;
            } else {
                return true;
            }
        } else {
            return this == MobEffects.HUNGER;
        }
    }

    public boolean isInstant() {
        return false;
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.makeDescriptionId("effect", IRegistry.MOB_EFFECT.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public IChatBaseComponent getDisplayName() {
        return new ChatMessage(this.getDescriptionId());
    }

    public MobEffectInfo getCategory() {
        return this.category;
    }

    public int getColor() {
        return this.color;
    }

    public MobEffectList addAttributeModifier(AttributeBase attribute, String uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier attributeModifier = new AttributeModifier(UUID.fromString(uuid), this::getDescriptionId, amount, operation);
        this.attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public Map<AttributeBase, AttributeModifier> getAttributeModifiers() {
        return this.attributeModifiers;
    }

    public void removeAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        for(Entry<AttributeBase, AttributeModifier> entry : this.attributeModifiers.entrySet()) {
            AttributeModifiable attributeInstance = attributes.getInstance(entry.getKey());
            if (attributeInstance != null) {
                attributeInstance.removeModifier(entry.getValue());
            }
        }

    }

    public void addAttributeModifiers(EntityLiving entity, AttributeMapBase attributes, int amplifier) {
        for(Entry<AttributeBase, AttributeModifier> entry : this.attributeModifiers.entrySet()) {
            AttributeModifiable attributeInstance = attributes.getInstance(entry.getKey());
            if (attributeInstance != null) {
                AttributeModifier attributeModifier = entry.getValue();
                attributeInstance.removeModifier(attributeModifier);
                attributeInstance.addPermanentModifier(new AttributeModifier(attributeModifier.getUniqueId(), this.getDescriptionId() + " " + amplifier, this.getAttributeModifierValue(amplifier, attributeModifier), attributeModifier.getOperation()));
            }
        }

    }

    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (double)(amplifier + 1);
    }

    public boolean isBeneficial() {
        return this.category == MobEffectInfo.BENEFICIAL;
    }
}
