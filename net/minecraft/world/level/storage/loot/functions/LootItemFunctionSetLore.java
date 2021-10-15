package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionSetLore extends LootItemFunctionConditional {
    final boolean replace;
    final List<IChatBaseComponent> lore;
    @Nullable
    final LootTableInfo.EntityTarget resolutionContext;

    public LootItemFunctionSetLore(LootItemCondition[] conditions, boolean replace, List<IChatBaseComponent> lore, @Nullable LootTableInfo.EntityTarget entity) {
        super(conditions);
        this.replace = replace;
        this.lore = ImmutableList.copyOf(lore);
        this.resolutionContext = entity;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LORE;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.resolutionContext != null ? ImmutableSet.of(this.resolutionContext.getParam()) : ImmutableSet.of();
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        NBTTagList listTag = this.getLoreTag(stack, !this.lore.isEmpty());
        if (listTag != null) {
            if (this.replace) {
                listTag.clear();
            }

            UnaryOperator<IChatBaseComponent> unaryOperator = LootItemFunctionSetName.createResolver(context, this.resolutionContext);
            this.lore.stream().map(unaryOperator).map(IChatBaseComponent.ChatSerializer::toJson).map(NBTTagString::valueOf).forEach(listTag::add);
        }

        return stack;
    }

    @Nullable
    private NBTTagList getLoreTag(ItemStack stack, boolean otherLoreExists) {
        NBTTagCompound compoundTag;
        if (stack.hasTag()) {
            compoundTag = stack.getTag();
        } else {
            if (!otherLoreExists) {
                return null;
            }

            compoundTag = new NBTTagCompound();
            stack.setTag(compoundTag);
        }

        NBTTagCompound compoundTag4;
        if (compoundTag.hasKeyOfType("display", 10)) {
            compoundTag4 = compoundTag.getCompound("display");
        } else {
            if (!otherLoreExists) {
                return null;
            }

            compoundTag4 = new NBTTagCompound();
            compoundTag.set("display", compoundTag4);
        }

        if (compoundTag4.hasKeyOfType("Lore", 9)) {
            return compoundTag4.getList("Lore", 8);
        } else if (otherLoreExists) {
            NBTTagList listTag = new NBTTagList();
            compoundTag4.set("Lore", listTag);
            return listTag;
        } else {
            return null;
        }
    }

    public static LootItemFunctionSetLore.Builder setLore() {
        return new LootItemFunctionSetLore.Builder();
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionSetLore.Builder> {
        private boolean replace;
        private LootTableInfo.EntityTarget resolutionContext;
        private final List<IChatBaseComponent> lore = Lists.newArrayList();

        public LootItemFunctionSetLore.Builder setReplace(boolean replace) {
            this.replace = replace;
            return this;
        }

        public LootItemFunctionSetLore.Builder setResolutionContext(LootTableInfo.EntityTarget target) {
            this.resolutionContext = target;
            return this;
        }

        public LootItemFunctionSetLore.Builder addLine(IChatBaseComponent lore) {
            this.lore.add(lore);
            return this;
        }

        @Override
        protected LootItemFunctionSetLore.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionSetLore(this.getConditions(), this.replace, this.lore, this.resolutionContext);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetLore> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetLore object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("replace", object.replace);
            JsonArray jsonArray = new JsonArray();

            for(IChatBaseComponent component : object.lore) {
                jsonArray.add(IChatBaseComponent.ChatSerializer.toJsonTree(component));
            }

            json.add("lore", jsonArray);
            if (object.resolutionContext != null) {
                json.add("entity", context.serialize(object.resolutionContext));
            }

        }

        @Override
        public LootItemFunctionSetLore deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "replace", false);
            List<IChatBaseComponent> list = Streams.stream(ChatDeserializer.getAsJsonArray(jsonObject, "lore")).map(IChatBaseComponent.ChatSerializer::fromJson).collect(ImmutableList.toImmutableList());
            LootTableInfo.EntityTarget entityTarget = ChatDeserializer.getAsObject(jsonObject, "entity", (LootTableInfo.EntityTarget)null, jsonDeserializationContext, LootTableInfo.EntityTarget.class);
            return new LootItemFunctionSetLore(lootItemConditions, bl, list, entityTarget);
        }
    }
}
