package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;

public class CriterionTriggerUsedTotem extends CriterionTriggerAbstract<CriterionTriggerUsedTotem.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("used_totem");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerUsedTotem.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerUsedTotem.TriggerInstance(composite, itemPredicate);
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(CriterionTriggerUsedTotem.ID, player);
            this.item = item;
        }

        public static CriterionTriggerUsedTotem.TriggerInstance usedTotem(CriterionConditionItem itemPredicate) {
            return new CriterionTriggerUsedTotem.TriggerInstance(CriterionConditionEntity.Composite.ANY, itemPredicate);
        }

        public static CriterionTriggerUsedTotem.TriggerInstance usedTotem(IMaterial item) {
            return new CriterionTriggerUsedTotem.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionItem.Builder.item().of(item).build());
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
