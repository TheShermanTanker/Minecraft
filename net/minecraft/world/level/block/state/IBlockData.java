package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class IBlockData extends BlockBase.BlockData {
    public static final Codec<IBlockData> CODEC = codec(IRegistry.BLOCK, Block::getBlockData).stable();

    public IBlockData(Block block, ImmutableMap<IBlockState<?>, Comparable<?>> propertyMap, MapCodec<IBlockData> codec) {
        super(block, propertyMap, codec);
    }

    @Override
    protected IBlockData asState() {
        return this;
    }
}
