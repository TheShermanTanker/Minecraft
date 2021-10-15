package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public class BlockCauldron extends BlockCauldronAbstract {
    private static final float RAIN_FILL_CHANCE = 0.05F;
    private static final float POWDER_SNOW_FILL_CHANCE = 0.1F;

    public BlockCauldron(BlockBase.Info settings) {
        super(settings, CauldronInteraction.EMPTY);
    }

    @Override
    public boolean isFull(IBlockData state) {
        return false;
    }

    protected static boolean shouldHandlePrecipitation(World world, BiomeBase.Precipitation precipitation) {
        if (precipitation == BiomeBase.Precipitation.RAIN) {
            return world.getRandom().nextFloat() < 0.05F;
        } else if (precipitation == BiomeBase.Precipitation.SNOW) {
            return world.getRandom().nextFloat() < 0.1F;
        } else {
            return false;
        }
    }

    @Override
    public void handlePrecipitation(IBlockData state, World world, BlockPosition pos, BiomeBase.Precipitation precipitation) {
        if (shouldHandlePrecipitation(world, precipitation)) {
            if (precipitation == BiomeBase.Precipitation.RAIN) {
                world.setTypeUpdate(pos, Blocks.WATER_CAULDRON.getBlockData());
                world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
            } else if (precipitation == BiomeBase.Precipitation.SNOW) {
                world.setTypeUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.getBlockData());
                world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
            }

        }
    }

    @Override
    protected boolean canReceiveStalactiteDrip(FluidType fluid) {
        return true;
    }

    @Override
    protected void receiveStalactiteDrip(IBlockData state, World world, BlockPosition pos, FluidType fluid) {
        if (fluid == FluidTypes.WATER) {
            world.setTypeUpdate(pos, Blocks.WATER_CAULDRON.getBlockData());
            world.triggerEffect(1047, pos, 0);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
        } else if (fluid == FluidTypes.LAVA) {
            world.setTypeUpdate(pos, Blocks.LAVA_CAULDRON.getBlockData());
            world.triggerEffect(1046, pos, 0);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
        }

    }
}
