package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class CriterionTriggerUseItem extends CriterionTriggerAbstract<CriterionTriggerUseItem.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("using_item");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerUseItem.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerUseItem.CriterionInstanceTrigger(composite, itemPredicate);
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(CriterionTriggerUseItem.ID, player);
            this.item = item;
        }

        public static CriterionTriggerUseItem.CriterionInstanceTrigger lookingAt(CriterionConditionEntity.Builder player, CriterionConditionItem.Builder item) {
            return new CriterionTriggerUseItem.CriterionInstanceTrigger(CriterionConditionEntity.Composite.wrap(player.build()), item.build());
        }

        public boolean matches(ItemStack stack) {
            return this.item.matches(stack);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
