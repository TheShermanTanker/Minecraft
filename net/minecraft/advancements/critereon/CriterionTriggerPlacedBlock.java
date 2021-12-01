package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class CriterionTriggerPlacedBlock extends CriterionTriggerAbstract<CriterionTriggerPlacedBlock.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("placed_block");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerPlacedBlock.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("state"));
        if (block != null) {
            statePropertiesPredicate.checkState(block.getStates(), (name) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + name + ":");
            });
        }

        CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("location"));
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerPlacedBlock.CriterionInstanceTrigger(composite, block, statePropertiesPredicate, locationPredicate, itemPredicate);
    }

    @Nullable
    private static Block deserializeBlock(JsonObject obj) {
        if (obj.has("block")) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(obj, "block"));
            return IRegistry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block type '" + resourceLocation + "'");
            });
        } else {
            return null;
        }
    }

    public void trigger(EntityPlayer player, BlockPosition blockPos, ItemStack stack) {
        IBlockData blockState = player.getWorldServer().getType(blockPos);
        this.trigger(player, (conditions) -> {
            return conditions.matches(blockState, blockPos, player.getWorldServer(), stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        @Nullable
        private final Block block;
        private final CriterionTriggerProperties state;
        private final CriterionConditionLocation location;
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, @Nullable Block block, CriterionTriggerProperties state, CriterionConditionLocation location, CriterionConditionItem item) {
            super(CriterionTriggerPlacedBlock.ID, player);
            this.block = block;
            this.state = state;
            this.location = location;
            this.item = item;
        }

        public static CriterionTriggerPlacedBlock.CriterionInstanceTrigger placedBlock(Block block) {
            return new CriterionTriggerPlacedBlock.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, block, CriterionTriggerProperties.ANY, CriterionConditionLocation.ANY, CriterionConditionItem.ANY);
        }

        public boolean matches(IBlockData state, BlockPosition pos, WorldServer world, ItemStack stack) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else if (!this.state.matches(state)) {
                return false;
            } else if (!this.location.matches(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ())) {
                return false;
            } else {
                return this.item.matches(stack);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", IRegistry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("state", this.state.serializeToJson());
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
