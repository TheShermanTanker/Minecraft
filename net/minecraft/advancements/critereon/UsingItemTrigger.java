package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class UsingItemTrigger extends CriterionTriggerAbstract<UsingItemTrigger.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("using_item");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public UsingItemTrigger.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new UsingItemTrigger.TriggerInstance(composite, itemPredicate);
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(UsingItemTrigger.ID, player);
            this.item = item;
        }

        public static UsingItemTrigger.TriggerInstance lookingAt(CriterionConditionEntity.Builder player, CriterionConditionItem.Builder item) {
            return new UsingItemTrigger.TriggerInstance(CriterionConditionEntity.Composite.wrap(player.build()), item.build());
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
