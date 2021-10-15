package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public class BlockCauldronLayered extends BlockCauldronAbstract {
    public static final int MIN_FILL_LEVEL = 1;
    public static final int MAX_FILL_LEVEL = 3;
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL_CAULDRON;
    private static final int BASE_CONTENT_HEIGHT = 6;
    private static final double HEIGHT_PER_LEVEL = 3.0D;
    public static final Predicate<BiomeBase.Precipitation> RAIN = (precipitation) -> {
        return precipitation == BiomeBase.Precipitation.RAIN;
    };
    public static final Predicate<BiomeBase.Precipitation> SNOW = (precipitation) -> {
        return precipitation == BiomeBase.Precipitation.SNOW;
    };
    private final Predicate<BiomeBase.Precipitation> fillPredicate;

    public BlockCauldronLayered(BlockBase.Info settings, Predicate<BiomeBase.Precipitation> precipitationPredicate, Map<Item, CauldronInteraction> behaviorMap) {
        super(settings, behaviorMap);
        this.fillPredicate = precipitationPredicate;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LEVEL, Integer.valueOf(1)));
    }

    @Override
    public boolean isFull(IBlockData state) {
        return state.get(LEVEL) == 3;
    }

    @Override
    protected boolean canReceiveStalactiteDrip(FluidType fluid) {
        return fluid == FluidTypes.WATER && this.fillPredicate == RAIN;
    }

    @Override
    protected double getContentHeight(IBlockData state) {
        return (6.0D + (double)state.get(LEVEL).intValue() * 3.0D) / 16.0D;
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide && entity.isBurning() && this.isEntityInsideContent(state, pos, entity)) {
            entity.extinguish();
            if (entity.mayInteract(world, pos)) {
                this.handleEntityOnFireInside(state, world, pos);
            }
        }

    }

    protected void handleEntityOnFireInside(IBlockData state, World world, BlockPosition pos) {
        lowerFillLevel(state, world, pos);
    }

    public static void lowerFillLevel(IBlockData state, World world, BlockPosition pos) {
        int i = state.get(LEVEL) - 1;
        world.setTypeUpdate(pos, i == 0 ? Blocks.CAULDRON.getBlockData() : state.set(LEVEL, Integer.valueOf(i)));
    }

    @Override
    public void handlePrecipitation(IBlockData state, World world, BlockPosition pos, BiomeBase.Precipitation precipitation) {
        if (BlockCauldron.shouldHandlePrecipitation(world, precipitation) && state.get(LEVEL) != 3 && this.fillPredicate.test(precipitation)) {
            world.setTypeUpdate(pos, state.cycle(LEVEL));
        }
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return state.get(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void receiveStalactiteDrip(IBlockData state, World world, BlockPosition pos, FluidType fluid) {
        if (!this.isFull(state)) {
            world.setTypeUpdate(pos, state.set(LEVEL, Integer.valueOf(state.get(LEVEL) + 1)));
            world.triggerEffect(1047, pos, 0);
        }
    }
}
