package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;

public class CriterionConditionEntityFlags {
    public static final CriterionConditionEntityFlags ANY = (new CriterionConditionEntityFlags.Builder()).build();
    @Nullable
    private final Boolean isOnFire;
    @Nullable
    private final Boolean isCrouching;
    @Nullable
    private final Boolean isSprinting;
    @Nullable
    private final Boolean isSwimming;
    @Nullable
    private final Boolean isBaby;

    public CriterionConditionEntityFlags(@Nullable Boolean isOnFire, @Nullable Boolean isSneaking, @Nullable Boolean isSprinting, @Nullable Boolean isSwimming, @Nullable Boolean isBaby) {
        this.isOnFire = isOnFire;
        this.isCrouching = isSneaking;
        this.isSprinting = isSprinting;
        this.isSwimming = isSwimming;
        this.isBaby = isBaby;
    }

    public boolean matches(Entity entity) {
        if (this.isOnFire != null && entity.isBurning() != this.isOnFire) {
            return false;
        } else if (this.isCrouching != null && entity.isCrouching() != this.isCrouching) {
            return false;
        } else if (this.isSprinting != null && entity.isSprinting() != this.isSprinting) {
            return false;
        } else if (this.isSwimming != null && entity.isSwimming() != this.isSwimming) {
            return false;
        } else {
            return this.isBaby == null || !(entity instanceof EntityLiving) || ((EntityLiving)entity).isBaby() == this.isBaby;
        }
    }

    @Nullable
    private static Boolean getOptionalBoolean(JsonObject json, String key) {
        return json.has(key) ? ChatDeserializer.getAsBoolean(json, key) : null;
    }

    public static CriterionConditionEntityFlags fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "entity flags");
            Boolean boolean_ = getOptionalBoolean(jsonObject, "is_on_fire");
            Boolean boolean2 = getOptionalBoolean(jsonObject, "is_sneaking");
            Boolean boolean3 = getOptionalBoolean(jsonObject, "is_sprinting");
            Boolean boolean4 = getOptionalBoolean(jsonObject, "is_swimming");
            Boolean boolean5 = getOptionalBoolean(jsonObject, "is_baby");
            return new CriterionConditionEntityFlags(boolean_, boolean2, boolean3, boolean4, boolean5);
        } else {
            return ANY;
        }
    }

    private void addOptionalBoolean(JsonObject json, String key, @Nullable Boolean value) {
        if (value != null) {
            json.addProperty(key, value);
        }

    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            this.addOptionalBoolean(jsonObject, "is_on_fire", this.isOnFire);
            this.addOptionalBoolean(jsonObject, "is_sneaking", this.isCrouching);
            this.addOptionalBoolean(jsonObject, "is_sprinting", this.isSprinting);
            this.addOptionalBoolean(jsonObject, "is_swimming", this.isSwimming);
            this.addOptionalBoolean(jsonObject, "is_baby", this.isBaby);
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private Boolean isOnFire;
        @Nullable
        private Boolean isCrouching;
        @Nullable
        private Boolean isSprinting;
        @Nullable
        private Boolean isSwimming;
        @Nullable
        private Boolean isBaby;

        public static CriterionConditionEntityFlags.Builder flags() {
            return new CriterionConditionEntityFlags.Builder();
        }

        public CriterionConditionEntityFlags.Builder setOnFire(@Nullable Boolean onFire) {
            this.isOnFire = onFire;
            return this;
        }

        public CriterionConditionEntityFlags.Builder setCrouching(@Nullable Boolean sneaking) {
            this.isCrouching = sneaking;
            return this;
        }

        public CriterionConditionEntityFlags.Builder setSprinting(@Nullable Boolean sprinting) {
            this.isSprinting = sprinting;
            return this;
        }

        public CriterionConditionEntityFlags.Builder setSwimming(@Nullable Boolean swimming) {
            this.isSwimming = swimming;
            return this;
        }

        public CriterionConditionEntityFlags.Builder setIsBaby(@Nullable Boolean isBaby) {
            this.isBaby = isBaby;
            return this;
        }

        public CriterionConditionEntityFlags build() {
            return new CriterionConditionEntityFlags(this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isBaby);
        }
    }
}
