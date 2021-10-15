package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import net.minecraft.advancements.critereon.CriterionTriggerProperties;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionBlockStateProperty implements LootItemCondition {
    final Block block;
    final CriterionTriggerProperties properties;

    LootItemConditionBlockStateProperty(Block block, CriterionTriggerProperties properties) {
        this.block = block;
        this.properties = properties;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.BLOCK_STATE_PROPERTY;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.BLOCK_STATE);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        IBlockData blockState = lootContext.getContextParameter(LootContextParameters.BLOCK_STATE);
        return blockState != null && blockState.is(this.block) && this.properties.matches(blockState);
    }

    public static LootItemConditionBlockStateProperty.Builder hasBlockStateProperties(Block block) {
        return new LootItemConditionBlockStateProperty.Builder(block);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Block block;
        private CriterionTriggerProperties properties = CriterionTriggerProperties.ANY;

        public Builder(Block block) {
            this.block = block;
        }

        public LootItemConditionBlockStateProperty.Builder setProperties(CriterionTriggerProperties.Builder builder) {
            this.properties = builder.build();
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new LootItemConditionBlockStateProperty(this.block, this.properties);
        }
    }

    public static class Serializer implements LootSerializer<LootItemConditionBlockStateProperty> {
        @Override
        public void serialize(JsonObject json, LootItemConditionBlockStateProperty object, JsonSerializationContext context) {
            json.addProperty("block", IRegistry.BLOCK.getKey(object.block).toString());
            json.add("properties", object.properties.serializeToJson());
        }

        @Override
        public LootItemConditionBlockStateProperty deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "block"));
            Block block = IRegistry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new IllegalArgumentException("Can't find block " + resourceLocation);
            });
            CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("properties"));
            statePropertiesPredicate.checkState(block.getStates(), (propertyName) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + propertyName);
            });
            return new LootItemConditionBlockStateProperty(block, statePropertiesPredicate);
        }
    }
}
