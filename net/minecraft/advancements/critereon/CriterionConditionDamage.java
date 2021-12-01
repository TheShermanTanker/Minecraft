package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.damagesource.DamageSource;

public class CriterionConditionDamage {
    public static final CriterionConditionDamage ANY = CriterionConditionDamage.Builder.damageInstance().build();
    private final CriterionConditionValue.DoubleRange dealtDamage;
    private final CriterionConditionValue.DoubleRange takenDamage;
    private final CriterionConditionEntity sourceEntity;
    @Nullable
    private final Boolean blocked;
    private final CriterionConditionDamageSource type;

    public CriterionConditionDamage() {
        this.dealtDamage = CriterionConditionValue.DoubleRange.ANY;
        this.takenDamage = CriterionConditionValue.DoubleRange.ANY;
        this.sourceEntity = CriterionConditionEntity.ANY;
        this.blocked = null;
        this.type = CriterionConditionDamageSource.ANY;
    }

    public CriterionConditionDamage(CriterionConditionValue.DoubleRange dealt, CriterionConditionValue.DoubleRange taken, CriterionConditionEntity sourceEntity, @Nullable Boolean blocked, CriterionConditionDamageSource type) {
        this.dealtDamage = dealt;
        this.takenDamage = taken;
        this.sourceEntity = sourceEntity;
        this.blocked = blocked;
        this.type = type;
    }

    public boolean matches(EntityPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
        if (this == ANY) {
            return true;
        } else if (!this.dealtDamage.matches((double)dealt)) {
            return false;
        } else if (!this.takenDamage.matches((double)taken)) {
            return false;
        } else if (!this.sourceEntity.matches(player, source.getEntity())) {
            return false;
        } else if (this.blocked != null && this.blocked != blocked) {
            return false;
        } else {
            return this.type.matches(player, source);
        }
    }

    public static CriterionConditionDamage fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "damage");
            CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("dealt"));
            CriterionConditionValue.DoubleRange doubles2 = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("taken"));
            Boolean boolean_ = jsonObject.has("blocked") ? ChatDeserializer.getAsBoolean(jsonObject, "blocked") : null;
            CriterionConditionEntity entityPredicate = CriterionConditionEntity.fromJson(jsonObject.get("source_entity"));
            CriterionConditionDamageSource damageSourcePredicate = CriterionConditionDamageSource.fromJson(jsonObject.get("type"));
            return new CriterionConditionDamage(doubles, doubles2, entityPredicate, boolean_, damageSourcePredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("dealt", this.dealtDamage.serializeToJson());
            jsonObject.add("taken", this.takenDamage.serializeToJson());
            jsonObject.add("source_entity", this.sourceEntity.serializeToJson());
            jsonObject.add("type", this.type.serializeToJson());
            if (this.blocked != null) {
                jsonObject.addProperty("blocked", this.blocked);
            }

            return jsonObject;
        }
    }

    public static class Builder {
        private CriterionConditionValue.DoubleRange dealtDamage = CriterionConditionValue.DoubleRange.ANY;
        private CriterionConditionValue.DoubleRange takenDamage = CriterionConditionValue.DoubleRange.ANY;
        private CriterionConditionEntity sourceEntity = CriterionConditionEntity.ANY;
        @Nullable
        private Boolean blocked;
        private CriterionConditionDamageSource type = CriterionConditionDamageSource.ANY;

        public static CriterionConditionDamage.Builder damageInstance() {
            return new CriterionConditionDamage.Builder();
        }

        public CriterionConditionDamage.Builder dealtDamage(CriterionConditionValue.DoubleRange dealt) {
            this.dealtDamage = dealt;
            return this;
        }

        public CriterionConditionDamage.Builder takenDamage(CriterionConditionValue.DoubleRange taken) {
            this.takenDamage = taken;
            return this;
        }

        public CriterionConditionDamage.Builder sourceEntity(CriterionConditionEntity sourceEntity) {
            this.sourceEntity = sourceEntity;
            return this;
        }

        public CriterionConditionDamage.Builder blocked(Boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public CriterionConditionDamage.Builder type(CriterionConditionDamageSource type) {
            this.type = type;
            return this;
        }

        public CriterionConditionDamage.Builder type(CriterionConditionDamageSource.Builder builder) {
            this.type = builder.build();
            return this;
        }

        public CriterionConditionDamage build() {
            return new CriterionConditionDamage(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
        }
    }
}
