package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerThrownItemPickedUpByEntity extends CriterionTriggerAbstract<CriterionTriggerThrownItemPickedUpByEntity.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("thrown_item_picked_up_by_entity");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    protected CriterionTriggerThrownItemPickedUpByEntity.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new CriterionTriggerThrownItemPickedUpByEntity.CriterionInstanceTrigger(composite, itemPredicate, composite2);
    }

    public void trigger(EntityPlayer player, ItemStack stack, Entity entity) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, stack, lootContext);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;
        private final CriterionConditionEntity.Composite entity;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionItem item, CriterionConditionEntity.Composite entity) {
            super(CriterionTriggerThrownItemPickedUpByEntity.ID, player);
            this.item = item;
            this.entity = entity;
        }

        public static CriterionTriggerThrownItemPickedUpByEntity.CriterionInstanceTrigger itemPickedUpByEntity(CriterionConditionEntity.Composite player, CriterionConditionItem.Builder item, CriterionConditionEntity.Composite entity) {
            return new CriterionTriggerThrownItemPickedUpByEntity.CriterionInstanceTrigger(player, item.build(), entity);
        }

        public boolean matches(EntityPlayer player, ItemStack stack, LootTableInfo entityContext) {
            if (!this.item.matches(stack)) {
                return false;
            } else {
                return this.entity.matches(entityContext);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
