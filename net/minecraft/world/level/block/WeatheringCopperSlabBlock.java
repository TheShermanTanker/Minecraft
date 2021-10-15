package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class WeatheringCopperSlabBlock extends BlockStepAbstract implements WeatheringCopper {
    private final WeatheringCopper.WeatherState weatherState;

    public WeatheringCopperSlabBlock(WeatheringCopper.WeatherState oxidizationLevel, BlockBase.Info settings) {
        super(settings);
        this.weatherState = oxidizationLevel;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.onRandomTick(state, world, pos, random);
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}
