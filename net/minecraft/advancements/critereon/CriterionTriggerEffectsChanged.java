package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerEffectsChanged extends CriterionTriggerAbstract<CriterionTriggerEffectsChanged.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("effects_changed");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerEffectsChanged.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionMobEffect mobEffectsPredicate = CriterionConditionMobEffect.fromJson(jsonObject.get("effects"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "source", deserializationContext);
        return new CriterionTriggerEffectsChanged.CriterionInstanceTrigger(composite, mobEffectsPredicate, composite2);
    }

    public void trigger(EntityPlayer player, @Nullable Entity source) {
        LootTableInfo lootContext = source != null ? CriterionConditionEntity.createContext(player, source) : null;
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionMobEffect effects;
        private final CriterionConditionEntity.Composite source;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionMobEffect effects, CriterionConditionEntity.Composite source) {
            super(CriterionTriggerEffectsChanged.ID, player);
            this.effects = effects;
            this.source = source;
        }

        public static CriterionTriggerEffectsChanged.CriterionInstanceTrigger hasEffects(CriterionConditionMobEffect effects) {
            return new CriterionTriggerEffectsChanged.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, effects, CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerEffectsChanged.CriterionInstanceTrigger gotEffectsFrom(CriterionConditionEntity source) {
            return new CriterionTriggerEffectsChanged.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionMobEffect.ANY, CriterionConditionEntity.Composite.wrap(source));
        }

        public boolean matches(EntityPlayer player, @Nullable LootTableInfo context) {
            if (!this.effects.matches((EntityLiving)player)) {
                return false;
            } else {
                return this.source == CriterionConditionEntity.Composite.ANY || context != null && this.source.matches(context);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("effects", this.effects.serializeToJson());
            jsonObject.add("source", this.source.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
