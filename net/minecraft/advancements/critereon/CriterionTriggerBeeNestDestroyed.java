package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class CriterionTriggerBeeNestDestroyed extends CriterionTriggerAbstract<CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("bee_nest_destroyed");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("num_bees_inside"));
        return new CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger(composite, block, itemPredicate, ints);
    }

    @Nullable
    private static Block deserializeBlock(JsonObject root) {
        if (root.has("block")) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(root, "block"));
            return IRegistry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block type '" + resourceLocation + "'");
            });
        } else {
            return null;
        }
    }

    public void trigger(EntityPlayer player, IBlockData state, ItemStack stack, int beeCount) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(state, stack, beeCount);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        @Nullable
        private final Block block;
        private final CriterionConditionItem item;
        private final CriterionConditionValue.IntegerRange numBees;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, @Nullable Block block, CriterionConditionItem item, CriterionConditionValue.IntegerRange beeCount) {
            super(CriterionTriggerBeeNestDestroyed.ID, player);
            this.block = block;
            this.item = item;
            this.numBees = beeCount;
        }

        public static CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger destroyedBeeNest(Block block, CriterionConditionItem.Builder itemPredicateBuilder, CriterionConditionValue.IntegerRange beeCountRange) {
            return new CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, block, itemPredicateBuilder.build(), beeCountRange);
        }

        public boolean matches(IBlockData state, ItemStack stack, int count) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else {
                return !this.item.matches(stack) ? false : this.numBees.matches(count);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", IRegistry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("num_bees_inside", this.numBees.serializeToJson());
            return jsonObject;
        }
    }
}
