package net.minecraft.world.level.levelgen.flat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.DimensionManager;

public class WorldGenFlatLayerInfo {
    public static final Codec<WorldGenFlatLayerInfo> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(0, DimensionManager.Y_SIZE).fieldOf("height").forGetter(WorldGenFlatLayerInfo::getHeight), IRegistry.BLOCK.fieldOf("block").orElse(Blocks.AIR).forGetter((flatLayerInfo) -> {
            return flatLayerInfo.getBlockState().getBlock();
        })).apply(instance, WorldGenFlatLayerInfo::new);
    });
    private final Block block;
    private final int height;

    public WorldGenFlatLayerInfo(int thickness, Block block) {
        this.height = thickness;
        this.block = block;
    }

    public int getHeight() {
        return this.height;
    }

    public IBlockData getBlockState() {
        return this.block.getBlockData();
    }

    @Override
    public String toString() {
        return (this.height != 1 ? this.height + "*" : "") + IRegistry.BLOCK.getKey(this.block);
    }
}
