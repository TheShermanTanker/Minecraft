package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3D;

public class CriterionConditionDamageSource {
    public static final CriterionConditionDamageSource ANY = CriterionConditionDamageSource.Builder.damageType().build();
    private final Boolean isProjectile;
    private final Boolean isExplosion;
    private final Boolean bypassesArmor;
    private final Boolean bypassesInvulnerability;
    private final Boolean bypassesMagic;
    private final Boolean isFire;
    private final Boolean isMagic;
    private final Boolean isLightning;
    private final CriterionConditionEntity directEntity;
    private final CriterionConditionEntity sourceEntity;

    public CriterionConditionDamageSource(@Nullable Boolean isProjectile, @Nullable Boolean isExplosion, @Nullable Boolean bypassesArmor, @Nullable Boolean bypassesInvulnerability, @Nullable Boolean bypassesMagic, @Nullable Boolean isFire, @Nullable Boolean isMagic, @Nullable Boolean isLightning, CriterionConditionEntity directEntity, CriterionConditionEntity sourceEntity) {
        this.isProjectile = isProjectile;
        this.isExplosion = isExplosion;
        this.bypassesArmor = bypassesArmor;
        this.bypassesInvulnerability = bypassesInvulnerability;
        this.bypassesMagic = bypassesMagic;
        this.isFire = isFire;
        this.isMagic = isMagic;
        this.isLightning = isLightning;
        this.directEntity = directEntity;
        this.sourceEntity = sourceEntity;
    }

    public boolean matches(EntityPlayer player, DamageSource damageSource) {
        return this.matches(player.getWorldServer(), player.getPositionVector(), damageSource);
    }

    public boolean matches(WorldServer world, Vec3D pos, DamageSource damageSource) {
        if (this == ANY) {
            return true;
        } else if (this.isProjectile != null && this.isProjectile != damageSource.isProjectile()) {
            return false;
        } else if (this.isExplosion != null && this.isExplosion != damageSource.isExplosion()) {
            return false;
        } else if (this.bypassesArmor != null && this.bypassesArmor != damageSource.ignoresArmor()) {
            return false;
        } else if (this.bypassesInvulnerability != null && this.bypassesInvulnerability != damageSource.ignoresInvulnerability()) {
            return false;
        } else if (this.bypassesMagic != null && this.bypassesMagic != damageSource.isStarvation()) {
            return false;
        } else if (this.isFire != null && this.isFire != damageSource.isFire()) {
            return false;
        } else if (this.isMagic != null && this.isMagic != damageSource.isMagic()) {
            return false;
        } else if (this.isLightning != null && this.isLightning != (damageSource == DamageSource.LIGHTNING_BOLT)) {
            return false;
        } else if (!this.directEntity.matches(world, pos, damageSource.getDirectEntity())) {
            return false;
        } else {
            return this.sourceEntity.matches(world, pos, damageSource.getEntity());
        }
    }

