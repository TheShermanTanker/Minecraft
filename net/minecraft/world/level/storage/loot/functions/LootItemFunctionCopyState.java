package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionCopyState extends LootItemFunctionConditional {
    final Block block;
    final Set<IBlockState<?>> properties;

    LootItemFunctionCopyState(LootItemCondition[] conditions, Block block, Set<IBlockState<?>> properties) {
        super(conditions);
        this.block = block;
        this.properties = properties;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_STATE;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.BLOCK_STATE);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootTableInfo context) {
        IBlockData blockState = context.getContextParameter(LootContextParameters.BLOCK_STATE);
        if (blockState != null) {
            NBTTagCompound compoundTag = stack.getOrCreateTag();
            NBTTagCompound compoundTag2;
            if (compoundTag.hasKeyOfType("BlockStateTag", 10)) {
                compoundTag2 = compoundTag.getCompound("BlockStateTag");
            } else {
                compoundTag2 = new NBTTagCompound();
                compoundTag.set("BlockStateTag", compoundTag2);
            }

            this.properties.stream().filter(blockState::hasProperty).forEach((property) -> {
                compoundTag2.setString(property.getName(), serialize(blockState, property));
            });
        }

        return stack;
    }

    public static LootItemFunctionCopyState.Builder copyState(Block block) {
        return new LootItemFunctionCopyState.Builder(block);
    }

    private static <T extends Comparable<T>> String serialize(IBlockData state, IBlockState<T> property) {
        T comparable = state.get(property);
        return property.getName(comparable);
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionCopyState.Builder> {
        private final Block block;
        private final Set<IBlockState<?>> properties = Sets.newHashSet();

        Builder(Block block) {
            this.block = block;
        }

        public LootItemFunctionCopyState.Builder copy(IBlockState<?> property) {
            if (!this.block.getStates().getProperties().contains(property)) {
                throw new IllegalStateException("Property " + property + " is not present on block " + this.block);
            } else {
                this.properties.add(property);
                return this;
            }
        }

        @Override
        protected LootItemFunctionCopyState.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionCopyState(this.getConditions(), this.block, this.properties);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionCopyState> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionCopyState object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("block", IRegistry.BLOCK.getKey(object.block).toString());
            JsonArray jsonArray = new JsonArray();
            object.properties.forEach((property) -> {
                jsonArray.add(property.getName());
            });
            json.add("properties", jsonArray);
        }

        @Override
        public LootItemFunctionCopyState deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "block"));
            Block block = IRegistry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new IllegalArgumentException("Can't find block " + resourceLocation);
            });
            BlockStateList<Block, IBlockData> stateDefinition = block.getStates();
            Set<IBlockState<?>> set = Sets.newHashSet();
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "properties", (JsonArray)null);
            if (jsonArray != null) {
                jsonArray.forEach((jsonElement) -> {
                    set.add(stateDefinition.getProperty(ChatDeserializer.convertToString(jsonElement, "property")));
                });
            }

            return new LootItemFunctionCopyState(lootItemConditions, block, set);
        }
    }
}
