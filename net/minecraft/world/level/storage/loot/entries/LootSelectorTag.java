package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootSelectorTag extends LootSelectorEntry {
    final Tag<Item> tag;
    final boolean expand;

    LootSelectorTag(Tag<Item> tag, boolean bl, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.tag = tag;
        this.expand = bl;
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.TAG;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        this.tag.getTagged().forEach((item) -> {
            lootConsumer.accept(new ItemStack(item));
        });
    }

    private boolean expandTag(LootTableInfo context, Consumer<LootEntry> lootChoiceExpander) {
        if (!this.canRun(context)) {
            return false;
        } else {
            for(final Item item : this.tag.getTagged()) {
                lootChoiceExpander.accept(new LootSelectorEntry.EntryBase() {
                    @Override
                    public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
                        lootConsumer.accept(new ItemStack(item));
                    }
                });
            }

            return true;
        }
    }

    @Override
    public boolean expand(LootTableInfo context, Consumer<LootEntry> choiceConsumer) {
        return this.expand ? this.expandTag(context, choiceConsumer) : super.expand(context, choiceConsumer);
    }

    public static LootSelectorEntry.Builder<?> tagContents(Tag<Item> name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootSelectorTag(name, false, weight, quality, conditions, functions);
        });
    }

    public static LootSelectorEntry.Builder<?> expandTag(Tag<Item> name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootSelectorTag(name, true, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootSelectorEntry.Serializer<LootSelectorTag> {
        @Override
        public void serializeType(JsonObject json, LootSelectorTag entry, JsonSerializationContext context) {
            super.serializeType(json, entry, context);
            json.addProperty("name", TagsInstance.getInstance().getIdOrThrow(IRegistry.ITEM_REGISTRY, entry.tag, () -> {
                return new IllegalStateException("Unknown item tag");
            }).toString());
            json.addProperty("expand", entry.expand);
        }

        @Override
        protected LootSelectorTag deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "name"));
            Tag<Item> tag = TagsInstance.getInstance().getTagOrThrow(IRegistry.ITEM_REGISTRY, resourceLocation, (id) -> {
                return new JsonParseException("Can't find tag: " + id);
            });
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "expand");
            return new LootSelectorTag(tag, bl, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