    public static CriterionConditionDamageSource fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "damage type");
            Boolean boolean_ = getOptionalBoolean(jsonObject, "is_projectile");
            Boolean boolean2 = getOptionalBoolean(jsonObject, "is_explosion");
            Boolean boolean3 = getOptionalBoolean(jsonObject, "bypasses_armor");
            Boolean boolean4 = getOptionalBoolean(jsonObject, "bypasses_invulnerability");
            Boolean boolean5 = getOptionalBoolean(jsonObject, "bypasses_magic");
            Boolean boolean6 = getOptionalBoolean(jsonObject, "is_fire");
            Boolean boolean7 = getOptionalBoolean(jsonObject, "is_magic");
            Boolean boolean8 = getOptionalBoolean(jsonObject, "is_lightning");
            CriterionConditionEntity entityPredicate = CriterionConditionEntity.fromJson(jsonObject.get("direct_entity"));
            CriterionConditionEntity entityPredicate2 = CriterionConditionEntity.fromJson(jsonObject.get("source_entity"));
            return new CriterionConditionDamageSource(boolean_, boolean2, boolean3, boolean4, boolean5, boolean6, boolean7, boolean8, entityPredicate, entityPredicate2);
        } else {
            return ANY;
        }
    }

    @Nullable
    private static Boolean getOptionalBoolean(JsonObject obj, String name) {
        return obj.has(name) ? ChatDeserializer.getAsBoolean(obj, name) : null;
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            this.addOptionally(jsonObject, "is_projectile", this.isProjectile);
            this.addOptionally(jsonObject, "is_explosion", this.isExplosion);
            this.addOptionally(jsonObject, "bypasses_armor", this.bypassesArmor);
            this.addOptionally(jsonObject, "bypasses_invulnerability", this.bypassesInvulnerability);
            this.addOptionally(jsonObject, "bypasses_magic", this.bypassesMagic);
            this.addOptionally(jsonObject, "is_fire", this.isFire);
            this.addOptionally(jsonObject, "is_magic", this.isMagic);
            this.addOptionally(jsonObject, "is_lightning", this.isLightning);
            jsonObject.add("direct_entity", this.directEntity.serializeToJson());
            jsonObject.add("source_entity", this.sourceEntity.serializeToJson());
            return jsonObject;
        }
    }

    private void addOptionally(JsonObject json, String key, @Nullable Boolean value) {
        if (value != null) {
            json.addProperty(key, value);
        }

    }

    public static class Builder {
        private Boolean isProjectile;
        private Boolean isExplosion;
        private Boolean bypassesArmor;
        private Boolean bypassesInvulnerability;
        private Boolean bypassesMagic;
        private Boolean isFire;
        private Boolean isMagic;
        private Boolean isLightning;
        private CriterionConditionEntity directEntity = CriterionConditionEntity.ANY;
        private CriterionConditionEntity sourceEntity = CriterionConditionEntity.ANY;

        public static CriterionConditionDamageSource.Builder damageType() {
            return new CriterionConditionDamageSource.Builder();
        }

        public CriterionConditionDamageSource.Builder isProjectile(Boolean projectile) {
            this.isProjectile = projectile;
            return this;
        }

        public CriterionConditionDamageSource.Builder isExplosion(Boolean explosion) {
            this.isExplosion = explosion;
            return this;
        }

        public CriterionConditionDamageSource.Builder bypassesArmor(Boolean bypassesArmor) {
            this.bypassesArmor = bypassesArmor;
            return this;
        }

        public CriterionConditionDamageSource.Builder bypassesInvulnerability(Boolean bypassesInvulnerability) {
            this.bypassesInvulnerability = bypassesInvulnerability;
            return this;
        }

        public CriterionConditionDamageSource.Builder bypassesMagic(Boolean bypassesMagic) {
            this.bypassesMagic = bypassesMagic;
            return this;
        }

        public CriterionConditionDamageSource.Builder isFire(Boolean fire) {
            this.isFire = fire;
            return this;
        }

        public CriterionConditionDamageSource.Builder isMagic(Boolean magic) {
            this.isMagic = magic;
            return this;
        }

        public CriterionConditionDamageSource.Builder isLightning(Boolean lightning) {
            this.isLightning = lightning;
            return this;
        }

        public CriterionConditionDamageSource.Builder direct(CriterionConditionEntity entity) {
            this.directEntity = entity;
            return this;
        }

        public CriterionConditionDamageSource.Builder direct(CriterionConditionEntity.Builder entity) {
            this.directEntity = entity.build();
            return this;
        }

        public CriterionConditionDamageSource.Builder source(CriterionConditionEntity entity) {
            this.sourceEntity = entity;
            return this;
        }

        public CriterionConditionDamageSource.Builder source(CriterionConditionEntity.Builder entity) {
            this.sourceEntity = entity.build();
            return this;
        }

        public CriterionConditionDamageSource build() {
            return new CriterionConditionDamageSource(this.isProjectile, this.isExplosion, this.bypassesArmor, this.bypassesInvulnerability, this.bypassesMagic, this.isFire, this.isMagic, this.isLightning, this.directEntity, this.sourceEntity);
        }
    }
}
