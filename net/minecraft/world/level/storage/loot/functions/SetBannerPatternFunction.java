package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnumBannerPatternType;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemFunctionConditional {
    final List<Pair<EnumBannerPatternType, EnumColor>> patterns;
    final boolean append;

    SetBannerPatternFunction(LootItemCondition[] condiitons, List<Pair<EnumBannerPatternType, EnumColor>> patterns, boolean append) {
        super(condiitons);
        this.patterns = patterns;
        this.append = append;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootTableInfo context) {
        NBTTagCompound compoundTag = ItemBlock.getBlockEntityData(stack);
        if (compoundTag == null) {
            compoundTag = new NBTTagCompound();
        }

        EnumBannerPatternType.Builder builder = new EnumBannerPatternType.Builder();
        this.patterns.forEach(builder::addPattern);
        NBTTagList listTag = builder.toListTag();
        NBTTagList listTag2;
        if (this.append) {
            listTag2 = compoundTag.getList("Patterns", 10).copy();
            listTag2.addAll(listTag);
        } else {
            listTag2 = listTag;
        }

        compoundTag.set("Patterns", listTag2);
        ItemBlock.setBlockEntityData(stack, TileEntityTypes.BANNER, compoundTag);
        return stack;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_BANNER_PATTERN;
    }

    public static SetBannerPatternFunction.Builder setBannerPattern(boolean append) {
        return new SetBannerPatternFunction.Builder(append);
    }

    public static class Builder extends LootItemFunctionConditional.Builder<SetBannerPatternFunction.Builder> {
        private final ImmutableList.Builder<Pair<EnumBannerPatternType, EnumColor>> patterns = ImmutableList.builder();
        private final boolean append;

        Builder(boolean append) {
            this.append = append;
        }

        @Override
        protected SetBannerPatternFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
        }

        public SetBannerPatternFunction.Builder addPattern(EnumBannerPatternType pattern, EnumColor color) {
            this.patterns.add(Pair.of(pattern, color));
            return this;
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<SetBannerPatternFunction> {
        @Override
        public void serialize(JsonObject json, SetBannerPatternFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonArray jsonArray = new JsonArray();
            object.patterns.forEach((pair) -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pattern", pair.getFirst().getFilename());
                jsonObject.addProperty("color", pair.getSecond().getName());
                jsonArray.add(jsonObject);
            });
            json.add("patterns", jsonArray);
            json.addProperty("append", object.append);
        }

        @Override
        public SetBannerPatternFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            ImmutableList.Builder<Pair<EnumBannerPatternType, EnumColor>> builder = ImmutableList.builder();
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "patterns");

            for(int i = 0; i < jsonArray.size(); ++i) {
                JsonObject jsonObject2 = ChatDeserializer.convertToJsonObject(jsonArray.get(i), "pattern[" + i + "]");
                String string = ChatDeserializer.getAsString(jsonObject2, "pattern");
                EnumBannerPatternType bannerPattern = EnumBannerPatternType.byFilename(string);
                if (bannerPattern == null) {
                    throw new JsonSyntaxException("Unknown pattern: " + string);
                }

                String string2 = ChatDeserializer.getAsString(jsonObject2, "color");
                EnumColor dyeColor = EnumColor.byName(string2, (EnumColor)null);
                if (dyeColor == null) {
                    throw new JsonSyntaxException("Unknown color: " + string2);
                }

                builder.add(Pair.of(bannerPattern, dyeColor));
            }

            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "append");
            return new SetBannerPatternFunction(lootItemConditions, builder.build(), bl);
        }
    }
}
