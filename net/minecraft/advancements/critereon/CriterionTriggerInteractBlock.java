package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

public class CriterionTriggerInteractBlock extends CriterionTriggerAbstract<CriterionTriggerInteractBlock.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("item_used_on_block");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerInteractBlock.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("location"));
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerInteractBlock.CriterionInstanceTrigger(composite, locationPredicate, itemPredicate);
    }

    public void trigger(EntityPlayer player, BlockPosition pos, ItemStack stack) {
        IBlockData blockState = player.getWorldServer().getType(pos);
        this.trigger(player, (conditions) -> {
            return conditions.matches(blockState, player.getWorldServer(), pos, stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionLocation location;
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionLocation location, CriterionConditionItem item) {
            super(CriterionTriggerInteractBlock.ID, player);
            this.location = location;
            this.item = item;
        }

        public static CriterionTriggerInteractBlock.CriterionInstanceTrigger itemUsedOnBlock(CriterionConditionLocation.Builder location, CriterionConditionItem.Builder item) {
            return new CriterionTriggerInteractBlock.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, location.build(), item.build());
        }

        public boolean matches(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
            return !this.location.matches(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) ? false : this.item.matches(stack);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
