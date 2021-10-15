package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class CriterionTriggerItemDurabilityChanged extends CriterionTriggerAbstract<CriterionTriggerItemDurabilityChanged.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("item_durability_changed");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerItemDurabilityChanged.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("durability"));
        CriterionConditionValue.IntegerRange ints2 = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("delta"));
        return new CriterionTriggerItemDurabilityChanged.TriggerInstance(composite, itemPredicate, ints, ints2);
    }

    public void trigger(EntityPlayer player, ItemStack stack, int durability) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, durability);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;
        private final CriterionConditionValue.IntegerRange durability;
        private final CriterionConditionValue.IntegerRange delta;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem item, CriterionConditionValue.IntegerRange durability, CriterionConditionValue.IntegerRange delta) {
            super(CriterionTriggerItemDurabilityChanged.ID, player);
            this.item = item;
            this.durability = durability;
            this.delta = delta;
        }

        public static CriterionTriggerItemDurabilityChanged.TriggerInstance changedDurability(CriterionConditionItem item, CriterionConditionValue.IntegerRange durability) {
            return changedDurability(CriterionConditionEntity.Composite.ANY, item, durability);
        }

        public static CriterionTriggerItemDurabilityChanged.TriggerInstance changedDurability(CriterionConditionEntity.Composite player, CriterionConditionItem item, CriterionConditionValue.IntegerRange durability) {
            return new CriterionTriggerItemDurabilityChanged.TriggerInstance(player, item, durability, CriterionConditionValue.IntegerRange.ANY);
        }

        public boolean matches(ItemStack stack, int durability) {
            if (!this.item.matches(stack)) {
                return false;
            } else if (!this.durability.matches(stack.getMaxDamage() - durability)) {
                return false;
            } else {
                return this.delta.matches(stack.getDamage() - durability);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("durability", this.durability.serializeToJson());
            jsonObject.add("delta", this.delta.serializeToJson());
            return jsonObject;
        }
    }
}
