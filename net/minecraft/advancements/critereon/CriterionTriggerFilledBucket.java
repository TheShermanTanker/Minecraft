package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class CriterionTriggerFilledBucket extends CriterionTriggerAbstract<CriterionTriggerFilledBucket.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("filled_bucket");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerFilledBucket.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerFilledBucket.TriggerInstance(composite, itemPredicate);
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(CriterionTriggerFilledBucket.ID, player);
            this.item = item;
        }

        public static CriterionTriggerFilledBucket.TriggerInstance filledBucket(CriterionConditionItem item) {
            return new CriterionTriggerFilledBucket.TriggerInstance(CriterionConditionEntity.Composite.ANY, item);
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
