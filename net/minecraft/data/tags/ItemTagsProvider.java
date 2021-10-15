package net.minecraft.data.tags;

import java.nio.file.Path;
import java.util.function.Function;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class ItemTagsProvider extends TagsProvider<Item> {
    private final Function<Tag.Named<Block>, Tag.Builder> blockTags;

    public ItemTagsProvider(DebugReportGenerator root, BlockTagsProvider blockTagsProvider) {
        super(root, IRegistry.ITEM);
        this.blockTags = blockTagsProvider::getOrCreateRawBuilder;
    }

    @Override
    protected void addTags() {
        this.copy(TagsBlock.WOOL, TagsItem.WOOL);
        this.copy(TagsBlock.PLANKS, TagsItem.PLANKS);
        this.copy(TagsBlock.STONE_BRICKS, TagsItem.STONE_BRICKS);
        this.copy(TagsBlock.WOODEN_BUTTONS, TagsItem.WOODEN_BUTTONS);
        this.copy(TagsBlock.BUTTONS, TagsItem.BUTTONS);
        this.copy(TagsBlock.CARPETS, TagsItem.CARPETS);
        this.copy(TagsBlock.WOODEN_DOORS, TagsItem.WOODEN_DOORS);
        this.copy(TagsBlock.WOODEN_STAIRS, TagsItem.WOODEN_STAIRS);
        this.copy(TagsBlock.WOODEN_SLABS, TagsItem.WOODEN_SLABS);
        this.copy(TagsBlock.WOODEN_FENCES, TagsItem.WOODEN_FENCES);
        this.copy(TagsBlock.WOODEN_PRESSURE_PLATES, TagsItem.WOODEN_PRESSURE_PLATES);
        this.copy(TagsBlock.DOORS, TagsItem.DOORS);
        this.copy(TagsBlock.SAPLINGS, TagsItem.SAPLINGS);
        this.copy(TagsBlock.OAK_LOGS, TagsItem.OAK_LOGS);
        this.copy(TagsBlock.DARK_OAK_LOGS, TagsItem.DARK_OAK_LOGS);
        this.copy(TagsBlock.BIRCH_LOGS, TagsItem.BIRCH_LOGS);
        this.copy(TagsBlock.ACACIA_LOGS, TagsItem.ACACIA_LOGS);
        this.copy(TagsBlock.SPRUCE_LOGS, TagsItem.SPRUCE_LOGS);
        this.copy(TagsBlock.JUNGLE_LOGS, TagsItem.JUNGLE_LOGS);
        this.copy(TagsBlock.CRIMSON_STEMS, TagsItem.CRIMSON_STEMS);
        this.copy(TagsBlock.WARPED_STEMS, TagsItem.WARPED_STEMS);
        this.copy(TagsBlock.LOGS_THAT_BURN, TagsItem.LOGS_THAT_BURN);
        this.copy(TagsBlock.LOGS, TagsItem.LOGS);
        this.copy(TagsBlock.SAND, TagsItem.SAND);
        this.copy(TagsBlock.SLABS, TagsItem.SLABS);
        this.copy(TagsBlock.WALLS, TagsItem.WALLS);
        this.copy(TagsBlock.STAIRS, TagsItem.STAIRS);
        this.copy(TagsBlock.ANVIL, TagsItem.ANVIL);
        this.copy(TagsBlock.RAILS, TagsItem.RAILS);
        this.copy(TagsBlock.LEAVES, TagsItem.LEAVES);
        this.copy(TagsBlock.WOODEN_TRAPDOORS, TagsItem.WOODEN_TRAPDOORS);
        this.copy(TagsBlock.TRAPDOORS, TagsItem.TRAPDOORS);
        this.copy(TagsBlock.SMALL_FLOWERS, TagsItem.SMALL_FLOWERS);
        this.copy(TagsBlock.BEDS, TagsItem.BEDS);
        this.copy(TagsBlock.FENCES, TagsItem.FENCES);
        this.copy(TagsBlock.TALL_FLOWERS, TagsItem.TALL_FLOWERS);
        this.copy(TagsBlock.FLOWERS, TagsItem.FLOWERS);
        this.copy(TagsBlock.SOUL_FIRE_BASE_BLOCKS, TagsItem.SOUL_FIRE_BASE_BLOCKS);
        this.copy(TagsBlock.CANDLES, TagsItem.CANDLES);
        this.copy(TagsBlock.OCCLUDES_VIBRATION_SIGNALS, TagsItem.OCCLUDES_VIBRATION_SIGNALS);
        this.copy(TagsBlock.GOLD_ORES, TagsItem.GOLD_ORES);
        this.copy(TagsBlock.IRON_ORES, TagsItem.IRON_ORES);
        this.copy(TagsBlock.DIAMOND_ORES, TagsItem.DIAMOND_ORES);
        this.copy(TagsBlock.REDSTONE_ORES, TagsItem.REDSTONE_ORES);
        this.copy(TagsBlock.LAPIS_ORES, TagsItem.LAPIS_ORES);
        this.copy(TagsBlock.COAL_ORES, TagsItem.COAL_ORES);
        this.copy(TagsBlock.EMERALD_ORES, TagsItem.EMERALD_ORES);
        this.copy(TagsBlock.COPPER_ORES, TagsItem.COPPER_ORES);
        this.tag(TagsItem.BANNERS).add(Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER, Items.LIGHT_BLUE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER, Items.PINK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER, Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER, Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER);
        this.tag(TagsItem.BOATS).add(Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT, Items.ACACIA_BOAT, Items.DARK_OAK_BOAT);
        this.tag(TagsItem.FISHES).add(Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH);
        this.copy(TagsBlock.STANDING_SIGNS, TagsItem.SIGNS);
        this.tag(TagsItem.CREEPER_DROP_MUSIC_DISCS).add(Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT);
        this.tag(TagsItem.MUSIC_DISCS).addTag(TagsItem.CREEPER_DROP_MUSIC_DISCS).add(Items.MUSIC_DISC_PIGSTEP);
        this.tag(TagsItem.COALS).add(Items.COAL, Items.CHARCOAL);
        this.tag(TagsItem.ARROWS).add(Items.ARROW, Items.TIPPED_ARROW, Items.SPECTRAL_ARROW);
        this.tag(TagsItem.LECTERN_BOOKS).add(Items.WRITTEN_BOOK, Items.WRITABLE_BOOK);
        this.tag(TagsItem.BEACON_PAYMENT_ITEMS).add(Items.NETHERITE_INGOT, Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT);
        this.tag(TagsItem.PIGLIN_REPELLENTS).add(Items.SOUL_TORCH).add(Items.SOUL_LANTERN).add(Items.SOUL_CAMPFIRE);
        this.tag(TagsItem.PIGLIN_LOVED).addTag(TagsItem.GOLD_ORES).add(Items.GOLD_BLOCK, Items.GILDED_BLACKSTONE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT, Items.BELL, Items.CLOCK, Items.GOLDEN_CARROT, Items.GLISTERING_MELON_SLICE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK);
        this.tag(TagsItem.IGNORED_BY_PIGLIN_BABIES).add(Items.LEATHER);
        this.tag(TagsItem.PIGLIN_FOOD).add(Items.PORKCHOP, Items.COOKED_PORKCHOP);
        this.tag(TagsItem.FOX_FOOD).add(Items.SWEET_BERRIES, Items.GLOW_BERRIES);
        this.tag(TagsItem.NON_FLAMMABLE_WOOD).add(Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM, Items.WARPED_HYPHAE, Items.STRIPPED_WARPED_HYPHAE, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_HYPHAE, Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS, Items.CRIMSON_SLAB, Items.WARPED_SLAB, Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE, Items.CRIMSON_FENCE, Items.WARPED_FENCE, Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE, Items.CRIMSON_STAIRS, Items.WARPED_STAIRS, Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.CRIMSON_DOOR, Items.WARPED_DOOR, Items.CRIMSON_SIGN, Items.WARPED_SIGN);
        this.tag(TagsItem.STONE_TOOL_MATERIALS).add(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE);
        this.tag(TagsItem.STONE_CRAFTING_MATERIALS).add(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE);
        this.tag(TagsItem.FREEZE_IMMUNE_WEARABLES).add(Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET, Items.LEATHER_HORSE_ARMOR);
        this.tag(TagsItem.AXOLOTL_TEMPT_ITEMS).add(Items.TROPICAL_FISH_BUCKET);
        this.tag(TagsItem.CLUSTER_MAX_HARVESTABLES).add(Items.DIAMOND_PICKAXE, Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE, Items.NETHERITE_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE);
    }

    protected void copy(Tag.Named<Block> blockTag, Tag.Named<Item> itemTag) {
        Tag.Builder builder = this.getOrCreateRawBuilder(itemTag);
        Tag.Builder builder2 = this.blockTags.apply(blockTag);
        builder2.getEntries().forEach(builder::add);
    }

    @Override
    protected Path getPath(MinecraftKey id) {
        return this.generator.getOutputFolder().resolve("data/" + id.getNamespace() + "/tags/items/" + id.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Item Tags";
    }
}
