package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class CriterionTriggerEntityHurtPlayer extends CriterionTriggerAbstract<CriterionTriggerEntityHurtPlayer.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("entity_hurt_player");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerEntityHurtPlayer.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionDamage damagePredicate = CriterionConditionDamage.fromJson(jsonObject.get("damage"));
        return new CriterionTriggerEntityHurtPlayer.TriggerInstance(composite, damagePredicate);
    }

    public void trigger(EntityPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, source, dealt, taken, blocked);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionDamage damage;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionDamage damage) {
            super(CriterionTriggerEntityHurtPlayer.ID, player);
            this.damage = damage;
        }

        public static CriterionTriggerEntityHurtPlayer.TriggerInstance entityHurtPlayer() {
            return new CriterionTriggerEntityHurtPlayer.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionDamage.ANY);
        }

        public static CriterionTriggerEntityHurtPlayer.TriggerInstance entityHurtPlayer(CriterionConditionDamage predicate) {
            return new CriterionTriggerEntityHurtPlayer.TriggerInstance(CriterionConditionEntity.Composite.ANY, predicate);
        }

        public static CriterionTriggerEntityHurtPlayer.TriggerInstance entityHurtPlayer(CriterionConditionDamage.Builder damageBuilder) {
            return new CriterionTriggerEntityHurtPlayer.TriggerInstance(CriterionConditionEntity.Composite.ANY, damageBuilder.build());
        }

        public boolean matches(EntityPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
            return this.damage.matches(player, source, dealt, taken, blocked);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("damage", this.damage.serializeToJson());
            return jsonObject;
        }
    }
}
