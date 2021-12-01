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

public class CriterionTriggerEnterBlock extends CriterionTriggerAbstract<CriterionTriggerEnterBlock.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("enter_block");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerEnterBlock.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("state"));
        if (block != null) {
            statePropertiesPredicate.checkState(block.getStates(), (name) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + name);
            });
        }

        return new CriterionTriggerEnterBlock.CriterionInstanceTrigger(composite, block, statePropertiesPredicate);
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

    public void trigger(EntityPlayer player, IBlockData state) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(state);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        @Nullable
        private final Block block;
        private final CriterionTriggerProperties state;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, @Nullable Block block, CriterionTriggerProperties state) {
            super(CriterionTriggerEnterBlock.ID, player);
            this.block = block;
            this.state = state;
        }

        public static CriterionTriggerEnterBlock.CriterionInstanceTrigger entersBlock(Block block) {
            return new CriterionTriggerEnterBlock.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, block, CriterionTriggerProperties.ANY);
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
