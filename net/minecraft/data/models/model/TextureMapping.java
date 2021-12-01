package net.minecraft.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class TextureMapping {
    private final Map<TextureSlot, MinecraftKey> slots = Maps.newHashMap();
    private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

    public TextureMapping put(TextureSlot key, MinecraftKey id) {
        this.slots.put(key, id);
        return this;
    }

    public TextureMapping putForced(TextureSlot key, MinecraftKey id) {
        this.slots.put(key, id);
        this.forcedSlots.add(key);
        return this;
    }

    public Stream<TextureSlot> getForced() {
        return this.forcedSlots.stream();
    }

    public TextureMapping copySlot(TextureSlot parent, TextureSlot child) {
        this.slots.put(child, this.slots.get(parent));
        return this;
    }

    public TextureMapping copyForced(TextureSlot parent, TextureSlot child) {
        this.slots.put(child, this.slots.get(parent));
        this.forcedSlots.add(child);
        return this;
    }

    public MinecraftKey get(TextureSlot key) {
        for(TextureSlot textureSlot = key; textureSlot != null; textureSlot = textureSlot.getParent()) {
            MinecraftKey resourceLocation = this.slots.get(textureSlot);
            if (resourceLocation != null) {
                return resourceLocation;
            }
        }

        throw new IllegalStateException("Can't find texture for slot " + key);
    }

    public TextureMapping copyAndUpdate(TextureSlot key, MinecraftKey id) {
        TextureMapping textureMapping = new TextureMapping();
        textureMapping.slots.putAll(this.slots);
        textureMapping.forcedSlots.addAll(this.forcedSlots);
        textureMapping.put(key, id);
        return textureMapping;
    }

    public static TextureMapping cube(Block block) {
        MinecraftKey resourceLocation = getBlockTexture(block);
        return cube(resourceLocation);
    }

    public static TextureMapping defaultTexture(Block block) {
        MinecraftKey resourceLocation = getBlockTexture(block);
        return defaultTexture(resourceLocation);
    }

    public static TextureMapping defaultTexture(MinecraftKey id) {
        return (new TextureMapping()).put(TextureSlot.TEXTURE, id);
    }

    public static TextureMapping cube(MinecraftKey id) {
        return (new TextureMapping()).put(TextureSlot.ALL, id);
    }

    public static TextureMapping cross(Block block) {
        return singleSlot(TextureSlot.CROSS, getBlockTexture(block));
    }

    public static TextureMapping cross(MinecraftKey id) {
        return singleSlot(TextureSlot.CROSS, id);
    }

    public static TextureMapping plant(Block block) {
        return singleSlot(TextureSlot.PLANT, getBlockTexture(block));
    }

    public static TextureMapping plant(MinecraftKey id) {
        return singleSlot(TextureSlot.PLANT, id);
    }

    public static TextureMapping rail(Block block) {
        return singleSlot(TextureSlot.RAIL, getBlockTexture(block));
    }

    public static TextureMapping rail(MinecraftKey id) {
        return singleSlot(TextureSlot.RAIL, id);
    }

    public static TextureMapping wool(Block block) {
        return singleSlot(TextureSlot.WOOL, getBlockTexture(block));
    }

    public static TextureMapping wool(MinecraftKey id) {
        return singleSlot(TextureSlot.WOOL, id);
    }

    public static TextureMapping stem(Block block) {
        return singleSlot(TextureSlot.STEM, getBlockTexture(block));
    }

    public static TextureMapping attachedStem(Block stem, Block upper) {
        return (new TextureMapping()).put(TextureSlot.STEM, getBlockTexture(stem)).put(TextureSlot.UPPER_STEM, getBlockTexture(upper));
    }

    public static TextureMapping pattern(Block block) {
        return singleSlot(TextureSlot.PATTERN, getBlockTexture(block));
    }

    public static TextureMapping fan(Block block) {
        return singleSlot(TextureSlot.FAN, getBlockTexture(block));
    }

    public static TextureMapping crop(MinecraftKey id) {
        return singleSlot(TextureSlot.CROP, id);
    }

    public static TextureMapping pane(Block block, Block top) {
        return (new TextureMapping()).put(TextureSlot.PANE, getBlockTexture(block)).put(TextureSlot.EDGE, getBlockTexture(top, "_top"));
    }

    public static TextureMapping singleSlot(TextureSlot key, MinecraftKey id) {
        return (new TextureMapping()).put(key, id);
    }

    public static TextureMapping column(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping cubeTop(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping logColumn(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block)).put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping column(MinecraftKey side, MinecraftKey end) {
        return (new TextureMapping()).put(TextureSlot.SIDE, side).put(TextureSlot.END, end);
    }

    public static TextureMapping cubeBottomTop(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping cubeBottomTopWithWall(Block block) {
        MinecraftKey resourceLocation = getBlockTexture(block);
        return (new TextureMapping()).put(TextureSlot.WALL, resourceLocation).put(TextureSlot.SIDE, resourceLocation).put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping columnWithWall(Block block) {
        MinecraftKey resourceLocation = getBlockTexture(block);
        return (new TextureMapping()).put(TextureSlot.WALL, resourceLocation).put(TextureSlot.SIDE, resourceLocation).put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping door(MinecraftKey top, MinecraftKey bottom) {
        return (new TextureMapping()).put(TextureSlot.TOP, top).put(TextureSlot.BOTTOM, bottom);
    }

    public static TextureMapping door(Block block) {
        return (new TextureMapping()).put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping particle(Block block) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getBlockTexture(block));
    }

    public static TextureMapping particle(MinecraftKey id) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, id);
    }

    public static TextureMapping fire0(Block block) {
        return (new TextureMapping()).put(TextureSlot.FIRE, getBlockTexture(block, "_0"));
    }

    public static TextureMapping fire1(Block block) {
        return (new TextureMapping()).put(TextureSlot.FIRE, getBlockTexture(block, "_1"));
    }

    public static TextureMapping lantern(Block block) {
        return (new TextureMapping()).put(TextureSlot.LANTERN, getBlockTexture(block));
    }

    public static TextureMapping torch(Block block) {
        return (new TextureMapping()).put(TextureSlot.TORCH, getBlockTexture(block));
    }

    public static TextureMapping torch(MinecraftKey id) {
        return (new TextureMapping()).put(TextureSlot.TORCH, id);
    }

    public static TextureMapping particleFromItem(Item item) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getItemTexture(item));
    }

    public static TextureMapping commandBlock(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.FRONT, getBlockTexture(block, "_front")).put(TextureSlot.BACK, getBlockTexture(block, "_back"));
    }

    public static TextureMapping orientableCube(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.FRONT, getBlockTexture(block, "_front")).put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping orientableCubeOnlyTop(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.FRONT, getBlockTexture(block, "_front")).put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping orientableCubeSameEnds(Block block) {
        return (new TextureMapping()).put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.FRONT, getBlockTexture(block, "_front")).put(TextureSlot.END, getBlockTexture(block, "_end"));
    }

    public static TextureMapping top(Block top) {
        return (new TextureMapping()).put(TextureSlot.TOP, getBlockTexture(top, "_top"));
    }

    public static TextureMapping craftingTable(Block block, Block bottom) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getBlockTexture(block, "_front")).put(TextureSlot.DOWN, getBlockTexture(bottom)).put(TextureSlot.UP, getBlockTexture(block, "_top")).put(TextureSlot.NORTH, getBlockTexture(block, "_front")).put(TextureSlot.EAST, getBlockTexture(block, "_side")).put(TextureSlot.SOUTH, getBlockTexture(block, "_side")).put(TextureSlot.WEST, getBlockTexture(block, "_front"));
    }

    public static TextureMapping fletchingTable(Block frontTopSideBlock, Block downBlock) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getBlockTexture(frontTopSideBlock, "_front")).put(TextureSlot.DOWN, getBlockTexture(downBlock)).put(TextureSlot.UP, getBlockTexture(frontTopSideBlock, "_top")).put(TextureSlot.NORTH, getBlockTexture(frontTopSideBlock, "_front")).put(TextureSlot.SOUTH, getBlockTexture(frontTopSideBlock, "_front")).put(TextureSlot.EAST, getBlockTexture(frontTopSideBlock, "_side")).put(TextureSlot.WEST, getBlockTexture(frontTopSideBlock, "_side"));
    }

    public static TextureMapping campfire(Block block) {
        return (new TextureMapping()).put(TextureSlot.LIT_LOG, getBlockTexture(block, "_log_lit")).put(TextureSlot.FIRE, getBlockTexture(block, "_fire"));
    }

    public static TextureMapping candleCake(Block block, boolean lit) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAKE, "_side")).put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAKE, "_bottom")).put(TextureSlot.TOP, getBlockTexture(Blocks.CAKE, "_top")).put(TextureSlot.SIDE, getBlockTexture(Blocks.CAKE, "_side")).put(TextureSlot.CANDLE, getBlockTexture(block, lit ? "_lit" : ""));
    }

    public static TextureMapping cauldron(MinecraftKey content) {
        return (new TextureMapping()).put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAULDRON, "_side")).put(TextureSlot.SIDE, getBlockTexture(Blocks.CAULDRON, "_side")).put(TextureSlot.TOP, getBlockTexture(Blocks.CAULDRON, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAULDRON, "_bottom")).put(TextureSlot.INSIDE, getBlockTexture(Blocks.CAULDRON, "_inner")).put(TextureSlot.CONTENT, content);
    }

    public static TextureMapping layer0(Item item) {
        return (new TextureMapping()).put(TextureSlot.LAYER0, getItemTexture(item));
    }

    public static TextureMapping layer0(Block block) {
        return (new TextureMapping()).put(TextureSlot.LAYER0, getBlockTexture(block));
    }

    public static TextureMapping layer0(MinecraftKey id) {
        return (new TextureMapping()).put(TextureSlot.LAYER0, id);
    }

    public static MinecraftKey getBlockTexture(Block block) {
        MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
        return new MinecraftKey(resourceLocation.getNamespace(), "block/" + resourceLocation.getKey());
    }

    public static MinecraftKey getBlockTexture(Block block, String suffix) {
        MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
        return new MinecraftKey(resourceLocation.getNamespace(), "block/" + resourceLocation.getKey() + suffix);
    }

    public static MinecraftKey getItemTexture(Item item) {
        MinecraftKey resourceLocation = IRegistry.ITEM.getKey(item);
        return new MinecraftKey(resourceLocation.getNamespace(), "item/" + resourceLocation.getKey());
    }

    public static MinecraftKey getItemTexture(Item item, String suffix) {
        MinecraftKey resourceLocation = IRegistry.ITEM.getKey(item);
        return new MinecraftKey(resourceLocation.getNamespace(), "item/" + resourceLocation.getKey() + suffix);
    }
}
