package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;

public class LootItemFunctionCopyNBT extends LootItemFunctionConditional {
    final NbtProvider source;
    final List<LootItemFunctionCopyNBT.CopyOperation> operations;

    LootItemFunctionCopyNBT(LootItemCondition[] conditions, NbtProvider source, List<LootItemFunctionCopyNBT.CopyOperation> operations) {
        super(conditions);
        this.source = source;
        this.operations = ImmutableList.copyOf(operations);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_NBT;
    }

    static ArgumentNBTKey.NbtPath compileNbtPath(String nbtPath) {
        try {
            return (new ArgumentNBTKey()).parse(new StringReader(nbtPath));
        } catch (CommandSyntaxException var2) {
            throw new IllegalArgumentException("Failed to parse path " + nbtPath, var2);
        }
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.source.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        NBTBase tag = this.source.get(context);
        if (tag != null) {
            this.operations.forEach((operation) -> {
                operation.apply(stack::getOrCreateTag, tag);
            });
        }

        return stack;
    }

    public static LootItemFunctionCopyNBT.Builder copyData(NbtProvider source) {
        return new LootItemFunctionCopyNBT.Builder(source);
    }

    public static LootItemFunctionCopyNBT.Builder copyData(LootTableInfo.EntityTarget target) {
        return new LootItemFunctionCopyNBT.Builder(ContextNbtProvider.forContextEntity(target));
    }

    public static enum Action {
        REPLACE("replace") {
            @Override
            public void merge(NBTBase itemTag, ArgumentNBTKey.NbtPath targetPath, List<NBTBase> sourceTags) throws CommandSyntaxException {
                targetPath.set(itemTag, Iterables.getLast(sourceTags)::clone);
            }
        },
        APPEND("append") {
            @Override
            public void merge(NBTBase itemTag, ArgumentNBTKey.NbtPath targetPath, List<NBTBase> sourceTags) throws CommandSyntaxException {
                List<NBTBase> list = targetPath.getOrCreate(itemTag, NBTTagList::new);
                list.forEach((foundTag) -> {
                    if (foundTag instanceof NBTTagList) {
                        sourceTags.forEach((listTag) -> {
                            ((NBTTagList)foundTag).add(listTag.clone());
                        });
                    }

                });
            }
        },
        MERGE("merge") {
            @Override
            public void merge(NBTBase itemTag, ArgumentNBTKey.NbtPath targetPath, List<NBTBase> sourceTags) throws CommandSyntaxException {
                List<NBTBase> list = targetPath.getOrCreate(itemTag, NBTTagCompound::new);
                list.forEach((foundTag) -> {
                    if (foundTag instanceof NBTTagCompound) {
                        sourceTags.forEach((compoundTag) -> {
                            if (compoundTag instanceof NBTTagCompound) {
                                ((NBTTagCompound)foundTag).merge((NBTTagCompound)compoundTag);
                            }

                        });
                    }

                });
            }
        };

        final String name;

        public abstract void merge(NBTBase itemTag, ArgumentNBTKey.NbtPath targetPath, List<NBTBase> sourceTags) throws CommandSyntaxException;

        Action(String name) {
            this.name = name;
        }

        public static LootItemFunctionCopyNBT.Action getByName(String name) {
            for(LootItemFunctionCopyNBT.Action mergeStrategy : values()) {
                if (mergeStrategy.name.equals(name)) {
                    return mergeStrategy;
                }
            }

            throw new IllegalArgumentException("Invalid merge strategy" + name);
        }
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionCopyNBT.Builder> {
        private final NbtProvider source;
        private final List<LootItemFunctionCopyNBT.CopyOperation> ops = Lists.newArrayList();

        Builder(NbtProvider source) {
            this.source = source;
        }

        public LootItemFunctionCopyNBT.Builder copy(String source, String target, LootItemFunctionCopyNBT.Action operator) {
            this.ops.add(new LootItemFunctionCopyNBT.CopyOperation(source, target, operator));
            return this;
        }

        public LootItemFunctionCopyNBT.Builder copy(String source, String target) {
            return this.copy(source, target, LootItemFunctionCopyNBT.Action.REPLACE);
        }

        @Override
        protected LootItemFunctionCopyNBT.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionCopyNBT(this.getConditions(), this.source, this.ops);
        }
    }

    static class CopyOperation {
        private final String sourcePathText;
        private final ArgumentNBTKey.NbtPath sourcePath;
        private final String targetPathText;
        private final ArgumentNBTKey.NbtPath targetPath;
        private final LootItemFunctionCopyNBT.Action op;

        CopyOperation(String sourcePath, String targetPath, LootItemFunctionCopyNBT.Action operator) {
            this.sourcePathText = sourcePath;
            this.sourcePath = LootItemFunctionCopyNBT.compileNbtPath(sourcePath);
            this.targetPathText = targetPath;
            this.targetPath = LootItemFunctionCopyNBT.compileNbtPath(targetPath);
            this.op = operator;
        }

        public void apply(Supplier<NBTBase> itemTagTagGetter, NBTBase sourceEntityTag) {
            try {
                List<NBTBase> list = this.sourcePath.get(sourceEntityTag);
                if (!list.isEmpty()) {
                    this.op.merge(itemTagTagGetter.get(), this.targetPath, list);
                }
            } catch (CommandSyntaxException var4) {
            }

        }

        public JsonObject toJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("source", this.sourcePathText);
            jsonObject.addProperty("target", this.targetPathText);
            jsonObject.addProperty("op", this.op.name);
            return jsonObject;
        }

        public static LootItemFunctionCopyNBT.CopyOperation fromJson(JsonObject json) {
            String string = ChatDeserializer.getAsString(json, "source");
            String string2 = ChatDeserializer.getAsString(json, "target");
            LootItemFunctionCopyNBT.Action mergeStrategy = LootItemFunctionCopyNBT.Action.getByName(ChatDeserializer.getAsString(json, "op"));
            return new LootItemFunctionCopyNBT.CopyOperation(string, string2, mergeStrategy);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionCopyNBT> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionCopyNBT object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("source", context.serialize(object.source));
            JsonArray jsonArray = new JsonArray();
            object.operations.stream().map(LootItemFunctionCopyNBT.CopyOperation::toJson).forEach(jsonArray::add);
            json.add("ops", jsonArray);
        }

        @Override
        public LootItemFunctionCopyNBT deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NbtProvider nbtProvider = ChatDeserializer.getAsObject(jsonObject, "source", jsonDeserializationContext, NbtProvider.class);
            List<LootItemFunctionCopyNBT.CopyOperation> list = Lists.newArrayList();

            for(JsonElement jsonElement : ChatDeserializer.getAsJsonArray(jsonObject, "ops")) {
                JsonObject jsonObject2 = ChatDeserializer.convertToJsonObject(jsonElement, "op");
                list.add(LootItemFunctionCopyNBT.CopyOperation.fromJson(jsonObject2));
            }

            return new LootItemFunctionCopyNBT(lootItemConditions, nbtProvider, list);
        }
    }
}
