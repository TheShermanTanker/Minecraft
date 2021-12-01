package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;

public final class RecipeItemStack implements Predicate<ItemStack> {
    public static final RecipeItemStack EMPTY = new RecipeItemStack(Stream.empty());
    private final RecipeItemStack.Provider[] values;
    @Nullable
    public ItemStack[] itemStacks;
    @Nullable
    private IntList stackingIds;

    public RecipeItemStack(Stream<? extends RecipeItemStack.Provider> entries) {
        this.values = entries.toArray((i) -> {
            return new RecipeItemStack.Provider[i];
        });
    }

    public ItemStack[] getItems() {
        this.buildChoices();
        return this.itemStacks;
    }

    public void buildChoices() {
        if (this.itemStacks == null) {
            this.itemStacks = Arrays.stream(this.values).flatMap((value) -> {
                return value.getItems().stream();
            }).distinct().toArray((i) -> {
                return new ItemStack[i];
            });
        }

    }

    @Override
    public boolean test(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            this.buildChoices();
            if (this.itemStacks.length == 0) {
                return itemStack.isEmpty();
            } else {
                for(ItemStack itemStack2 : this.itemStacks) {
                    if (itemStack2.is(itemStack.getItem())) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public IntList getStackingIds() {
        if (this.stackingIds == null) {
            this.buildChoices();
            this.stackingIds = new IntArrayList(this.itemStacks.length);

            for(ItemStack itemStack : this.itemStacks) {
                this.stackingIds.add(AutoRecipeStackManager.getStackingIndex(itemStack));
            }

            this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
        }

        return this.stackingIds;
    }

    public void toNetwork(PacketDataSerializer buf) {
        this.buildChoices();
        buf.writeCollection(Arrays.asList(this.itemStacks), PacketDataSerializer::writeItem);
    }

    public JsonElement toJson() {
        if (this.values.length == 1) {
            return this.values[0].serialize();
        } else {
            JsonArray jsonArray = new JsonArray();

            for(RecipeItemStack.Provider value : this.values) {
                jsonArray.add(value.serialize());
            }

            return jsonArray;
        }
    }

    public boolean isEmpty() {
        return this.values.length == 0 && (this.itemStacks == null || this.itemStacks.length == 0) && (this.stackingIds == null || this.stackingIds.isEmpty());
    }

    private static RecipeItemStack fromValues(Stream<? extends RecipeItemStack.Provider> entries) {
        RecipeItemStack ingredient = new RecipeItemStack(entries);
        return ingredient.values.length == 0 ? EMPTY : ingredient;
    }

    public static RecipeItemStack of() {
        return EMPTY;
    }

    public static RecipeItemStack of(IMaterial... items) {
        return of(Arrays.stream(items).map(ItemStack::new));
    }

    public static RecipeItemStack of(ItemStack... stacks) {
        return of(Arrays.stream(stacks));
    }

    public static RecipeItemStack of(Stream<ItemStack> stacks) {
        return fromValues(stacks.filter((stack) -> {
            return !stack.isEmpty();
        }).map(RecipeItemStack.StackProvider::new));
    }

    public static RecipeItemStack of(Tag<Item> tag) {
        return fromValues(Stream.of(new RecipeItemStack.TagValue(tag)));
    }

    public static RecipeItemStack fromNetwork(PacketDataSerializer buf) {
        return fromValues(buf.readList(PacketDataSerializer::readItem).stream().map(RecipeItemStack.StackProvider::new));
    }

    public static RecipeItemStack fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return fromValues(Stream.of(valueFromJson(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray jsonArray = json.getAsJsonArray();
                if (jsonArray.size() == 0) {
                    throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
                } else {
                    return fromValues(StreamSupport.stream(jsonArray.spliterator(), false).map((jsonElement) -> {
                        return valueFromJson(ChatDeserializer.convertToJsonObject(jsonElement, "item"));
                    }));
                }
            } else {
                throw new JsonSyntaxException("Expected item to be object or array of objects");
            }
        } else {
            throw new JsonSyntaxException("Item cannot be null");
        }
    }

    private static RecipeItemStack.Provider valueFromJson(JsonObject json) {
        if (json.has("item") && json.has("tag")) {
            throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
        } else if (json.has("item")) {
            Item item = ShapedRecipes.itemFromJson(json);
            return new RecipeItemStack.StackProvider(new ItemStack(item));
        } else if (json.has("tag")) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(json, "tag"));
            Tag<Item> tag = TagsInstance.getInstance().getTagOrThrow(IRegistry.ITEM_REGISTRY, resourceLocation, (resourceLocationx) -> {
                return new JsonSyntaxException("Unknown item tag '" + resourceLocationx + "'");
            });
            return new RecipeItemStack.TagValue(tag);
        } else {
            throw new JsonParseException("An ingredient entry needs either a tag or an item");
        }
    }

    public interface Provider {
        Collection<ItemStack> getItems();

        JsonObject serialize();
    }

    public static class StackProvider implements RecipeItemStack.Provider {
        private final ItemStack item;

        public StackProvider(ItemStack stack) {
            this.item = stack;
        }

        @Override
        public Collection<ItemStack> getItems() {
            return Collections.singleton(this.item);
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", IRegistry.ITEM.getKey(this.item.getItem()).toString());
            return jsonObject;
        }
    }

    static class TagValue implements RecipeItemStack.Provider {
        private final Tag<Item> tag;

        TagValue(Tag<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemStack> getItems() {
            List<ItemStack> list = Lists.newArrayList();

            for(Item item : this.tag.getTagged()) {
                list.add(new ItemStack(item));
            }

            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("tag", TagsInstance.getInstance().getIdOrThrow(IRegistry.ITEM_REGISTRY, this.tag, () -> {
                return new IllegalStateException("Unknown item tag");
            }).toString());
            return jsonObject;
        }
    }
}
