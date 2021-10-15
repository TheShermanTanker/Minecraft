package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;

public class CriterionConditionBlock {
    public static final CriterionConditionBlock ANY = new CriterionConditionBlock((Tag<Block>)null, (Set<Block>)null, CriterionTriggerProperties.ANY, CriterionConditionNBT.ANY);
    @Nullable
    private final Tag<Block> tag;
    @Nullable
    private final Set<Block> blocks;
    private final CriterionTriggerProperties properties;
    private final CriterionConditionNBT nbt;

    public CriterionConditionBlock(@Nullable Tag<Block> tag, @Nullable Set<Block> blocks, CriterionTriggerProperties state, CriterionConditionNBT nbt) {
        this.tag = tag;
        this.blocks = blocks;
        this.properties = state;
        this.nbt = nbt;
    }

    public boolean matches(WorldServer world, BlockPosition pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            IBlockData blockState = world.getType(pos);
            if (this.tag != null && !blockState.is(this.tag)) {
                return false;
            } else if (this.blocks != null && !this.blocks.contains(blockState.getBlock())) {
                return false;
            } else if (!this.properties.matches(blockState)) {
                return false;
            } else {
                if (this.nbt != CriterionConditionNBT.ANY) {
                    TileEntity blockEntity = world.getTileEntity(pos);
                    if (blockEntity == null || !this.nbt.matches(blockEntity.save(new NBTTagCompound()))) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static CriterionConditionBlock fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "block");
            CriterionConditionNBT nbtPredicate = CriterionConditionNBT.fromJson(jsonObject.get("nbt"));
            Set<Block> set = null;
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "blocks", (JsonArray)null);
            if (jsonArray != null) {
                ImmutableSet.Builder<Block> builder = ImmutableSet.builder();

                for(JsonElement jsonElement : jsonArray) {
                    MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.convertToString(jsonElement, "block"));
                    builder.add(IRegistry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown block id '" + resourceLocation + "'");
                    }));
                }

                set = builder.build();
            }

            Tag<Block> tag = null;
            if (jsonObject.has("tag")) {
                MinecraftKey resourceLocation2 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "tag"));
                tag = TagsInstance.getInstance().getTagOrThrow(IRegistry.BLOCK_REGISTRY, resourceLocation2, (resourceLocationx) -> {
                    return new JsonSyntaxException("Unknown block tag '" + resourceLocationx + "'");
                });
            }

            CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("state"));
            return new CriterionConditionBlock(tag, set, statePropertiesPredicate, nbtPredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.blocks != null) {
                JsonArray jsonArray = new JsonArray();

                for(Block block : this.blocks) {
                    jsonArray.add(IRegistry.BLOCK.getKey(block).toString());
                }

                jsonObject.add("blocks", jsonArray);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", TagsInstance.getInstance().getIdOrThrow(IRegistry.BLOCK_REGISTRY, this.tag, () -> {
                    return new IllegalStateException("Unknown block tag");
                }).toString());
            }

            jsonObject.add("nbt", this.nbt.serializeToJson());
            jsonObject.add("state", this.properties.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private Set<Block> blocks;
        @Nullable
        private Tag<Block> tag;
        private CriterionTriggerProperties properties = CriterionTriggerProperties.ANY;
        private CriterionConditionNBT nbt = CriterionConditionNBT.ANY;

        private Builder() {
        }

        public static CriterionConditionBlock.Builder block() {
            return new CriterionConditionBlock.Builder();
        }

        public CriterionConditionBlock.Builder of(Block... blocks) {
            this.blocks = ImmutableSet.copyOf(blocks);
            return this;
        }

        public CriterionConditionBlock.Builder of(Iterable<Block> blocks) {
            this.blocks = ImmutableSet.copyOf(blocks);
            return this;
        }

        public CriterionConditionBlock.Builder of(Tag<Block> tag) {
            this.tag = tag;
            return this;
        }

        public CriterionConditionBlock.Builder hasNbt(NBTTagCompound nbt) {
            this.nbt = new CriterionConditionNBT(nbt);
            return this;
        }

        public CriterionConditionBlock.Builder setProperties(CriterionTriggerProperties state) {
            this.properties = state;
            return this;
        }

        public CriterionConditionBlock build() {
            return new CriterionConditionBlock(this.tag, this.blocks, this.properties, this.nbt);
        }
    }
}
