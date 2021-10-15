package net.minecraft.world.level.block;

import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.block.state.BlockBase;

public class BlockStainedGlassPane extends BlockIronBars implements IBeaconBeam {
    private final EnumColor color;

    public BlockStainedGlassPane(EnumColor color, BlockBase.Info settings) {
        super(settings);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public EnumColor getColor() {
        return this.color;
    }
}
