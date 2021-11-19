package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.level.IMaterial;

public class CriterionTriggerConsumeItem extends CriterionTriggerAbstract<CriterionTriggerConsumeItem.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("consume_item");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerConsumeItem.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        return new CriterionTriggerConsumeItem.CriterionInstanceTrigger(composite, CriterionConditionItem.fromJson(jsonObject.get("item")));
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(CriterionTriggerConsumeItem.ID, player);
            this.item = item;
        }

        public static CriterionTriggerConsumeItem.CriterionInstanceTrigger usedItem() {
            return new CriterionTriggerConsumeItem.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionItem.ANY);
        }

        public static CriterionTriggerConsumeItem.CriterionInstanceTrigger usedItem(CriterionConditionItem predicate) {
            return new CriterionTriggerConsumeItem.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, predicate);
        }

        public static CriterionTriggerConsumeItem.CriterionInstanceTrigger usedItem(IMaterial item) {
            return new CriterionTriggerConsumeItem.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, new CriterionConditionItem((Tag<Item>)null, ImmutableSet.of(item.getItem()), CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionEnchantments.NONE, CriterionConditionEnchantments.NONE, (PotionRegistry)null, CriterionConditionNBT.ANY));
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
