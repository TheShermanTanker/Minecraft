package net.minecraft.world.level.material;

import net.minecraft.core.IRegistry;

public class FluidTypes {
    public static final FluidType EMPTY = register("empty", new FluidTypeEmpty());
    public static final FluidTypeFlowing FLOWING_WATER = register("flowing_water", new FluidTypeWater.Flowing());
    public static final FluidTypeFlowing WATER = register("water", new FluidTypeWater.Source());
    public static final FluidTypeFlowing FLOWING_LAVA = register("flowing_lava", new FluidTypeLava.Flowing());
    public static final FluidTypeFlowing LAVA = register("lava", new FluidTypeLava.Source());

    private static <T extends FluidType> T register(String id, T value) {
        return IRegistry.register(IRegistry.FLUID, id, value);
    }

    static {
        for(FluidType fluid : IRegistry.FLUID) {
            for(Fluid fluidState : fluid.getStateDefinition().getPossibleStates()) {
                FluidType.FLUID_STATE_REGISTRY.add(fluidState);
            }
        }

    }
}
