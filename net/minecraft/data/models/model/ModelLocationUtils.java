package net.minecraft.data.models.model;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
    /** @deprecated */
    @Deprecated
    public static MinecraftKey decorateBlockModelLocation(String name) {
        return new MinecraftKey("minecraft", "block/" + name);
    }

    public static MinecraftKey decorateItemModelLocation(String name) {
        return new MinecraftKey("minecraft", "item/" + name);
    }

    public static MinecraftKey getModelLocation(Block block, String suffix) {
        MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
        return new MinecraftKey(resourceLocation.getNamespace(), "block/" + resourceLocation.getKey() + suffix);
    }

    public static MinecraftKey getModelLocation(Block block) {
        MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
        return new MinecraftKey(resourceLocation.getNamespace(), "block/" + resourceLocation.getKey());
    }

    public static MinecraftKey getModelLocation(Item item) {
        MinecraftKey resourceLocation = IRegistry.ITEM.getKey(item);
        return new MinecraftKey(resourceLocation.getNamespace(), "item/" + resourceLocation.getKey());
    }

    public static MinecraftKey getModelLocation(Item item, String suffix) {
        MinecraftKey resourceLocation = IRegistry.ITEM.getKey(item);
        return new MinecraftKey(resourceLocation.getNamespace(), "item/" + resourceLocation.getKey() + suffix);
    }
}
