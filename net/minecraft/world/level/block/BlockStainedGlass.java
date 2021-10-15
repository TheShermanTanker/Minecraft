package net.minecraft.world.level.block;

import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.block.state.BlockBase;

public class BlockStainedGlass extends BlockGlassAbstract implements IBeaconBeam {
    private final EnumColor color;

    public BlockStainedGlass(EnumColor color, BlockBase.Info settings) {
        super(settings);
        this.color = color;
    }

    @Override
    public EnumColor getColor() {
        return this.color;
    }
}
