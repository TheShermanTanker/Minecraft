package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerBredAnimals extends CriterionTriggerAbstract<CriterionTriggerBredAnimals.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("bred_animals");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerBredAnimals.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "parent", deserializationContext);
        CriterionConditionEntity.Composite composite3 = CriterionConditionEntity.Composite.fromJson(jsonObject, "partner", deserializationContext);
        CriterionConditionEntity.Composite composite4 = CriterionConditionEntity.Composite.fromJson(jsonObject, "child", deserializationContext);
        return new CriterionTriggerBredAnimals.CriterionInstanceTrigger(composite, composite2, composite3, composite4);
    }

    public void trigger(EntityPlayer player, EntityAnimal parent, EntityAnimal partner, @Nullable EntityAgeable child) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, parent);
        LootTableInfo lootContext2 = CriterionConditionEntity.createContext(player, partner);
        LootTableInfo lootContext3 = child != null ? CriterionConditionEntity.createContext(player, child) : null;
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, lootContext2, lootContext3);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite parent;
        private final CriterionConditionEntity.Composite partner;
        private final CriterionConditionEntity.Composite child;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite parent, CriterionConditionEntity.Composite partner, CriterionConditionEntity.Composite child) {
            super(CriterionTriggerBredAnimals.ID, player);
            this.parent = parent;
            this.partner = partner;
            this.child = child;
        }

        public static CriterionTriggerBredAnimals.CriterionInstanceTrigger bredAnimals() {
            return new CriterionTriggerBredAnimals.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY);
        }

        public static CriterionTriggerBredAnimals.CriterionInstanceTrigger bredAnimals(CriterionConditionEntity.Builder child) {
            return new CriterionTriggerBredAnimals.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(child.build()));
        }

        public static CriterionTriggerBredAnimals.CriterionInstanceTrigger bredAnimals(CriterionConditionEntity parent, CriterionConditionEntity partner, CriterionConditionEntity child) {
            return new CriterionTriggerBredAnimals.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(parent), CriterionConditionEntity.Composite.wrap(partner), CriterionConditionEntity.Composite.wrap(child));
        }

        public boolean matches(LootTableInfo parentContext, LootTableInfo partnerContext, @Nullable LootTableInfo childContext) {
            if (this.child == CriterionConditionEntity.Composite.ANY || childContext != null && this.child.matches(childContext)) {
                return this.parent.matches(parentContext) && this.partner.matches(partnerContext) || this.parent.matches(partnerContext) && this.partner.matches(parentContext);
            } else {
                return false;
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("parent", this.parent.toJson(predicateSerializer));
            jsonObject.add("partner", this.partner.toJson(predicateSerializer));
            jsonObject.add("child", this.child.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
