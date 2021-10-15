package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;

public class CriterionConditionMobEffect {
    public static final CriterionConditionMobEffect ANY = new CriterionConditionMobEffect(Collections.emptyMap());
    private final Map<MobEffectList, CriterionConditionMobEffect.MobEffectInstancePredicate> effects;

    public CriterionConditionMobEffect(Map<MobEffectList, CriterionConditionMobEffect.MobEffectInstancePredicate> effects) {
        this.effects = effects;
    }

    public static CriterionConditionMobEffect effects() {
        return new CriterionConditionMobEffect(Maps.newLinkedHashMap());
    }

    public CriterionConditionMobEffect and(MobEffectList statusEffect) {
        this.effects.put(statusEffect, new CriterionConditionMobEffect.MobEffectInstancePredicate());
        return this;
    }

    public CriterionConditionMobEffect and(MobEffectList statusEffect, CriterionConditionMobEffect.MobEffectInstancePredicate data) {
        this.effects.put(statusEffect, data);
        return this;
    }

    public boolean matches(Entity entity) {
        if (this == ANY) {
            return true;
        } else {
            return entity instanceof EntityLiving ? this.matches(((EntityLiving)entity).getActiveEffectsMap()) : false;
        }
    }

    public boolean matches(EntityLiving livingEntity) {
        return this == ANY ? true : this.matches(livingEntity.getActiveEffectsMap());
    }

    public boolean matches(Map<MobEffectList, MobEffect> effects) {
        if (this == ANY) {
            return true;
        } else {
            for(Entry<MobEffectList, CriterionConditionMobEffect.MobEffectInstancePredicate> entry : this.effects.entrySet()) {
                MobEffect mobEffectInstance = effects.get(entry.getKey());
                if (!entry.getValue().matches(mobEffectInstance)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static CriterionConditionMobEffect fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "effects");
            Map<MobEffectList, CriterionConditionMobEffect.MobEffectInstancePredicate> map = Maps.newLinkedHashMap();

            for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                MinecraftKey resourceLocation = new MinecraftKey(entry.getKey());
                MobEffectList mobEffect = IRegistry.MOB_EFFECT.getOptional(resourceLocation).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown effect '" + resourceLocation + "'");
                });
                CriterionConditionMobEffect.MobEffectInstancePredicate mobEffectInstancePredicate = CriterionConditionMobEffect.MobEffectInstancePredicate.fromJson(ChatDeserializer.convertToJsonObject(entry.getValue(), entry.getKey()));
                map.put(mobEffect, mobEffectInstancePredicate);
            }

            return new CriterionConditionMobEffect(map);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();

            for(Entry<MobEffectList, CriterionConditionMobEffect.MobEffectInstancePredicate> entry : this.effects.entrySet()) {
                jsonObject.add(IRegistry.MOB_EFFECT.getKey(entry.getKey()).toString(), entry.getValue().serializeToJson());
            }

            return jsonObject;
        }
    }

    public static class MobEffectInstancePredicate {
        private final CriterionConditionValue.IntegerRange amplifier;
        private final CriterionConditionValue.IntegerRange duration;
        @Nullable
        private final Boolean ambient;
        @Nullable
        private final Boolean visible;

        public MobEffectInstancePredicate(CriterionConditionValue.IntegerRange amplifier, CriterionConditionValue.IntegerRange duration, @Nullable Boolean ambient, @Nullable Boolean visible) {
            this.amplifier = amplifier;
            this.duration = duration;
            this.ambient = ambient;
            this.visible = visible;
        }

        public MobEffectInstancePredicate() {
            this(CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, (Boolean)null, (Boolean)null);
        }

        public boolean matches(@Nullable MobEffect statusEffectInstance) {
            if (statusEffectInstance == null) {
                return false;
            } else if (!this.amplifier.matches(statusEffectInstance.getAmplifier())) {
                return false;
            } else if (!this.duration.matches(statusEffectInstance.getDuration())) {
                return false;
            } else if (this.ambient != null && this.ambient != statusEffectInstance.isAmbient()) {
                return false;
            } else {
                return this.visible == null || this.visible == statusEffectInstance.isShowParticles();
            }
        }

        public JsonElement serializeToJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("amplifier", this.amplifier.serializeToJson());
            jsonObject.add("duration", this.duration.serializeToJson());
            jsonObject.addProperty("ambient", this.ambient);
            jsonObject.addProperty("visible", this.visible);
            return jsonObject;
        }

        public static CriterionConditionMobEffect.MobEffectInstancePredicate fromJson(JsonObject json) {
            CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(json.get("amplifier"));
            CriterionConditionValue.IntegerRange ints2 = CriterionConditionValue.IntegerRange.fromJson(json.get("duration"));
            Boolean boolean_ = json.has("ambient") ? ChatDeserializer.getAsBoolean(json, "ambient") : null;
            Boolean boolean2 = json.has("visible") ? ChatDeserializer.getAsBoolean(json, "visible") : null;
            return new CriterionConditionMobEffect.MobEffectInstancePredicate(ints, ints2, boolean_, boolean2);
        }
    }
}
