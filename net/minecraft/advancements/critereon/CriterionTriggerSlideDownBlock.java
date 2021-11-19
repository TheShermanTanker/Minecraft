package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class CriterionTriggerSlideDownBlock extends CriterionTriggerAbstract<CriterionTriggerSlideDownBlock.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("slide_down_block");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerSlideDownBlock.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("state"));
        if (block != null) {
            statePropertiesPredicate.checkState(block.getStates(), (key) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + key);
            });
        }

        return new CriterionTriggerSlideDownBlock.CriterionInstanceTrigger(composite, block, statePropertiesPredicate);
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

    public void trigger(EntityPlayer player, IBlockData state) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(state);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final Block block;
        private final CriterionTriggerProperties state;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, @Nullable Block block, CriterionTriggerProperties state) {
            super(CriterionTriggerSlideDownBlock.ID, player);
            this.block = block;
            this.state = state;
        }

        public static CriterionTriggerSlideDownBlock.CriterionInstanceTrigger slidesDownBlock(Block block) {
            return new CriterionTriggerSlideDownBlock.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, block, CriterionTriggerProperties.ANY);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", IRegistry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("state", this.state.serializeToJson());
            return jsonObject;
        }

        public boolean matches(IBlockData state) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else {
                return this.state.matches(state);
            }
        }
    }
}