package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerPlayerInteractedWithEntity extends CriterionTriggerAbstract<CriterionTriggerPlayerInteractedWithEntity.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("player_interacted_with_entity");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    protected CriterionTriggerPlayerInteractedWithEntity.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new CriterionTriggerPlayerInteractedWithEntity.CriterionInstanceTrigger(composite, itemPredicate, composite2);
    }

    public void trigger(EntityPlayer player, ItemStack stack, Entity entity) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, lootContext);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;
        private final CriterionConditionEntity.Composite entity;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionItem item, CriterionConditionEntity.Composite entity) {
            super(CriterionTriggerPlayerInteractedWithEntity.ID, player);
            this.item = item;
            this.entity = entity;
        }

        public static CriterionTriggerPlayerInteractedWithEntity.CriterionInstanceTrigger itemUsedOnEntity(CriterionConditionEntity.Composite player, CriterionConditionItem.Builder itemBuilder, CriterionConditionEntity.Composite entity) {
            return new CriterionTriggerPlayerInteractedWithEntity.CriterionInstanceTrigger(player, itemBuilder.build(), entity);
        }

        public boolean matches(ItemStack stack, LootTableInfo context) {
            return !this.item.matches(stack) ? false : this.entity.matches(context);
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
