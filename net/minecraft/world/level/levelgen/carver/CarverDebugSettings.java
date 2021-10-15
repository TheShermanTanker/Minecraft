package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class CarverDebugSettings {
    public static final CarverDebugSettings DEFAULT = new CarverDebugSettings(false, Blocks.ACACIA_BUTTON.getBlockData(), Blocks.CANDLE.getBlockData(), Blocks.ORANGE_STAINED_GLASS.getBlockData(), Blocks.GLASS.getBlockData());
    public static final Codec<CarverDebugSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.BOOL.optionalFieldOf("debug_mode", Boolean.valueOf(false)).forGetter(CarverDebugSettings::isDebugMode), IBlockData.CODEC.optionalFieldOf("air_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getAirState), IBlockData.CODEC.optionalFieldOf("water_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getWaterState), IBlockData.CODEC.optionalFieldOf("lava_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getLavaState), IBlockData.CODEC.optionalFieldOf("barrier_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getBarrierState)).apply(instance, CarverDebugSettings::new);
    });
    private boolean debugMode;
    private final IBlockData airState;
    private final IBlockData waterState;
    private final IBlockData lavaState;
    private final IBlockData barrierState;

    public static CarverDebugSettings of(boolean debugMode, IBlockData airState, IBlockData waterState, IBlockData lavaState, IBlockData barrierState) {
        return new CarverDebugSettings(debugMode, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(IBlockData airState, IBlockData waterState, IBlockData lavaState, IBlockData barrierState) {
        return new CarverDebugSettings(false, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(boolean debugMode, IBlockData debugState) {
        return new CarverDebugSettings(debugMode, debugState, DEFAULT.getWaterState(), DEFAULT.getLavaState(), DEFAULT.getBarrierState());
    }

    private CarverDebugSettings(boolean debugMode, IBlockData airState, IBlockData waterState, IBlockData lavaState, IBlockData barrierState) {
        this.debugMode = debugMode;
        this.airState = airState;
        this.waterState = waterState;
        this.lavaState = lavaState;
        this.barrierState = barrierState;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public IBlockData getAirState() {
        return this.airState;
    }

    public IBlockData getWaterState() {
        return this.waterState;
    }

    public IBlockData getLavaState() {
        return this.lavaState;
    }

    public IBlockData getBarrierState() {
        return this.barrierState;
    }
}
