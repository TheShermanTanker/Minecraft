package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.tags.Tag;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.level.IMaterial;

public class CriterionTriggerInventoryChanged extends CriterionTriggerAbstract<CriterionTriggerInventoryChanged.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("inventory_changed");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerInventoryChanged.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        JsonObject jsonObject2 = ChatDeserializer.getAsJsonObject(jsonObject, "slots", new JsonObject());
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject2.get("occupied"));
        CriterionConditionValue.IntegerRange ints2 = CriterionConditionValue.IntegerRange.fromJson(jsonObject2.get("full"));
        CriterionConditionValue.IntegerRange ints3 = CriterionConditionValue.IntegerRange.fromJson(jsonObject2.get("empty"));
        CriterionConditionItem[] itemPredicates = CriterionConditionItem.fromJsonArray(jsonObject.get("items"));
        return new CriterionTriggerInventoryChanged.TriggerInstance(composite, ints, ints2, ints3, itemPredicates);
    }

    public void trigger(EntityPlayer player, PlayerInventory inventory, ItemStack stack) {
        int i = 0;
        int j = 0;
        int k = 0;

        for(int l = 0; l < inventory.getSize(); ++l) {
            ItemStack itemStack = inventory.getItem(l);
            if (itemStack.isEmpty()) {
                ++j;
            } else {
                ++k;
                if (itemStack.getCount() >= itemStack.getMaxStackSize()) {
                    ++i;
                }
            }
        }

        this.trigger(player, inventory, stack, i, j, k);
    }

    private void trigger(EntityPlayer player, PlayerInventory inventory, ItemStack stack, int full, int empty, int occupied) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(inventory, stack, full, empty, occupied);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionValue.IntegerRange slotsOccupied;
        private final CriterionConditionValue.IntegerRange slotsFull;
        private final CriterionConditionValue.IntegerRange slotsEmpty;
        private final CriterionConditionItem[] predicates;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionValue.IntegerRange occupied, CriterionConditionValue.IntegerRange full, CriterionConditionValue.IntegerRange empty, CriterionConditionItem[] items) {
            super(CriterionTriggerInventoryChanged.ID, player);
            this.slotsOccupied = occupied;
            this.slotsFull = full;
            this.slotsEmpty = empty;
            this.predicates = items;
        }

        public static CriterionTriggerInventoryChanged.TriggerInstance hasItems(CriterionConditionItem... items) {
            return new CriterionTriggerInventoryChanged.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, items);
        }

        public static CriterionTriggerInventoryChanged.TriggerInstance hasItems(IMaterial... items) {
            CriterionConditionItem[] itemPredicates = new CriterionConditionItem[items.length];

            for(int i = 0; i < items.length; ++i) {
                itemPredicates[i] = new CriterionConditionItem((Tag<Item>)null, ImmutableSet.of(items[i].getItem()), CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionEnchantments.NONE, CriterionConditionEnchantments.NONE, (PotionRegistry)null, CriterionConditionNBT.ANY);
            }

            return hasItems(itemPredicates);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (!this.slotsOccupied.isAny() || !this.slotsFull.isAny() || !this.slotsEmpty.isAny()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("occupied", this.slotsOccupied.serializeToJson());
                jsonObject2.add("full", this.slotsFull.serializeToJson());
                jsonObject2.add("empty", this.slotsEmpty.serializeToJson());
                jsonObject.add("slots", jsonObject2);
            }

            if (this.predicates.length > 0) {
                JsonArray jsonArray = new JsonArray();

                for(CriterionConditionItem itemPredicate : this.predicates) {
                    jsonArray.add(itemPredicate.serializeToJson());
                }

                jsonObject.add("items", jsonArray);
            }

            return jsonObject;
        }

        public boolean matches(PlayerInventory inventory, ItemStack stack, int full, int empty, int occupied) {
            if (!this.slotsFull.matches(full)) {
                return false;
            } else if (!this.slotsEmpty.matches(empty)) {
                return false;
            } else if (!this.slotsOccupied.matches(occupied)) {
                return false;
            } else {
                int i = this.predicates.length;
                if (i == 0) {
                    return true;
                } else if (i != 1) {
                    List<CriterionConditionItem> list = new ObjectArrayList<>(this.predicates);
                    int j = inventory.getSize();

                    for(int k = 0; k < j; ++k) {
                        if (list.isEmpty()) {
                            return true;
                        }

                        ItemStack itemStack = inventory.getItem(k);
                        if (!itemStack.isEmpty()) {
                            list.removeIf((itemPredicate) -> {
                                return itemPredicate.matches(itemStack);
                            });
                        }
                    }

                    return list.isEmpty();
                } else {
                    return !stack.isEmpty() && this.predicates[0].matches(stack);
                }
            }
        }
    }
}
