package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.ICauldronBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockCauldronPowderSnow extends BlockCauldronLayered {
    public BlockCauldronPowderSnow(BlockBase.Info settings, Predicate<BiomeBase.Precipitation> precipitationPredicate, Map<Item, ICauldronBehavior> behaviorMap) {
        super(settings, precipitationPredicate, behaviorMap);
    }

    @Override
    protected void handleEntityOnFireInside(IBlockData state, World world, BlockPosition pos) {
        lowerFillLevel(Blocks.WATER_CAULDRON.getBlockData().set(LEVEL, state.get(LEVEL)), world, pos);
    }
}
